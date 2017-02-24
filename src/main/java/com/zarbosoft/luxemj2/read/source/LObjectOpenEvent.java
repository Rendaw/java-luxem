package com.zarbosoft.luxemj2.read.source;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxemj2.read.LuxemEvent;
import com.zarbosoft.pidgoon.events.Event;

@Configuration(name = "object-open")
public class LObjectOpenEvent implements LuxemEvent {

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass();
	}

	@Override
	public String toString() {
		return String.format("{");
	}
}
