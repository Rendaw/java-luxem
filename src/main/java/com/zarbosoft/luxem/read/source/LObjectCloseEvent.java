package com.zarbosoft.luxem.read.source;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.read.LuxemEvent;
import com.zarbosoft.pidgoon.events.MatchingEvent;

@Configuration(name = "object-close")
public class LObjectCloseEvent implements LuxemEvent {

	@Override
	public boolean matches(final MatchingEvent event) {
		return event.getClass() == getClass();
	}

	@Override
	public String toString() {
		return String.format("}");
	}
}
