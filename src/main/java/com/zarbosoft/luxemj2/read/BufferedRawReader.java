package com.zarbosoft.luxemj2.read;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

public class BufferedRawReader extends RawReader {
	public Runnable eatPrimitiveBegin = () -> {
	};
	public Consumer<byte[]> eatPrimitive = b -> {
	};
	public Runnable eatPrimitiveEnd = () -> {
	};
	public Runnable eatTypeBegin = () -> {
	};
	public Consumer<byte[]> eatType = b -> {
	};
	public Runnable eatTypeEnd = () -> {
	};
	public Runnable eatKeyBegin = () -> {
	};
	public Consumer<byte[]> eatKey = b -> {
	};
	public Runnable eatKeyEnd = () -> {
	};

	private ByteArrayOutputStream top = new ByteArrayOutputStream();
	private final Integer chunked;

	public BufferedRawReader() {
		this(null);
	}

	public BufferedRawReader(final Integer newChunked) {
		this.chunked = newChunked;
		super.eatPrimitiveBegin = () -> {
			eatPrimitiveBegin.run();
			top = new ByteArrayOutputStream();
		};
		super.eatPrimitive = b -> {
			top.write(b);
			if (chunked != null && top.size() >= chunked) {
				eatPrimitive.accept(top.toByteArray());
				top.reset();
			}
		};
		super.eatPrimitiveEnd = () -> {
			if (top.size() > 0) {
				eatPrimitive.accept(top.toByteArray());
				top.reset();
			}
			eatPrimitiveEnd.run();
		};
		super.eatTypeBegin = () -> {
			eatTypeBegin.run();
			top = new ByteArrayOutputStream();
		};
		super.eatType = b -> {
			top.write(b);
			if (chunked != null && top.size() >= chunked) {
				eatType.accept(top.toByteArray());
				top.reset();
			}
		};
		super.eatTypeEnd = () -> {
			if (top.size() > 0) {
				eatType.accept(top.toByteArray());
				top.reset();
			}
			eatTypeEnd.run();
		};
		super.eatKeyBegin = () -> {
			eatKeyBegin.run();
			top = new ByteArrayOutputStream();
		};
		super.eatKey = b -> {
			top.write(b);
			if (chunked != null && top.size() >= chunked) {
				eatKey.accept(top.toByteArray());
				top.reset();
			}
		};
		super.eatKeyEnd = () -> {
			if (top.size() > 0) {
				eatKey.accept(top.toByteArray());
				top.reset();
			}
			eatKeyEnd.run();
		};
	}
}
