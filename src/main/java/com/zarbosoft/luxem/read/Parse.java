package com.zarbosoft.luxem.read;

import com.zarbosoft.pidgoon.events.Event;
import com.zarbosoft.pidgoon.events.EventStream;
import com.zarbosoft.pidgoon.events.Store;
import com.zarbosoft.pidgoon.internal.BaseParse;
import com.zarbosoft.pidgoon.internal.Callback;
import com.zarbosoft.rendaw.common.Pair;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.concatNull;
import static com.zarbosoft.rendaw.common.Common.iterable;

public class Parse<O> extends BaseParse<Parse<O>> {

	private int eventUncertainty = 20;
	private RawReader.EventFactory factory = null;

	private Parse(final Parse<O> other) {
		super(other);
		this.eventUncertainty = other.eventUncertainty;
		this.factory = other.factory;
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

	public Parse<O> eventFactory(final RawReader.EventFactory factory) {
		if (this.factory != null)
			throw new IllegalArgumentException("Factory already set");
		final Parse<O> out = split();
		out.factory = factory;
		return out;
	}

	public O parse(final String string) {
		return parse(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}

	public O parse(final InputStream stream) {
		return parse(RawReader.streamEvents(stream, factory == null ? new RawReader.DefaultEventFactory() : factory));
	}

	public O parse(final Stream<Pair<Event, Object>> stream) {
		EventStream<O> stream1 = new com.zarbosoft.pidgoon.events.Parse<O>()
				.grammar(grammar)
				.root(root)
				.stack(initialStack)
				.errorHistory(errorHistoryLimit)
				.dumpAmbiguity(dumpAmbiguity)
				.uncertainty(eventUncertainty)
				.callbacks((Map<Object, Callback<Store>>) (Object) callbacks)
				.parse();
		for (final Pair<Event, Object> pair : iterable(stream))
			stream1 = stream1.push(pair.first, pair.second);
		return stream1.finish();
	}

	public Stream<O> parseByElement(final String string) {
		return parseByElement(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
	}

	public Stream<O> parseByElement(final InputStream stream) {
		return parseByElement(RawReader.streamEvents(stream,
				factory == null ? new RawReader.DefaultEventFactory() : factory
		));
	}

	public Stream<O> parseByElement(final Stream<Pair<Event, Object>> stream) {
		class State {
			EventStream<O> stream = null;

			private void createStream() {
				stream = new com.zarbosoft.pidgoon.events.Parse<O>()
						.grammar(grammar)
						.root(root)
						.stack(initialStack)
						.errorHistory(errorHistoryLimit)
						.dumpAmbiguity(dumpAmbiguity)
						.uncertainty(eventUncertainty)
						.callbacks((Map<Object, Callback<Store>>) (Object) callbacks)
						.parse();
			}

			public void handleEvent(final Pair<Event, Object> pair) {
				stream = stream.push(pair.first, pair.second);
			}
		}
		final State state = new State();
		return concatNull(stream).map(pair -> {
			if (pair == null) {
				if (state.stream == null)
					return null;
				else
					throw new InvalidStream("Input stream ended mid-element.");
			}
			if (state.stream == null)
				state.createStream();
			state.handleEvent(pair);
			if (state.stream.ended()) {
				O result = state.stream.finish();
				state.stream = null;
				return result;
			} else
				return null;
		}).filter(o -> o != null);
	}
}
