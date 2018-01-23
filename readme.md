# luxem

A library for reading and writing `luxem` in Java.

# Maven

```
<dependency>
    <groupId>com.zarbosoft</groupId>
    <artifactId>luxem</artifactId>
    <version>0.0.1</version>
</dependency>
```

# Basic use cases

### 1. Read a document with no known type
```
System.out.format(
	"%s\n",
	 ((Map)Luxem.parse("[a, {b: c}]").get(1)).get("b")
);
```

### 2. Read a document into a list of annotated types
```
@Configuration
public class MyType {
	@Configuration
	public int a;
}

System.out.format(
	"%s\n",
	 Luxem.parse(new Reflections(), new TypeInfo(MyType.class), "{a: 4},").get(0).a
);
```

Output
```
4
```

### 3. Write a document from an annotated type
```
@Configuration
public class MyType {
	@Configuration
	public int a;
}

MyType x = new MyType();
x.a = 4;
Luxem.write(x, System.out);
```

Output
```
{a:4,},
```

If you have multiple root elements, use `TypeWriter` and call `write` for each element.

### 4. Write a document manually
```
RawWriter writer = new RawWriter(System.out);
writer.recordBegin();
writer.key("a");
writer.primitive("4");
writer.recordEnd();
```

Output
```
{a:4,},
```

# Other features

* Deserialize to an event stream for use with pidgoon
* Pretty print (indent)
* Write a tree of `Map`, `List`, `Typed`, and `toString`able objects (reverse of example 1) with `TreeWriter`
* Tokenize luxem `RawReader`

See the Javadoc (not hosted currently) for details.