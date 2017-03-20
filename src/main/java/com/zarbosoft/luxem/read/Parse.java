package com.zarbosoft.luxem.read;

import com.zarbosoft.luxem.read.path.LuxemArrayPath;
import com.zarbosoft.luxem.read.path.LuxemPath;
import com.zarbosoft.luxem.read.source.*;
import com.zarbosoft.pidgoon.bytes.Callback;
import com.zarbosoft.pidgoon.events.EventStream;
import com.zarbosoft.pidgoon.internal.BaseParse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.drain;
import static com.zarbosoft.rendaw.common.Common.enumerate;

public class Parse<O> extends BaseParse<Parse<O>> {

	private int eventUncertainty = 20;

	private Parse(final Parse<O> other) {
		super(other);
		this.eventUncertainty = other.eventUncertainty;
	}

	@Override
	protected Parse<O> split() {
		return new Parse<>(this);
	}

	public Parse() {
	}

	public Parse<O> eventUncertainty(final int limit) {
		if (eventUncertainty != 20)
			throw new IllegalArgumentException("Max event uncertainty already set");
		final Parse<O> out = split();
		out.eventUncertainty = limit;
		return out;
	}

	public Stream<O> parse(final String string) {
		return parse(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}

	public Stream<O> parse(final InputStream stream) {
		class State {
			EventStream<O> stream;
			LuxemPath path;
			Deque<LuxemEvent> events = new ArrayDeque<>();

			private void createStream() {
				stream = new com.zarbosoft.pidgoon.events.Parse<O>()
						.grammar(grammar)
						.node(node)
						.stack(initialStack)
						.errorHistory(errorHistoryLimit)
						.dumpAmbiguity(dumpAmbiguity)
						.uncertainty(eventUncertainty)
						.callbacks((Map<String, Callback>) (Object) callbacks)
						.parse();
			}

			State() {
				this.path = new LuxemArrayPath(null);
				createStream();
			}

			<O> Runnable wrap(final Supplier<LuxemEvent> supplier) {
				return () -> {
					events.addLast(supplier.get());
				};
			}

			<O> Consumer<byte[]> wrapBytes(
					final Function<String, LuxemEvent> supplier
			) {
				return bytes -> {
					final String string = new String(bytes, StandardCharsets.UTF_8);
					final LuxemEvent e = supplier.apply(string);
					events.addLast(e);
				};
			}

			public void handleEvent(final LuxemEvent e) {
				stream = stream.push(e, path.toString());
				path = path.push(e);
			}
		}
		final State state = new State();
		final BufferedRawReader reader = new BufferedRawReader();
		reader.eatRecordBegin = state.wrap(() -> new LObjectOpenEvent());
		reader.eatRecordEnd = state.wrap(() -> new LObjectCloseEvent());
		reader.eatArrayBegin = state.wrap(() -> new LArrayOpenEvent());
		reader.eatArrayEnd = state.wrap(() -> new LArrayCloseEvent());
		reader.eatKey = state.wrapBytes(s -> new LKeyEvent(s));
		reader.eatType = state.wrapBytes(s -> new LTypeEvent(s));
		reader.eatPrimitive = state.wrapBytes(s -> new LPrimitiveEvent(s));
		return RawReader.stream(reader, stream).flatMap(last -> {
			return enumerate(drain(state.events)).map(pair -> {
				int i = pair.first;
				state.handleEvent(pair.second);
				if (state.stream.ended()) {
					O result = state.stream.finish();
					state.createStream();
					return result;
				} else if (i == state.events.size() - 1 && last) {
					throw new InvalidStream("Input stream ended mid-element.");
				} else
					return null;
			}).filter(o -> o != null);
		});
	}

}
