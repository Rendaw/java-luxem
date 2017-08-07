package com.zarbosoft.luxem;

import com.zarbosoft.interface1.Walk;
import com.zarbosoft.luxem.read.Parse;
import com.zarbosoft.luxem.read.ReadTypeGrammar;
import com.zarbosoft.luxem.write.TypeWriter;
import com.zarbosoft.pidgoon.events.Grammar;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
		final HashSet<Type> seen = new HashSet<>();
		final Grammar grammar = ReadTypeGrammar.buildGrammar(reflections, rootType);
		return new Parse<T>().grammar(grammar).errorHistory(5).parse(data);
	}

	public static void write(final Object root, final OutputStream data) {
		write(new Walk.TypeInfo(root.getClass()), root, data);
	}

	public static void write(final Walk.TypeInfo rootType, final Object root, final OutputStream data) {
		new TypeWriter(data).write(rootType, root);
	}
}
