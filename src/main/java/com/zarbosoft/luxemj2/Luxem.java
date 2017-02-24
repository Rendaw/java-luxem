package com.zarbosoft.luxemj2;

import com.zarbosoft.luxemj2.read.Parse;
import com.zarbosoft.luxemj2.read.ReadTypeGrammar;
import com.zarbosoft.pidgoon.events.Grammar;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public class Luxem {

	public static <T> T parse(final Reflections reflections, final Class<T> outputClass, final String data) {
		return parse(reflections, outputClass, new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
	}

	public static <T> T parse(final Reflections reflections, final Class<T> outputClass, final InputStream data) {
		final HashSet<Type> seen = new HashSet<>();
		final Grammar grammar = ReadTypeGrammar.buildGrammar(reflections, outputClass);
		return new Parse<T>().grammar(grammar).errorHistory(5).parse(data);
	}

}
