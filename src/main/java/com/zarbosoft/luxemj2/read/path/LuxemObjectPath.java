package com.zarbosoft.luxemj2.read.path;

public class LuxemObjectPath extends LuxemPath {

	private String key;

	public LuxemObjectPath(final LuxemPath parent) {
		this.parent = parent;
	}

	public LuxemObjectPath(final LuxemPath parent, final String key) {
		this.parent = parent;
		this.key = key;
	}

	@Override
	public LuxemPath value() {
		return this;
	}

	@Override
	public LuxemPath key(final String data) {
		return new LuxemObjectPath(parent, data);
	}

	@Override
	public LuxemPath type() {
		return this;
	}

	@Override
	public String toString() {
		return String.format("%s/%s", parent == null ? "" : parent.toString(), key == null ? "" : key);
	}
}
