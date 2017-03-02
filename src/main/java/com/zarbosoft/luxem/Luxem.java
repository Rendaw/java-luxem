package com.zarbosoft.luxem;

import com.zarbosoft.interface1.Walk;
import com.zarbosoft.luxem.read.Parse;
import com.zarbosoft.luxem.read.ReadTypeGrammar;
import com.zarbosoft.pidgoon.events.Grammar;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.stream.Stream;

public class Luxem {

	public static <T> Stream<T> parse(final Reflections reflections, final Class<T> rootClass, final String data) {
		return parse(reflections, rootClass, new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
	}

	public static <T> Stream<T> parse(
			final Reflections reflections, final Type rootClass, final Type rootParameter, final String data
	) {
		return parse(reflections,
				rootClass,
				rootParameter,
				new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))
		);
	}

	public static <T> Stream<T> parse(
			final Reflections reflections, final Type rootClass, final Type rootParameter, final InputStream data
	) {
		final HashSet<Type> seen = new HashSet<>();
		final Grammar grammar = ReadTypeGrammar.buildGrammar(reflections, new Walk.TypeInfo(rootClass, rootParameter));
		return new Parse<T>().grammar(grammar).errorHistory(5).parse(data);
	}

	public static <T> Stream<T> parse(final Reflections reflections, final Class<T> rootClass, final InputStream data) {
		final HashSet<Type> seen = new HashSet<>();
		final Grammar grammar = ReadTypeGrammar.buildGrammar(reflections, new Walk.TypeInfo(rootClass));
		return new Parse<T>().grammar(grammar).errorHistory(5).parse(data);
	}

}
