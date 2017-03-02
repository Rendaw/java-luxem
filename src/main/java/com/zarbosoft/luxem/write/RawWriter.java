package com.zarbosoft.luxem.write;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

public class RawWriter {
	private final boolean pretty;
	private final byte indentByte;
	private final int indentMultiple;
	private final OutputStream stream;
	private boolean first = true;

	private enum State {
		ARRAY,
		RECORD,
		TYPED,
	}

	private final Deque<State> states = new ArrayDeque<>();

	public RawWriter(final OutputStream stream) {
		this(stream, false, (byte) 0, 0);
		states.addLast(State.ARRAY);
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
	}

	private void valueBegin() throws IOException {
		if (states.peekLast() == State.TYPED) {
			states.pollLast();
		} else if (pretty && !first) {
			stream.write('\n');
			indent();
			first = false;
		}
	}

	private void indent() throws IOException {
		if (!pretty)
			return;
		for (int i = 0; i < states.size(); ++i)
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

	public RawWriter key(final byte[] bytes) throws IOException {
		keyBegin();
		keyChunk(bytes);
		keyEnd();
		return this;
	}

	public RawWriter keyBegin() throws IOException {
		if (pretty) {
			stream.write('\n');
			indent();
		}
		return this;
	}

	private RawWriter keyEnd() throws IOException {
		stream.write(':');
		if (pretty)
			stream.write(' ');
		return this;
	}

	public RawWriter keyChunk(final byte[] bytes) throws IOException {
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
		valueBegin();
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
		states.addLast(State.TYPED);
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

	public RawWriter shortPrimitive(final byte[] bytes) throws IOException {
		for (int i = 0; i < bytes.length; ++i) {
			switch (bytes[i]) {
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
					return quotedPrimitive(bytes);
			}
		}
		valueBegin();
		stream.write(bytes);
		stream.write(',');
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
