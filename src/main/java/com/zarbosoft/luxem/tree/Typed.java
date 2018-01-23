package com.zarbosoft.luxem.tree;

public class Typed {
	public String name;
	public Object value;

	public Typed(final String type, final Object value) {
		this.name = type;
		this.value = value;
	}
}
