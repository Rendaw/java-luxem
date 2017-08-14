package com.zarbosoft.luxem;

import com.zarbosoft.interface1.Walk;
import com.zarbosoft.luxem.read.Parse;
import com.zarbosoft.luxem.read.ReadTypeGrammar;
import com.zarbosoft.luxem.write.TypeWriter;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

public class Luxem {

	public static <T> Stream<T> parse(
			final Reflections reflections, final Walk.TypeInfo rootType, final String data
	) {
		return parse(reflections, rootType, new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
	}

	public static <T> Stream<T> parse(
			final Reflections reflections, final Walk.TypeInfo rootType, final InputStream data
	) {
		return new Parse<T>()
				.grammar(ReadTypeGrammar.buildGrammar(reflections, rootType))
				.errorHistory(5)
				.parseByElement(data);
	}

	public static void write(final Object root, final OutputStream data) {
		write(new Walk.TypeInfo(root.getClass()), root, data);
	}

	public static void write(final Walk.TypeInfo rootType, final Object root, final OutputStream data) {
		new TypeWriter(data).write(rootType, root);
	}
}
