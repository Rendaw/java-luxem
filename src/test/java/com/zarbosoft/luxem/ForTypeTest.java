package com.zarbosoft.luxem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.pidgoon.InvalidStream;
import org.junit.Test;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

public class ForTypeTest {
	public static Reflections reflections = new Reflections("com.zarbosoft.luxem");

	private void check(final Walk.TypeInfo k, final String source, final Object expected) {
		final Object got = Luxem.parse(reflections, k, source).findFirst().get();
		assertReflectionEquals(expected, got);
	}

	private void check(final Class<?> k, final String source, final Object expected) {
		check(new Walk.TypeInfo(k), source, expected);
	}

	@Test
	public void testString() {
		check(String.class, "dog", "dog");
	}

	@Test
	public void testRootArray() {
		final List<String> got = Luxem.<String>parse(reflections,
				new Walk.TypeInfo(String.class),
				"\"l2:food\",\"online\",\"l1:expense\","
		).collect(Collectors.toList());
		assertReflectionEquals(ImmutableList.of("l2:food", "online", "l1:expense"), got);
	}

	@Test
	public void testStringNewlineSymbol() {
		check(String.class, "\"d\\nog\"", "d\nog");
	}

	@Test
	public void testStringNewlineLiteral() {
		check(String.class, "\"d\nog\"", "d\nog");
	}

	@Test(expected = InvalidStream.class)
	public void testStringFail() {
		check(String.class, "{}", "dog");
	}

	@Test
	public void testInteger() {
		check(Integer.class, "4007", 4007);
	}

	@Test(expected = InvalidStream.class)
	public void testIntegerFail() {
		check(Integer.class, "hamlet,", 4);
	}

	@Test
	public void testDouble() {
		check(Double.class, "4.7", 4.7);
		check(Double.class, "4", 4.0);
	}

	@Test(expected = InvalidStream.class)
	public void testDoubleFail() {
		check(Double.class, "hamlet,", 4.0);
	}

	@Test
	public void testBoolean() {
		check(Boolean.class, "true", true);
		check(Boolean.class, "false", false);
	}

	@Test(expected = InvalidStream.class)
	public void testBooleanFail() {
		check(Boolean.class, "1,", false);
	}

	@Configuration
	public static class Subject {
		public Subject() {
		}

		@Configuration
		public List<Integer> a = new ArrayList<>();

		public Subject(final List<Integer> a) {
			this.a = a;
		}
	}

	@Configuration
	public static class Subject2 {
		public Subject2() {
		}
	}

	@Configuration
	public static class Subject3 {
		public Subject3() {
		}

		@Configuration
		public Set<Integer> a;

		public Subject3(final Set<Integer> a) {
			this.a = a;
		}
	}

	@Configuration
	public static class Subject4 {
		public Subject4() {
		}

		@Configuration
		public Map<String, Integer> a = new HashMap<>();

		public Subject4(final Map<String, Integer> a) {
			this.a = a;
		}
	}

	@Configuration
	public static class Subject5 {
		public Subject5() {
		}

		@Configuration
		public String a;

		public Subject5(final String a) {
			this.a = a;
		}
	}

	@Test
	public void testClassAndList() {
		check(Subject.class, "{\"a\": [7, 14]}", new Subject(Arrays.asList(new Integer[] {7, 14})));
	}

	@Test
	public void testOptionalList() {
		check(Subject.class, "{}", new Subject(Arrays.asList(new Integer[] {})));
	}

	@Test
	public void testClassAndSet() {
		check(Subject3.class, "{\"a\": [7, 14]}", new Subject3(new HashSet<>(Arrays.asList(7, 14))));
	}

	@Test
	public void testClassAndMap() {
		final HashMap<String, Integer> data = new HashMap<>();
		data.put("h", 7);
		data.put("q", 12);
		check(Subject4.class, "{\"a\": {h:7, q:12}}", new Subject4(data));
	}

	@Test
	public void testOptionalMap() {
		check(Subject4.class, "{}", new Subject4(new HashMap<>()));
	}

	@Test
	public void test1FieldAbbreviation() {
		check(Subject.class, "[7, 14]", new Subject(Arrays.asList(new Integer[] {7, 14})));
	}

	@Test
	public void test0FieldClass() {
		check(Subject2.class, "{}", new Subject2());
	}

	@Configuration
	public static class Outer5 {
		@Configuration
		public List<Subject5> data;

		public Outer5(final List<Subject5> q) {
			data = q;
		}

		public Outer5() {

		}
	}

	@Test
	public void testRepeatedAbbreviations() {
		check(Outer5.class,
				"{data:[q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q,q]}",
				new Outer5(Arrays.asList(new Subject5[] {
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q"),
						new Subject5("q")
				}))
		);
	}

	@Configuration
	public static abstract class Subject6 {
	}

	@Configuration(name = "6a")
	public static class Subject6A extends Subject6 {
		public Subject6A() {
		}

		@Configuration
		public String a;

		@Configuration(optional = true)
		public String b;
		@Configuration(optional = true)
		public String c;

		public Subject6A(final String a) {
			this.a = a;
		}
	}

	@Configuration(name = "6b")
	public static class Subject6B extends Subject6 {
		public Subject6B() {
		}

		@Configuration
		public String a;

		public Subject6B(final String a) {
			this.a = a;
		}
	}

	@Configuration(name = "6c")
	public static class Subject6C extends Subject6 {
		public Subject6C() {
		}

		@Configuration
		public String a;

		public Subject6C(final String a) {
			this.a = a;
		}
	}

	@Configuration
	public static class Outer6 {
		@Configuration
		public List<Subject6> data;

		public Outer6(final List<Subject6> q) {
			data = q;
		}

		public Outer6() {

		}
	}

	@Test
	public void testRepeatedPolymorphicAbbreviations() {
		check(
				Outer6.class,
				"{data:[(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q,(6a)q]}",
				new Outer6(Arrays.asList(new Subject6[] {
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q"),
						new Subject6A("q")
				}))
		);
	}

	@Test
	public void testGenericRootList() {
		check(new Walk.TypeInfo(List.class, new Walk.TypeInfo(String.class)), "[a,b,c]", Arrays.asList("a", "b", "c"));
	}

	@Test
	public void testGenericRootMap() {
		check(
				new Walk.TypeInfo(Map.class, new Walk.TypeInfo(String.class), new Walk.TypeInfo(String.class)),
				"{a:1,b:2}",
				ImmutableMap.of("a", "1", "b", "2")
		);
	}

}
