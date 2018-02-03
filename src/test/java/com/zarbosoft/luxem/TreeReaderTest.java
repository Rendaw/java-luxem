package com.zarbosoft.luxem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TreeReaderTest {

	@Test
	public void testEmpty() {
		assertTrue(Luxem.parse("").isEmpty());
	}

	@Test
	public void testPrimitive() {
		assertThat(Luxem.parse("dog"), equalTo(ImmutableList.of("dog")));
	}

	@Test
	public void testArray() {
		assertThat(Luxem.parse("[]"), equalTo(ImmutableList.of(new ArrayList<>())));
	}

	@Test
	public void testArrayElement() {
		assertThat(Luxem.parse("[a]"), equalTo(ImmutableList.of(Arrays.asList("a"))));
	}

	@Test
	public void testRecord() {
		assertThat(Luxem.parse("{}"), equalTo(ImmutableList.of(new HashMap<>())));
	}

	@Test
	public void testRecordElement() {
		assertThat(Luxem.parse("{a:b}"), equalTo(ImmutableList.of(ImmutableMap.of("a", "b"))));
	}
}
