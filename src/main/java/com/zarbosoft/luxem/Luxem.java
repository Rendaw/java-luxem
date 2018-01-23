package com.zarbosoft.luxem;

import com.zarbosoft.interface1.Walk;
import com.zarbosoft.luxem.read.Parse;
import com.zarbosoft.luxem.read.ReadTypeGrammar;
import com.zarbosoft.luxem.read.TreeReader;
import com.zarbosoft.luxem.write.TypeWriter;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * Methods for common use cases.
 */
public class Luxem {

	/**
	 * Read a luxem document as a tree of Lists, String Maps, Strings, and Typeds.
	 *
	 * @param data luxem
	 * @return list of top level objects
	 */
	public static List parse(final String data) {
		return new TreeReader().read(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Read a luxem document as a tree of Lists, String Maps, Strings, and Typeds.
	 *
	 * @param data luxem
	 * @return list of top level objects
	 */
	public static List parse(final InputStream data) {
		return new TreeReader().read(data);
	}

	/**
	 * Read a luxem document as a stream of deserialized objects.  Objects and fields to deserialize should be
	 * annotated with @Configuration from the interface package.
	 *
	 * @param reflections Used to locate classes that can be deserialized.
	 * @param rootType    The type of the top level objects in the tree.
	 * @param data        The luxem
	 * @param <T>         rootType
	 * @return The stream of deserialized objects.
	 */
	public static <T> Stream<T> parse(
			final Reflections reflections, final Walk.TypeInfo rootType, final String data
	) {
		return parse(reflections, rootType, new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Read a luxem document as a stream of deserialized objects.  Objects and fields to deserialize should be
	 * annotated with @Configuration from the interface package.
	 *
	 * @param reflections Used to locate classes that can be deserialized.
	 * @param rootType    The type of the top level objects in the tree.
	 * @param data        The luxem
	 * @param <T>         rootType
	 * @return The stream of deserialized objects.
	 */
	public static <T> Stream<T> parse(
			final Reflections reflections, final Walk.TypeInfo rootType, final InputStream data
	) {
		return new Parse<T>()
				.grammar(ReadTypeGrammar.buildGrammar(reflections, rootType))
				.errorHistory(5)
				.parseByElement(data);
	}

	/**
	 * Write a type as a document.
	 *
	 * @param root
	 * @param data
	 */
	public static void write(final Object root, final OutputStream data) {
		write(new Walk.TypeInfo(root.getClass()), root, data);
	}

	/**
	 * If the document has a polymorphic root, write root with an explicit type.
	 *
	 * @param rootType the document root type
	 * @param root     the root value
	 * @param data
	 */
	public static void write(final Walk.TypeInfo rootType, final Object root, final OutputStream data) {
		new TypeWriter(data).write(rootType, root);
	}
}
