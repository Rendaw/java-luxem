package com.zarbosoft.luxem.read;

import com.zarbosoft.luxem.read.path.LuxemArrayPath;
import com.zarbosoft.luxem.read.path.LuxemPath;
import com.zarbosoft.pidgoon.events.EventStream;
import com.zarbosoft.pidgoon.events.Store;
import com.zarbosoft.pidgoon.internal.BaseParse;
import com.zarbosoft.pidgoon.internal.Callback;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.concatNull;

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
		return parse(RawReader.streamEvents(stream));
	}

	public Stream<O> parse(final Stream<LuxemEvent> stream) {
		class State {
			EventStream<O> stream = null;
			LuxemPath path;

			private void createStream() {
				stream = new com.zarbosoft.pidgoon.events.Parse<O>()
						.grammar(grammar)
						.node(node)
						.stack(initialStack)
						.errorHistory(errorHistoryLimit)
						.dumpAmbiguity(dumpAmbiguity)
						.uncertainty(eventUncertainty)
						.callbacks((Map<Object, Callback<Store>>) (Object) callbacks)
						.parse();
			}

			State() {
				this.path = new LuxemArrayPath(null);
			}

			public void handleEvent(final LuxemEvent e) {
				stream = stream.push(e, path.toString());
				path = path.push(e);
			}
		}
		final State state = new State();
		return concatNull(stream).map(event -> {
			if (event == null) {
				if (state.stream == null)
					return null;
				else
					throw new InvalidStream("Input stream ended mid-element.");
			}
			if (state.stream == null)
				state.createStream();
			state.handleEvent(event);
			if (state.stream.ended()) {
				O result = state.stream.finish();
				state.stream = null;
				return result;
			} else
				return null;
		}).filter(o -> o != null);
	}

}
