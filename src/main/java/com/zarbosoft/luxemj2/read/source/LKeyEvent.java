package com.zarbosoft.luxemj2.read.source;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxemj2.read.LuxemEvent;
import com.zarbosoft.pidgoon.events.Event;

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
	public boolean matches(final Event event) {
		return event.getClass() == getClass() && (value == null || value.equals(((LKeyEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("%s:", value == null ? "*" : value);
	}
}
