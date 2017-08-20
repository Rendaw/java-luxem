package com.zarbosoft.luxem.read;

import java.io.ByteArrayOutputStream;

public class BufferedRawReader extends RawReader {
	@FunctionalInterface
	public interface BytesConsumer {
		void accept(byte[] b);
	}

	public VoidConsumer eatPrimitiveBegin = () -> {
	};
	public BytesConsumer eatPrimitive = b -> {
	};
	public VoidConsumer eatPrimitiveEnd = () -> {
	};
	public VoidConsumer eatTypeBegin = () -> {
	};
	public BytesConsumer eatType = b -> {
	};
	public VoidConsumer eatTypeEnd = () -> {
	};
	public VoidConsumer eatKeyBegin = () -> {
	};
	public BytesConsumer eatKey = b -> {
	};
	public VoidConsumer eatKeyEnd = () -> {
	};

	private ByteArrayOutputStream top = new ByteArrayOutputStream();
	boolean sent = false;
	private final Integer chunked;

	public BufferedRawReader() {
		this(null);
	}

	public BufferedRawReader(final Integer newChunked) {
		this.chunked = newChunked;
		super.eatPrimitiveBegin = () -> {
			eatPrimitiveBegin.run();
			top = new ByteArrayOutputStream();
			sent = false;
		};
		super.eatPrimitive = b -> {
			top.write(b);
			if (chunked != null && top.size() >= chunked) {
				eatPrimitive.accept(top.toByteArray());
				top.reset();
				sent = true;
			}
		};
		super.eatPrimitiveEnd = () -> {
			if (!sent || top.size() > 0) {
				eatPrimitive.accept(top.toByteArray());
				top.reset();
			}
			eatPrimitiveEnd.run();
		};
		super.eatTypeBegin = () -> {
			eatTypeBegin.run();
			top = new ByteArrayOutputStream();
			sent = false;
		};
		super.eatType = b -> {
			top.write(b);
			if (chunked != null && top.size() >= chunked) {
				eatType.accept(top.toByteArray());
				top.reset();
				sent = true;
			}
		};
		super.eatTypeEnd = () -> {
			if (!sent || top.size() > 0) {
				eatType.accept(top.toByteArray());
				top.reset();
			}
			eatTypeEnd.run();
		};
		super.eatKeyBegin = () -> {
			eatKeyBegin.run();
			top = new ByteArrayOutputStream();
			sent = false;
		};
		super.eatKey = b -> {
			top.write(b);
			if (chunked != null && top.size() >= chunked) {
				eatKey.accept(top.toByteArray());
				top.reset();
				sent = true;
			}
		};
		super.eatKeyEnd = () -> {
			if (!sent || top.size() > 0) {
				eatKey.accept(top.toByteArray());
				top.reset();
			}
			eatKeyEnd.run();
		};
	}
}
