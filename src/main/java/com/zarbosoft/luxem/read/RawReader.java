package com.zarbosoft.luxem.read;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class RawReader {
	@FunctionalInterface
	public interface ByteConsumer {
		void accept(byte b);
	}

	public Runnable eatPrimitiveBegin = () -> {
	};
	public ByteConsumer eatPrimitive = b -> {
	};
	public Runnable eatPrimitiveEnd = () -> {
	};
	public Runnable eatTypeBegin = () -> {
	};
	public ByteConsumer eatType = b -> {
	};
	public Runnable eatTypeEnd = () -> {
	};
	public Runnable eatArrayBegin = () -> {
	};
	public Runnable eatArrayEnd = () -> {
	};
	public Runnable eatKeyBegin = () -> {
	};
	public ByteConsumer eatKey = b -> {
	};
	public Runnable eatKeyEnd = () -> {
	};
	public Runnable eatRecordBegin = () -> {
	};
	public Runnable eatRecordEnd = () -> {
	};

	private abstract static class State {
		public abstract boolean eat(RawReader raw, byte next);

		public void finish(final RawReader raw) {
			throw new InvalidStream("End reached mid-element.");
		}

		protected void finished(final RawReader raw) {
			raw.stack.pollLast();
		}
	}

	private final Deque<State> stack = new ArrayDeque<>();

	public RawReader() {
		stack.addLast(new RootArray());
	}

	public static void feed(final RawReader reader, final InputStream source) {
		final byte[] buffer = new byte[1024];
		int length;
		while ((length = uncheck(() -> source.read(buffer))) != -1) {
			for (int i = 0; i < length; ++i)
				reader.eat(buffer[i]);
		}
		reader.finish();
	}

	public static Feeder feeder(final RawReader reader, final InputStream source) {
		return new Feeder(reader, source);
	}

	public static class Feeder {

		private final RawReader reader;
		private final InputStream source;
		private final byte[] buffer;

		public Feeder(final RawReader reader, final InputStream source) {
			this.reader = reader;
			this.source = source;
			this.buffer = new byte[1024];
		}

		public boolean feed() {
			final int length = uncheck(() -> source.read(buffer));
			if (length == -1)
				return false;
			for (int i = 0; i < length; ++i)
				reader.eat(buffer[i]);
			return true;
		}

		public void finish() {
			reader.finish();
		}
	}

	private void finish() {
		if (stack.size() > 2)
			throw new InvalidStream("End reached mid-element.");
		if (stack.size() == 2)
			stack.peekLast().finish(this);
	}

	public void eat(final byte next) {
		while (!stack.peekLast().eat(this, next)) {
		}
	}

	private boolean eatInterstitial(final byte next) {
		switch (next) {
			case (byte) '\t':
				return true;
			case (byte) '\n':
				return true;
			case (byte) ' ':
				return true;
			case (byte) '*':
				stack.addLast(new Comment());
				return true;
		}
		return false;
	}

	private static class RootArray extends State {
		boolean first = true;

		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			if (!first && next == (byte) ',') {
				return true;
			}
			first = false;
			raw.stack.addLast(new Value());
			return false;
		}
	}

	private static class Type extends TextState {
		public void begin(final RawReader raw) {
			raw.eatTypeBegin.run();
		}

		protected void eatMiddle(final RawReader raw, final byte next) {
			raw.eatType.accept(next);
		}

		@Override
		protected void end(final RawReader raw) {
			raw.eatTypeEnd.run();
		}

		protected Resolution eatEnd(final RawReader raw, final byte next) {
			return next == (byte) ')' ? Resolution.ATE : Resolution.REJECTED;
		}
	}

	private static class Value extends State {
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			if (next == (byte) '(') {
				finished(raw);
				raw.stack.addLast(new UntypedValue());
				raw.stack.addLast(new Type());
				return true;
			}
			return UntypedValue.eatStatic(this, raw, next);
		}
	}

	private static class UntypedValue extends State {

		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			return eatStatic(this, raw, next);
		}

		public static boolean eatStatic(final State state, final RawReader raw, final byte next) {
			state.finished(raw);
			switch (next) {
				case (byte) '[':
					raw.eatArrayBegin.run();
					raw.stack.addLast(new Array());
					return true;
				case (byte) '{':
					raw.eatRecordBegin.run();
					raw.stack.addLast(new Record());
					return true;
				case (byte) '"':
					raw.stack.addLast(new QuotedPrimitive());
					return true;
			}
			raw.stack.addLast(new Primitive());
			return false;
		}
	}

	private static class Array extends State {
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			if (next == (byte) ']') {
				raw.eatArrayEnd.run();
				finished(raw);
				return true;
			}
			raw.stack.addLast(new ArrayBorder());
			raw.stack.addLast(new Value());
			return false;
		}

	}

	private static class ArrayBorder extends State {
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			switch (next) {
				case (byte) ',':
					finished(raw);
					return true;
				case (byte) ']':
					finished(raw);
					return false;
			}
			throw new InvalidStream("Expected [,] or []].");
		}
	}

	private static class Record extends State {
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			if (next == (byte) '}') {
				raw.eatRecordEnd.run();
				finished(raw);
				return true;
			}
			raw.stack.addLast(new RecordBorder());
			raw.stack.addLast(new Value());
			raw.stack.addLast(new RecordSeparator());
			raw.stack.addLast(new UndifferentiatedKey());
			return false;
		}
	}

	private static class RecordSeparator extends State {
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			if (next == (byte) ':') {
				finished(raw);
				return true;
			}
			throw new InvalidStream("Expected [:].");
		}
	}

	private static class RecordBorder extends State {
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			switch (next) {
				case (byte) ',':
					finished(raw);
					return true;
				case (byte) '}':
					finished(raw);
					return false;
			}
			throw new InvalidStream("Expected [,] or [}].");
		}
	}

	private static class UndifferentiatedKey extends State {

		public boolean eat(final RawReader raw, final byte next) {
			finished(raw);
			if (raw.eatInterstitial(next))
				return true;
			if (next == (byte) '"') {
				raw.stack.addLast(new QuotedKey());
				return true;
			} else {
				raw.stack.addLast(new Key());
				return false;
			}
		}
	}

	private static class QuotedKey extends TextState {
		public void begin(final RawReader raw) {
			if (raw.eatKeyBegin != null)
				raw.eatKeyBegin.run();
		}

		protected void end(final RawReader raw) {
			if (raw.eatKeyEnd != null)
				raw.eatKeyEnd.run();
		}

		protected void eatMiddle(final RawReader raw, final byte next) {
			if (raw.eatKey != null)
				raw.eatKey.accept(next);
		}

		protected Resolution eatEnd(final RawReader raw, final byte next) {
			return next == (byte) '"' ? Resolution.ATE : Resolution.REJECTED;
		}
	}

	private static class Key extends TextState {
		public void begin(final RawReader raw) {
			if (raw.eatKeyBegin != null)
				raw.eatKeyBegin.run();
		}

		protected void end(final RawReader raw) {
			if (raw.eatKeyEnd != null)
				raw.eatKeyEnd.run();
		}

		protected void eatMiddle(final RawReader raw, final byte next) {
			if (raw.eatKey != null)
				raw.eatKey.accept(next);
		}

		protected Resolution eatEnd(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return Resolution.ATE;
			if (next == (byte) ':')
				return Resolution.TASTED;
			return Resolution.REJECTED;
		}
	}

	private static class QuotedPrimitive extends TextState {
		public void begin(final RawReader raw) {
			if (raw.eatPrimitiveBegin != null)
				raw.eatPrimitiveBegin.run();
		}

		protected void end(final RawReader raw) {
			if (raw.eatPrimitiveEnd != null)
				raw.eatPrimitiveEnd.run();
		}

		protected void eatMiddle(final RawReader raw, final byte next) {
			if (raw.eatPrimitive != null)
				raw.eatPrimitive.accept(next);
		}

		protected Resolution eatEnd(final RawReader raw, final byte next) {
			return next == (byte) '"' ? Resolution.ATE : Resolution.REJECTED;
		}
	}

	private static class Primitive extends TextState {
		protected void begin(final RawReader raw) {
			if (raw.eatPrimitiveBegin != null)
				raw.eatPrimitiveBegin.run();
		}

		protected void end(final RawReader raw) {
			if (raw.eatPrimitiveEnd != null)
				raw.eatPrimitiveEnd.run();
		}

		protected void eatMiddle(final RawReader raw, final byte next) {
			if (raw.eatPrimitive != null)
				raw.eatPrimitive.accept(next);
		}

		protected Resolution eatEnd(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return Resolution.ATE;
			switch (next) {
				case (byte) ']':
				case (byte) '}':
				case (byte) ',':
					return Resolution.TASTED;
			}
			return Resolution.REJECTED;
		}
	}

	private static class Comment extends TextState {
		protected void begin(final RawReader raw) {

		}

		protected void end(final RawReader raw) {

		}

		protected void eatMiddle(final RawReader raw, final byte next) {

		}

		protected Resolution eatEnd(final RawReader raw, final byte next) {
			return next == (byte) '*' ? Resolution.ATE : Resolution.REJECTED;
		}
	}

	private static abstract class TextState extends State {
		boolean first = true;
		boolean escape = false;

		public enum Resolution {
			ATE,
			TASTED,
			REJECTED
		}

		@Override
		public final boolean eat(final RawReader raw, final byte next) {
			if (first) {
				first = false;
				begin(raw);
			}
			if (!escape) {
				if ((byte) next == '\\') {
					escape = true;
					return true;
				}
				final Resolution ended = eatEnd(raw, next);
				if (ended != Resolution.REJECTED) {
					end(raw);
					finished(raw);
					return ended == Resolution.ATE ? true : false;
				}
			}
			if (escape && next == 'n')
				eatMiddle(raw, (byte) '\n');
			else if (escape && next == 'r')
				eatMiddle(raw, (byte) '\r');
			else if (escape && next == 't')
				eatMiddle(raw, (byte) '\t');
			else
				eatMiddle(raw, next);
			escape = false;
			return true;
		}

		@Override
		public void finish(final RawReader raw) {
			end(raw);
			finished(raw);
		}

		protected abstract void begin(RawReader raw);

		protected abstract void end(RawReader raw);

		protected abstract void eatMiddle(RawReader raw, byte next);

		protected abstract Resolution eatEnd(RawReader raw, byte next);
	}
}
