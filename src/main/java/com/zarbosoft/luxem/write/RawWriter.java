package com.zarbosoft.luxem.write;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

public class RawWriter {
	private final boolean pretty;
	private final byte indentByte;
	private final int indentMultiple;
	private final OutputStream stream;
	private boolean first = true;

	public RawWriter type(final String value) throws IOException {
		return type(value.getBytes(StandardCharsets.UTF_8));
	}

	public RawWriter primitive(final String value) throws IOException {
		return primitive(value.getBytes(StandardCharsets.UTF_8));
	}

	public RawWriter key(final String key) throws IOException {
		return key(key.getBytes(StandardCharsets.UTF_8));
	}

	private enum State {
		ARRAY,
		RECORD,
		PREFIXED,
	}

	private final Deque<State> states = new ArrayDeque<>();

	public RawWriter(final OutputStream stream) {
		this(stream, false, (byte) 0, 0);
	}

	public RawWriter(final OutputStream stream, final byte indentByte, final int indentMultiple) {
		this(stream, true, indentByte, indentMultiple);
	}

	public RawWriter(
			final OutputStream stream, final boolean pretty, final byte indentByte, final int indentMultiple
	) {
		this.stream = stream;
		this.pretty = pretty;
		this.indentByte = indentByte;
		this.indentMultiple = indentMultiple;
		states.addLast(State.ARRAY);
	}

	private void valueBegin() throws IOException {
		if (states.peekLast() == State.PREFIXED) {
			states.pollLast();
		} else if (first) {
			first = false;
		} else if (pretty) {
			stream.write('\n');
			indent();
		}
	}

	private void indent() throws IOException {
		if (!pretty)
			return;
		for (int i = 0; i < states.size() - 1; ++i)
			for (int j = 0; j < indentMultiple; ++j)
				stream.write(indentByte);
	}

	public RawWriter recordBegin() throws IOException {
		valueBegin();
		stream.write((byte) '{');
		states.addLast(State.RECORD);
		return this;
	}

	public RawWriter recordEnd() throws IOException {
		states.pollLast();
		if (pretty) {
			stream.write('\n');
			indent();
		}
		stream.write((byte) '}');
		stream.write((byte) ',');
		return this;
	}

	public RawWriter arrayBegin() throws IOException {
		valueBegin();
		stream.write((byte) '[');
		states.addLast(State.ARRAY);
		return this;
	}

	public RawWriter arrayEnd() throws IOException {
		states.pollLast();
		if (pretty) {
			stream.write('\n');
			indent();
		}
		stream.write((byte) ']');
		stream.write((byte) ',');
		return this;
	}

	private boolean isAmbiguous(final byte b) {
		switch (b) {
			case ':':
			case ',':
			case '[':
			case ']':
			case '{':
			case '}':
			case '(':
			case ')':
			case ' ':
			case '\n':
			case '\t':
			case '\r':
			case '*':
				return true;
			default:
				return false;
		}
	}

	public RawWriter key(final byte[] bytes) throws IOException {
		if (pretty) {
			for (int i = 0; i < bytes.length; ++i) {
				if (isAmbiguous(bytes[i]))
					return quotedKey(bytes);
			}
			return shortKey(bytes);
		} else
			return quotedKey(bytes);
	}

	public RawWriter shortKey(final byte[] bytes) throws IOException {
		shortKeyBegin();
		shortKeyChunk(bytes);
		shortKeyEnd();
		return this;
	}

	public RawWriter shortKeyBegin() throws IOException {
		if (pretty) {
			stream.write('\n');
			indent();
		}
		return this;
	}

	private RawWriter shortKeyEnd() throws IOException {
		stream.write(':');
		if (pretty)
			stream.write(' ');
		states.addLast(State.PREFIXED);
		return this;
	}

	public RawWriter shortKeyChunk(final byte[] bytes) throws IOException {
		stream.write(bytes);
		return this;
	}

	public RawWriter quotedKey(final byte[] bytes) throws IOException {
		quotedKeyBegin();
		quotedKeyChunk(bytes);
		quotedKeyEnd();
		return this;
	}

	public RawWriter quotedKeyBegin() throws IOException {
		if (pretty) {
			stream.write('\n');
			indent();
		}
		stream.write('"');
		return this;
	}

