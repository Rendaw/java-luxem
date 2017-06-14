package com.zarbosoft.luxem.read.source;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.read.LuxemEvent;
import com.zarbosoft.pidgoon.events.MatchingEvent;

@Configuration(name = "type")
public class LTypeEvent implements LuxemEvent {

	public LTypeEvent(final String string) {
		this.value = string;
	}

	public LTypeEvent() {
	}

	@Configuration
	public String value;

	@Override
	public boolean matches(final MatchingEvent event) {
		return event.getClass() == getClass() && (value == null || value.equals(((LTypeEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("(%s)", value == null ? "*" : value);
	}
}
