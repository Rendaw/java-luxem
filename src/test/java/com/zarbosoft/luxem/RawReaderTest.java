package com.zarbosoft.luxem;

import com.zarbosoft.luxem.read.BufferedRawReader;
import com.zarbosoft.luxem.read.InvalidStream;
import com.zarbosoft.luxem.read.RawReader;
import com.zarbosoft.luxem.read.source.LPrimitiveEvent;
import com.zarbosoft.pidgoon.events.Event;
import com.zarbosoft.pidgoon.events.MatchingEvent;
import com.zarbosoft.rendaw.common.Common;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.zip;

public class RawReaderTest {
	public List<Event> read(final String source) {
		return BufferedRawReader
				.streamEvents(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)),
						new RawReader.DefaultEventFactory()
				)
				.map(pair -> pair.first)
				.collect(Collectors.toList());
	}

	public void check(final String source, final Event... events) {
		final List<Event> got = read(source);
		final List<Event> expected = Arrays.asList(events);
		if (got.size() != expected.size())
			throw new AssertionError(String.format("Size mismatch:\nGot %s: %s\nExpected %s: %s",
					got.size(),
					got,
					expected.size(),
					expected
			));
		zip(got.stream(), expected.stream()).map(new Common.Enumerator<>()).forEach(pair -> {
			if (!((MatchingEvent) pair.second.second).matches((MatchingEvent) pair.second.first)) {
				throw new AssertionError(String.format("Stream mismatch at %s:\nGot: %s\nExpected: %s",
						pair.first,
						pair.second.first,
						pair.second.second
				));
			}
		});
	}

	@Test
	public void testEmpty() {
		check("");
	}

	@Test
	public void testRootSingle() {
		check("a", new LPrimitiveEvent("a"));
	}

	@Test
	public void testRootSingleComma() {
		check("a,", new LPrimitiveEvent("a"));
	}

	@Test
	public void testRootArray() {
		check("a,b", new LPrimitiveEvent("a"), new LPrimitiveEvent("b"));
	}

	@Test(expected = InvalidStream.class)
	public void testMultipleKeys() {
		read("{a: b: c}");
	}

	@Test(expected = InvalidStream.class)
	public void testKeyInRootArray() {
		System.out.format("%s\n", read("a: b"));
	}

	@Test(expected = InvalidStream.class)
	public void testKeyInArray() {
		System.out.format("%s\n", read("[a: b]"));
	}
}