	private RawWriter quotedKeyEnd() throws IOException {
		stream.write('"');
		stream.write(':');
		if (pretty)
			stream.write(' ');
		states.addLast(State.PREFIXED);
		return this;
	}

	public RawWriter quotedKeyChunk(final byte[] bytes) throws IOException {
		int escapee = 0;
		for (int i = 0; i < bytes.length; ++i) {
			switch (bytes[i]) {
				case ':':
				case ' ':
				case '\n':
				case '\t':
				case '\r':
				case '*':
					break;
				default:
					continue;
			}
			stream.write(bytes, escapee, i - escapee);
			stream.write('\\');
			if (bytes[i] == '\n')
				stream.write('n');
			else if (bytes[i] == '\t')
				stream.write('t');
			else if (bytes[i] == '\r')
				stream.write('r');
			else
				stream.write(bytes[i]);
			escapee = i;
		}
		stream.write(bytes, escapee, bytes.length - escapee);
		return this;
	}

	public RawWriter type(final byte[] bytes) throws IOException {
		typeBegin();
		typeChunk(bytes);
		typeEnd();
		return this;
	}

	public RawWriter typeBegin() throws IOException {
		valueBegin();
		stream.write('(');
		return this;
	}

	public RawWriter typeEnd() throws IOException {
		stream.write(')');
		if (pretty)
			stream.write(' ');
		states.addLast(State.PREFIXED);
		return this;
	}

	public RawWriter typeChunk(final byte[] bytes) throws IOException {
		int escapee = 0;
		for (int i = 0; i < bytes.length; ++i) {
			switch (bytes[i]) {
				case ')':
				case '\n':
				case '\t':
				case '\r':
					break;
				default:
					continue;
			}
			stream.write(bytes, escapee, i - escapee);
			stream.write('\\');
			if (bytes[i] == '\n')
				stream.write('n');
			else if (bytes[i] == '\t')
				stream.write('t');
			else if (bytes[i] == '\r')
				stream.write('r');
			else
				stream.write(bytes[i]);
			escapee = i;
		}
		stream.write(bytes, escapee, bytes.length - escapee);
		return this;
	}

	public RawWriter primitive(final byte[] bytes) throws IOException {
		if (pretty) {
			for (int i = 0; i < bytes.length; ++i) {
				if (isAmbiguous(bytes[i]))
					return quotedPrimitive(bytes);
			}
			return shortPrimitive(bytes);
		} else
			return quotedPrimitive(bytes);
	}

	public RawWriter shortPrimitive(final byte[] bytes) throws IOException {
		shortPrimitiveBegin();
		shortPrimitiveChunk(bytes);
		shortPrimitiveEnd();
		return this;
	}

	public RawWriter shortPrimitiveBegin() throws IOException {
		valueBegin();
		return this;
	}

	private RawWriter shortPrimitiveEnd() throws IOException {
		stream.write(',');
		return this;
	}

	private RawWriter shortPrimitiveChunk(final byte[] bytes) throws IOException {
		stream.write(bytes);
		return this;
	}

	public RawWriter quotedPrimitive(final byte[] bytes) throws IOException {
		quotedPrimitiveBegin();
		quotedPrimitiveChunk(bytes);
		quotedPrimitiveEnd();
		return this;
	}

	public RawWriter quotedPrimitiveBegin() throws IOException {
		valueBegin();
		stream.write('"');
		return this;
	}

	public RawWriter quotedPrimitiveEnd() throws IOException {
		stream.write('"');
		stream.write(',');
		return this;
	}

	public RawWriter quotedPrimitiveChunk(final byte[] bytes) throws IOException {
		int escapee = 0;
		for (int i = 0; i < bytes.length; ++i) {
			switch (bytes[i]) {
				case ')':
				case '\n':
				case '\t':
				case '\r':
					break;
				default:
					continue;
			}
			stream.write(bytes, escapee, i - escapee);
			stream.write('\\');
			if (bytes[i] == '\n')
				stream.write('n');
			else if (bytes[i] == '\t')
				stream.write('t');
			else if (bytes[i] == '\r')
				stream.write('r');
			else
				stream.write(bytes[i]);
			escapee = i;
		}
		stream.write(bytes, escapee, bytes.length - escapee);
		return this;
	}
}
