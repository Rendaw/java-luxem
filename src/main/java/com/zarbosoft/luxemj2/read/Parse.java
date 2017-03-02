package com.zarbosoft.luxemj2.read;

import com.zarbosoft.luxemj2.read.path.LuxemArrayPath;
import com.zarbosoft.luxemj2.read.path.LuxemPath;
import com.zarbosoft.luxemj2.read.source.*;
import com.zarbosoft.pidgoon.bytes.Callback;
import com.zarbosoft.pidgoon.events.EventStream;
import com.zarbosoft.pidgoon.internal.BaseParse;
import com.zarbosoft.rendaw.common.Pair;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

	private EventStream<O> createStream() {
		return new com.zarbosoft.pidgoon.events.Parse<O>()
				.grammar(grammar)
				.node(node)
				.stack(initialStack)
				.errorHistory(errorHistoryLimit)
				.dumpAmbiguity(dumpAmbiguity)
				.uncertainty(eventUncertainty)
				.callbacks((Map<String, Callback>) (Object) callbacks)
				.parse();
	}

	public Stream<O> parse(final InputStream stream) {
		final Pair<EventStream<O>, LuxemPath> state = new Pair<>(createStream(), new LuxemArrayPath(null));
		final BufferedRawReader reader = new BufferedRawReader();
		reader.eatRecordBegin = wrap(state, () -> new LObjectOpenEvent());
		reader.eatRecordEnd = wrap(state, () -> new LObjectCloseEvent());
		reader.eatArrayBegin = wrap(state, () -> new LArrayOpenEvent());
		reader.eatArrayEnd = wrap(state, () -> new LArrayCloseEvent());
		reader.eatKey = wrapBytes(state, s -> new LKeyEvent(s));
		reader.eatType = wrapBytes(state, s -> new LTypeEvent(s));
		reader.eatPrimitive = wrapBytes(state, s -> new LPrimitiveEvent(s));
		final RawReader.Feeder feeder = RawReader.feeder(reader, stream);
		return Stream.generate(() -> {
			boolean foodRemains = true;
			while (!state.first.ended() && (foodRemains = feeder.feed())) {
			}
			if (!foodRemains)
				feeder.finish();
			final O out = state.first.finish();
			if (foodRemains)
				state.first = createStream();
			else
				state.first = null;
			return out;
		}).takeWhile(o -> o != null);
	}

	private static <O> Runnable wrap(final Pair<EventStream<O>, LuxemPath> state, final Supplier<LuxemEvent> supplier) {
		return () -> {
			final LuxemEvent e = supplier.get();
			state.first = state.first.push(e, state.second.toString());
			state.second = state.second.push(e);
		};
	}

	private static <O> Consumer<byte[]> wrapBytes(
			final Pair<EventStream<O>, LuxemPath> state, final Function<String, LuxemEvent> supplier
	) {
		return bytes -> {
			final String string = new String(bytes, StandardCharsets.UTF_8);
			final LuxemEvent e = supplier.apply(string);
			state.first = state.first.push(e, state.second.toString());
			state.second = state.second.push(e);
		};
	}
}
