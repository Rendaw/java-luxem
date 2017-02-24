package com.zarbosoft.luxemj2.read.path;

public class LuxemArrayPath extends LuxemPath {

	private boolean type = false;
	private int index = -1;

	public LuxemArrayPath(final LuxemPath parent) {
		this.parent = parent;
	}

	public LuxemArrayPath(final LuxemPath parent, final boolean type, final int index) {
		this.parent = parent;
		this.type = type;
		this.index = index;
	}

	@Override
	public LuxemPath value() {
		if (this.type)
			return new LuxemArrayPath(parent, false, index);
		else
			return new LuxemArrayPath(parent, false, index + 1);
	}

	@Override
	public LuxemPath key(final String data) {
		return this;
	}

	@Override
	public LuxemPath type() {
		return new LuxemArrayPath(parent, true, index + 1);
	}

	@Override
	public String toString() {
		return String.format("%s/%s",
				parent == null ? "" : parent.toString(),
				index == -1 ? "" : ((Integer) index).toString()
		);
	}
}
