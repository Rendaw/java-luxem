package com.zarbosoft.luxem.read.source;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.read.LuxemEvent;
import com.zarbosoft.pidgoon.events.MatchingEvent;

@Configuration(name = "key")
public class LKeyEvent implements LuxemEvent {
	public LKeyEvent(final String string) {
		value = string;
	}

	public LKeyEvent() {
	}

	@Configuration
	public String value;

	@Override
	public boolean matches(final MatchingEvent event) {
		return event.getClass() == getClass() && (value == null || value.equals(((LKeyEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("%s:", value == null ? "*" : value);
	}
}
