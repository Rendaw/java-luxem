package com.zarbosoft.luxem.write;

import com.zarbosoft.interface1.Walk;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class TypeWriter {
	private final OutputStream stream;
	private RawWriter writer = null;

	public TypeWriter(final OutputStream stream) {
		this.stream = stream;
	}

	public TypeWriter pretty(final byte indentByte, final int indentMultiple) {
		this.writer = new RawWriter(stream, indentByte, indentMultiple);
		return this;
	}

	public TypeWriter compact() {
		this.writer = new RawWriter(stream);
		return this;
	}

	public TypeWriter write(final Object root) {
		return write(root.getClass(), root);
	}

	public TypeWriter write(final Class<?> rootType, final Object root) {
		if (writer == null)
			writer = new RawWriter(stream);
		Walk.walk(rootType, root, new Walk.ObjectVisitor() {

			@Override
			public void visitString(final String value) {
				uncheck(() -> writer.quotedPrimitive(value.getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public void visitInteger(final Integer value) {
				uncheck(() -> writer.quotedPrimitive(value.toString().getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public void visitDouble(final Double value) {
				uncheck(() -> writer.quotedPrimitive(value.toString().getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public void visitBoolean(final Boolean value) {
				uncheck(() -> writer.quotedPrimitive((value ? "true" : "false").getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public void visitEnum(final Enum value) {
				uncheck(() -> writer.quotedPrimitive(Walk.decideEnumName(value).getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public void visitListStart(final List value) {
				uncheck(() -> writer.arrayBegin());
			}

			@Override
			public void visitListEnd(final List value) {
				uncheck(() -> writer.arrayEnd());
			}

			@Override
			public void visitSetStart(final Set value) {
				uncheck(() -> writer.arrayBegin());
			}

			@Override
			public void visitSetEnd(final Set value) {
				uncheck(() -> writer.arrayEnd());
			}

			@Override
			public void visitMapStart(final Map value) {
				uncheck(() -> writer.recordBegin());
			}

			@Override
			public void visitKeyBegin(final String key) {
				uncheck(() -> writer.key(key.getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public void visitKeyEnd(final String key) {

			}

			@Override
			public void visitMapEnd(final Map value) {
				uncheck(() -> writer.recordEnd());
			}

			@Override
			public boolean visitAbstractBegin(final Class<?> klass, final Object value) {
				uncheck(() -> writer.type(Walk.decideName(value.getClass()).getBytes(StandardCharsets.UTF_8)));
				return false;
			}

			@Override
			public void visitAbstractEnd(final Class<?> klass, final Object value) {

			}

			@Override
			public boolean visitConcreteBegin(final Class<?> klass, final Object value) {
				uncheck(() -> writer.recordBegin());
				return true;
			}

			@Override
			public void visitFieldBegin(final Field field, final Object value) {
				uncheck(() -> writer.key(Walk.decideName(field).getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public void visitFieldEnd(final Field field, final Object value) {

			}

			@Override
			public void visitConcreteEnd(final Class<?> klass, final Object value) {
				uncheck(() -> writer.recordEnd());
			}
		});
		return this;
	}
}
