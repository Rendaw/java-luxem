package com.zarbosoft.luxem.write;

import com.zarbosoft.luxem.tree.Typed;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class TreeWriter {
	public static void write(final OutputStream stream, final List<Object> tree) throws IOException {
		write(stream, false, (byte) 0, 0, tree);
	}

	public static void write(
			final OutputStream stream, final byte indentByte, final int indentMultiple, final List<Object> tree
	) throws IOException {
		write(stream, true, indentByte, indentMultiple, tree);
	}

	public static void write(
			final OutputStream stream,
			final boolean pretty,
			final byte indentByte,
			final int indentMultiple,
			final List<Object> tree
	) throws IOException {
		final RawWriter writer = new RawWriter(stream, pretty, indentByte, indentMultiple);
		for (final Object o : tree) {
			writeNode(writer, o);
		}
	}

	private static void writeNode(final RawWriter writer, final Object o) throws IOException {
		if (o instanceof Map) {
			writer.recordBegin();
			for (final Map.Entry<Object, Object> e : ((Map<Object, Object>) o).entrySet()) {
				writer.key(e.getKey().toString());
				writeNode(writer, e.getValue());
			}
			writer.recordEnd();
		} else if (o instanceof List) {
			writer.arrayBegin();
			for (final Object c : (List) o) {
				writeNode(writer, c);
			}
			writer.arrayEnd();
		} else if (o instanceof Typed) {
			writer.type(((Typed) o).name);
		} else {
			writer.primitive(o.toString().getBytes(StandardCharsets.UTF_8));
		}
	}
}
