package com.zarbosoft.luxem.read;

import com.zarbosoft.luxem.read.path.LuxemArrayPath;
import com.zarbosoft.luxem.read.path.LuxemObjectPath;
import com.zarbosoft.luxem.read.path.LuxemPath;
import com.zarbosoft.luxem.read.source.*;
import com.zarbosoft.pidgoon.events.Event;
import com.zarbosoft.rendaw.common.Common;
import com.zarbosoft.rendaw.common.Pair;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.drain;

public class RawReader {
	@FunctionalInterface
	public interface VoidConsumer {
		void run();
	}

	@FunctionalInterface
	public interface ByteConsumer {
		void accept(byte b);
	}

	public VoidConsumer eatPrimitiveBegin = () -> {
	};
	public ByteConsumer eatPrimitive = b -> {
	};
	public VoidConsumer eatPrimitiveEnd = () -> {
	};
	public VoidConsumer eatTypeBegin = () -> {
	};
	public ByteConsumer eatType = b -> {
	};
	public VoidConsumer eatTypeEnd = () -> {
	};
	public VoidConsumer eatArrayBegin = () -> {
	};
	public VoidConsumer eatArrayEnd = () -> {
	};
	public VoidConsumer eatKeyBegin = () -> {
	};
	public ByteConsumer eatKey = b -> {
	};
	public VoidConsumer eatKeyEnd = () -> {
	};
	public VoidConsumer eatRecordBegin = () -> {
	};
	public VoidConsumer eatRecordEnd = () -> {
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

	private void finish() {
		if (stack.size() >= 3) {
			if (stack.size() == 3 && stack.peekLast().getClass() == Primitive.class)
				stack.peekLast().finish(this);
			else
				throw new InvalidStream("End reached mid-element.");
		}
	}

	public static Stream<Boolean> stream(final RawReader reader, final InputStream source) {
		return Common.concatNull(Common.stream(source)).map(bytes -> {
			if (bytes == null) {
				// Post-last chunk
				reader.finish();
				return true;
			} else {
				for (final byte b : bytes)
					reader.eat(b);
				return false;
			}
		});
	}

	public static Stream<Pair<Event, Object>> streamEvents(final InputStream source, final EventFactory factory) {
		class State {
			LuxemPath path = new LuxemArrayPath(null);
			Deque<Pair<Event, Object>> events = new ArrayDeque<>();
		}
		final State state = new State();
		final BufferedRawReader reader = new BufferedRawReader();
		reader.eatRecordBegin = () -> {
			state.path = new LuxemObjectPath(state.path.value());
			state.events.addLast(new Pair<>(factory.objectOpen(), state.path));
		};
		reader.eatRecordEnd = () -> {
			state.path = state.path.pop();
			state.events.addLast(new Pair<>(factory.objectClose(), state.path));
		};
		reader.eatArrayBegin = () -> {
			state.path = new LuxemArrayPath(state.path.value());
			state.events.addLast(new Pair<>(factory.arrayOpen(), state.path));
		};
		reader.eatArrayEnd = () -> {
			state.path = state.path.pop();
			state.events.addLast(new Pair<>(factory.arrayClose(), state.path));
		};
		reader.eatKey = bytes -> {
			final String string = new String(bytes, StandardCharsets.UTF_8);
			state.path = state.path.key(string);
			state.events.addLast(new Pair<>(factory.key(string), state.path));
		};
		reader.eatType = bytes -> {
			state.path = state.path.type();
			state.events.addLast(new Pair<>(factory.type(new String(bytes, StandardCharsets.UTF_8)), state.path));
		};
		reader.eatPrimitive = bytes -> {
			state.path = state.path.value();
			state.events.addLast(new Pair<>(factory.primitive(new String(bytes, StandardCharsets.UTF_8)), state.path));
		};
		return RawReader.stream(reader, source).flatMap(last -> {
			return drain(state.events);
		});
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
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			raw.stack.addLast(new RootArrayBorder());
			raw.stack.addLast(new Value());
			return false;
		}
	}

	private static class RootArrayBorder extends State {
		public boolean eat(final RawReader raw, final byte next) {
			if (raw.eatInterstitial(next))
				return true;
			switch (next) {
				case (byte) ',':
					finished(raw);
					return true;
			}
			throw new InvalidStream("Expected [,].");
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

	public interface EventFactory {
		Event objectOpen();

		Event objectClose();

		Event arrayOpen();

		Event arrayClose();

		Event key(String s);

		Event type(String s);

		Event primitive(String s);
	}

	public static class DefaultEventFactory implements EventFactory {

		@Override
		public Event objectOpen() {
			return new LObjectOpenEvent();
		}

		@Override
		public Event objectClose() {
			return new LObjectCloseEvent();
		}

		@Override
		public Event arrayOpen() {
			return new LArrayOpenEvent();
		}

		@Override
		public Event arrayClose() {
			return new LArrayCloseEvent();
		}

		@Override
		public Event key(final String s) {
			return new LKeyEvent(s);
		}

		@Override
		public Event type(final String s) {
			return new LTypeEvent(s);
		}

		@Override
		public Event primitive(final String s) {
			return new LPrimitiveEvent(s);
		}
	}
}
