package com.zarbosoft.luxem.read.source;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.read.LuxemEvent;
import com.zarbosoft.pidgoon.events.Event;

@Configuration(name = "primitive")
public class LPrimitiveEvent implements LuxemEvent {
	public LPrimitiveEvent(final String value) {
		this.value = value;
	}

	public LPrimitiveEvent() {
	}

	@Configuration
	public String value;

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass() && (value == null || value.equals(((LPrimitiveEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("%s", value == null ? "*" : value);
	}
}
