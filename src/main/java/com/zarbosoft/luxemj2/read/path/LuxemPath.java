package com.zarbosoft.luxemj2.read.path;

import com.zarbosoft.luxemj2.read.LuxemEvent;
import com.zarbosoft.luxemj2.read.source.*;

public abstract class LuxemPath {

	public LuxemPath parent;

	public abstract LuxemPath value();

	public abstract LuxemPath key(String data);

	public abstract LuxemPath type();

	public LuxemPath pop() {
		return parent;
	}

	public LuxemPath push(final LuxemEvent e) {
		if (e.getClass() == LArrayOpenEvent.class) {
			return new LuxemArrayPath(value());
		} else if (e.getClass() == LArrayCloseEvent.class) {
			return pop();
		} else if (e.getClass() == LObjectOpenEvent.class) {
			return new LuxemObjectPath(value());
		} else if (e.getClass() == LObjectCloseEvent.class) {
			return pop();
		} else if (e.getClass() == LKeyEvent.class) {
			return key(((LKeyEvent) e).value);
		} else if (e.getClass() == LTypeEvent.class) {
			return type();
		} else if (e.getClass() == LPrimitiveEvent.class) {
			return value();
		} else
			throw new AssertionError(String.format("Unknown luxem event type [%s]", e.getClass()));
	}
}
