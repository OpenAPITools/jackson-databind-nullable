# jackson-databind-nullable

This module provides a `JsonNullable` wrapper class and a Jackson module to serialize/deserialize it.
The `JsonNullable` wrapper shall be used to wrap Java bean fields for which it is important to distinguish between an explicit `"null"` and the field not being present.
A typical usage is when implementing [Json Merge Patch](https://tools.ietf.org/html/rfc7386) where an explicit `"null"`has the meaning "set this field to null / remove this field" whereas a non-present field has the meaning "don't change the value of this field".

Note : a lot of people use `Optional` to bring this behavior.
Although it kinda works, it's not a good idea because:
* Beans shouldn't have `Optional` fields.
  `Optional` was designed to be used only as method return value.
* `Optional` should never be null.
  The goal of `Optional` is to wrap the `null` and prevent NPE so the code should be designed to never assign `null` to an `Optional`.
  A code invoking a method returning an Optional should be confident that this Optional is not null.
  
## Installation

The module is compatible with JDK6+
```
./mvnw clean install
```

## Usage

`JsonNullable` shall primarily be used in bean fields.

If we have the following class
```java
public static class Pet {
    public JsonNullable<String> name = JsonNullable.undefined();
    
    public Pet name(JsonNullable<String> name) {
        this.name = name;
        return this;
    }
}

```
Then we can serialize
```java
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
assertEquals("{}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>undefined())));
assertEquals("{\"name\":null}", mapper.writeValueAsString(new Pet().name(JsonNullable.<String>of(null))));
assertEquals("{\"name\":\"Rex\"}", mapper.writeValueAsString(new Pet().name(JsonNullable.of("Rex"))));

```
and deserialize
```java
assertEquals(JsonNullable.of("Rex"), mapper.readValue("{\"name\":\"Rex\"}", Pet.class).name);
assertEquals(JsonNullable.<String>of(null), mapper.readValue("{\"name\":null}", Pet.class).name);
assertEquals(JsonNullable.<String>undefined(), mapper.readValue("{}", Pet.class).name);

```

## Limitations

* Doesn't work when passed as a parameter to a `@JsonCreator` constructor (non present field gets deserialized as null instead of undefined).
  But as JsonNullable is here to represent "optional" values, there shouldn't be a need to put it in a constructor.
* Doesn't work with `@JsonUnwrapped`.
