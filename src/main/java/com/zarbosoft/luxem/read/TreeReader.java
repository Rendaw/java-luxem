package com.zarbosoft.luxem.read;

import com.zarbosoft.luxem.tree.Typed;
import com.zarbosoft.rendaw.common.Assertion;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TreeReader {
	private final Deque<State> stack = new ArrayDeque<>();

	private abstract class State {
		public void key(final String value) {
			throw new Assertion();
		}

		public abstract void value(Object value);

		public void record() {
			stack.addLast(new RecordState());
		}

		public void array() {
			stack.addLast(new ArrayState());
		}

		public abstract void type(String value);

		public abstract Object get();
	}

	private class ArrayState extends State {
		private final List data = new ArrayList();
		private String type = null;

		@Override
		public void value(final Object value) {
			if (type != null) {
				data.add(new Typed(type, value));
				type = null;
			} else
				data.add(value);
		}

		@Override
		public void type(final String value) {
			type = value;
		}

		@Override
		public Object get() {
			return data;
		}
	}

	private class RecordState extends State {
		private final Map data = new HashMap();
		private String key = null;
		private String type = null;

		@Override
		public void key(final String value) {
			key = value;
		}

		@Override
		public void value(final Object value) {
			if (type != null) {
				data.put(key, new Typed(type, value));
				type = null;
			} else
				data.put(key, value);
		}

		@Override
		public void type(final String value) {
			type = value;
		}

		@Override
		public Object get() {
			return data;
		}
	}

	public List read(final InputStream stream) {
		final ArrayState top = new ArrayState();
		stack.addLast(top);
		final BufferedRawReader reader = new BufferedRawReader();
		reader.eatKey = b -> stack.peekLast().key(new String(b, StandardCharsets.UTF_8));
		reader.eatPrimitive = b -> stack.peekLast().value(new String(b, StandardCharsets.UTF_8));
		reader.eatType = b -> stack.peekLast().type(new String(b, StandardCharsets.UTF_8));
		reader.eatArrayBegin = () -> stack.addLast(new ArrayState());
		reader.eatRecordBegin = () -> stack.addLast(new RecordState());
		reader.eatArrayEnd = reader.eatRecordEnd = () -> {
			final State done = stack.pollLast();
			stack.peekLast().value(done.get());
		};
		RawReader.stream(reader, stream);
		return (List) top.get();
	}
}
