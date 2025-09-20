/*
 * Copyright 2025 Christian Trimble
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openapitools.jackson.nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import static org.openapitools.jackson.nullable.JsonNullable.of;
import static org.openapitools.jackson.nullable.JsonNullable.undefined;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

/**
 * Tests for Stream API methods patterned after java.util.Optional.
 * 
 * @author Christian Trimble
 */
@RunWith(Enclosed.class)
public class StreamingApiTest {

  static String VALUE = "value";
  static String OTHER = "other";
  static String NULL = null;
  static JsonNullable<String> JSON_VALUE = of(VALUE);
  static JsonNullable<String> JSON_OTHER = of(OTHER);
  static JsonNullable<String> JSON_NULL  = of(null);
  static JsonNullable<String> UNDEFINED  = undefined();

  @RunWith(Parameterized.class)
  public static class OrTest extends OneArgumentTest {
    static BiFunction<JsonNullable<String>, Supplier<JsonNullable<String>>, JsonNullable<String>> OR = JsonNullable::or;
    static Supplier<Throwable> NULL_SUPPLIER = null;
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "or value supplied on value",     OR, JSON_VALUE, supplier(JSON_OTHER), equalTo(JSON_VALUE) },
            { "or value supplied on null",      OR, JSON_NULL,  supplier(JSON_OTHER), equalTo(JSON_NULL) },
            { "or value supplied on undefined", OR, UNDEFINED,  supplier(JSON_OTHER), equalTo(JSON_OTHER) },
            { "or null supplied on value",      OR, JSON_VALUE, supplier(NULL),       equalTo(JSON_VALUE) },
            { "or null supplied on null",       OR, JSON_NULL,  supplier(NULL),       equalTo(JSON_NULL) },
            { "or null supplied on undefined",  OR, UNDEFINED,  supplier(NULL),       instanceOf(NullPointerException.class) },
            { "or null supplier on value",      OR, JSON_VALUE, NULL_SUPPLIER,        equalTo(JSON_VALUE) },
            { "or null supplier on null",       OR, JSON_NULL,  NULL_SUPPLIER,        equalTo(JSON_NULL) },
            { "or null supplier on undefined",  OR, UNDEFINED,  NULL_SUPPLIER,        instanceOf(NullPointerException.class) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class OrElseTest extends OneArgumentTest {
    static BiFunction<JsonNullable<String>, String, String> OR_ELSE = JsonNullable::orElse;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElse on value",     OR_ELSE, JSON_VALUE, OTHER, equalTo(VALUE) },
            { "orElse on null",      OR_ELSE, JSON_NULL,  OTHER, nullValue() },
            { "orElse on undefined", OR_ELSE, UNDEFINED,  OTHER, equalTo(OTHER) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class OrElseGetTest extends OneArgumentTest {
    static BiFunction<JsonNullable<String>, Supplier<String>, String> OR_ELSE_GET = JsonNullable::orElseGet;
    static Supplier<String> NULL_SUPPLIER = null;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElseGet value supplied on value",     OR_ELSE_GET, JSON_VALUE, supplier(OTHER), equalTo(VALUE) },
            { "orElseGet value supplied on null",      OR_ELSE_GET, JSON_NULL,  supplier(OTHER), nullValue() },
            { "orElseGet value supplied on undefined", OR_ELSE_GET, UNDEFINED,  supplier(OTHER), equalTo(OTHER) },
            { "orElseGet null supplier on value",      OR_ELSE_GET, JSON_VALUE, NULL_SUPPLIER,   equalTo(VALUE) },
            { "orElseGet null supplier on null",       OR_ELSE_GET, JSON_NULL,  NULL_SUPPLIER,   nullValue() },
            { "orElseGet null supplier on undefined",  OR_ELSE_GET, UNDEFINED,  NULL_SUPPLIER,   instanceOf(NullPointerException.class) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class OrElseThrowTest extends NoArgumentsTest {
    static Function<JsonNullable<String>, String> OR_ELSE_THROW = JsonNullable::orElseThrow;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElseThrow on value",     OR_ELSE_THROW, JSON_VALUE, equalTo(VALUE) },
            { "orElseThrow on null",      OR_ELSE_THROW, JSON_NULL,  nullValue() },
            { "orElseThrow on undefined", OR_ELSE_THROW, UNDEFINED,  instanceOf(NoSuchElementException.class) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class OrElseThrowWithSupplierTest extends OneArgumentWithThrowsTest {
    public static class TestException extends Exception {}
    static BiFunctionWithThrows<JsonNullable<String>, Supplier<Throwable>, String> OR_ELSE_THROW = JsonNullable::orElseThrow;
    static Supplier<Throwable> NULL_SUPPLIER = null;
    static Supplier<Throwable> NULL_SUPPLIED = ()->null;
    static Supplier<Throwable> TEST_EXCEPTION = TestException::new;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElseThrow with supplier on value",     OR_ELSE_THROW, JSON_VALUE, TEST_EXCEPTION, equalTo(VALUE) },
            { "orElseThrow with supplier on null",      OR_ELSE_THROW, JSON_NULL,  TEST_EXCEPTION, nullValue() },
            { "orElseThrow with supplier on undefined", OR_ELSE_THROW, UNDEFINED,  TEST_EXCEPTION, instanceOf(TestException.class) },
            { "orElseThrow null supplier on value",     OR_ELSE_THROW, JSON_VALUE, NULL_SUPPLIER,  equalTo(VALUE) },
            { "orElseThrow null supplier on null",      OR_ELSE_THROW, JSON_NULL,  NULL_SUPPLIER,  nullValue() },
            { "orElseThrow null supplier on undefined", OR_ELSE_THROW, UNDEFINED,  NULL_SUPPLIER,  instanceOf(NullPointerException.class) },
            { "orElseThrow null supplied on value",     OR_ELSE_THROW, JSON_VALUE, NULL_SUPPLIED,  equalTo(VALUE) },
            { "orElseThrow null supplied on null",      OR_ELSE_THROW, JSON_NULL,  NULL_SUPPLIED,  nullValue() },
            { "orElseThrow null supplied on undefined", OR_ELSE_THROW, UNDEFINED,  NULL_SUPPLIED,  instanceOf(NullPointerException.class) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class MapTest extends OneArgumentTest {
    static BiFunction<JsonNullable<String>, Function<String, Object>, JsonNullable<Object>> MAP = JsonNullable::map;

    static Function<String, String> MAPPING = value->value!=null?value+" mapped":"null mapped";
    static Function<String, String> NULL_MAPPING = null;
    
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "map on value",                  MAP, JSON_VALUE, MAPPING,      equalTo(of("value mapped")) },
            { "map on null",                   MAP, JSON_NULL,  MAPPING,      equalTo(of("null mapped")) },
            { "map on undefined",              MAP, UNDEFINED,  MAPPING,      equalTo(UNDEFINED) },
            { "map null mapping on value",     MAP, JSON_VALUE, NULL_MAPPING, instanceOf(NullPointerException.class) },
            { "map null mapping on null",      MAP, JSON_NULL,  NULL_MAPPING, instanceOf(NullPointerException.class) },
            { "map null mapping on undefined", MAP, UNDEFINED,  NULL_MAPPING, equalTo(UNDEFINED) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class FilterTest extends OneArgumentTest {
    static BiFunction<JsonNullable<String>, Predicate<String>, JsonNullable<String>> FILTER = JsonNullable::filter;

    static Predicate<?> KEEP_ALL = value->true;
    static Predicate<?> KEEP_NULL = value->value==null;
    static Predicate<?> KEEP_VALUE = value->value!=null;
    static Predicate<?> KEEP_NONE = value->false;
    static Predicate<?> NULL_PREDICATE = null;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "filter keep all on value",           FILTER, JSON_VALUE, KEEP_ALL,        equalTo(JSON_VALUE) },
            { "filter keep all on null",            FILTER, JSON_NULL,  KEEP_ALL,        equalTo(JSON_NULL) },
            { "filter keep all on undefined",       FILTER, UNDEFINED,  KEEP_ALL,        equalTo(UNDEFINED) },
            { "filter keep value on value",         FILTER, JSON_VALUE, KEEP_VALUE,      equalTo(JSON_VALUE) },
            { "filter keep value on null",          FILTER, JSON_NULL,  KEEP_VALUE,      equalTo(UNDEFINED) },
            { "filter keep value on undefined",     FILTER, UNDEFINED,  KEEP_VALUE,      equalTo(UNDEFINED) },
            { "filter keep null on value",          FILTER, JSON_VALUE, KEEP_NULL,       equalTo(UNDEFINED) },
            { "filter keep null on null",           FILTER, JSON_NULL,  KEEP_NULL,       equalTo(JSON_NULL) },
            { "filter keep null on undefined",      FILTER, UNDEFINED,  KEEP_NULL,       equalTo(UNDEFINED) },
            { "filter remove on value",             FILTER, JSON_VALUE, KEEP_NONE,       equalTo(UNDEFINED) },
            { "filter remove on null",              FILTER, JSON_NULL,  KEEP_NONE,       equalTo(UNDEFINED) },
            { "filter remove on undefined",         FILTER, UNDEFINED,  KEEP_NONE,       equalTo(UNDEFINED) },
            { "filter null predicate on value",     FILTER, JSON_VALUE, NULL_PREDICATE,  instanceOf(NullPointerException.class) },
            { "filter null predicate on null",      FILTER, JSON_NULL,  NULL_PREDICATE,  instanceOf(NullPointerException.class) },
            { "filter null predicate on undefined", FILTER, UNDEFINED,  NULL_PREDICATE,  equalTo(UNDEFINED) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class FlatMapTest extends OneArgumentTest {
    static BiFunction<JsonNullable<String>, Function<String, JsonNullable<Object>>, JsonNullable<Object>> FLAT_MAP = JsonNullable::flatMap;

    static Function<String, JsonNullable<String>> ANY_TO_UNDEFINED = value->UNDEFINED;
    static Function<String, JsonNullable<String>> NULL_TO_UNDEFINED = value->value!=null?of(value):UNDEFINED;
    static Function<String, JsonNullable<String>> ANY_TO_OTHER = value->JSON_OTHER;
    static Function<String, JsonNullable<String>> NULL_MAPPING = null;
    static Function<String, JsonNullable<String>> NULL_RESULT = value->null;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "flatMap any to undefined on value",      FLAT_MAP, JSON_VALUE, ANY_TO_UNDEFINED,  equalTo(UNDEFINED) },
            { "flatMap any to undefined on null",       FLAT_MAP, JSON_NULL,  ANY_TO_UNDEFINED,  equalTo(UNDEFINED) },
            { "flatMap any to undefined on undefined",  FLAT_MAP, UNDEFINED,  ANY_TO_UNDEFINED,  equalTo(UNDEFINED) },
            { "flatMap null to undefined on value",     FLAT_MAP, JSON_VALUE, NULL_TO_UNDEFINED, equalTo(JSON_VALUE) },
            { "flatMap null to undefined on null",      FLAT_MAP, JSON_NULL,  NULL_TO_UNDEFINED, equalTo(UNDEFINED) },
            { "flatMap null to undefined on undefined", FLAT_MAP, UNDEFINED,  NULL_TO_UNDEFINED, equalTo(UNDEFINED) },
            { "flatMap any to other on value",          FLAT_MAP, JSON_VALUE, ANY_TO_OTHER,      equalTo(JSON_OTHER) },
            { "flatMap any to other on null",           FLAT_MAP, JSON_NULL,  ANY_TO_OTHER,      equalTo(JSON_OTHER) },
            { "flatMap any to other on undefined",      FLAT_MAP, UNDEFINED,  ANY_TO_OTHER,      equalTo(UNDEFINED) },
            { "flatMap null mapping on value",          FLAT_MAP, JSON_VALUE, NULL_MAPPING,      instanceOf(NullPointerException.class) },
            { "flatMap null mapping on null",           FLAT_MAP, JSON_NULL,  NULL_MAPPING,      instanceOf(NullPointerException.class) },
            { "flatMap null mapping on undefined",      FLAT_MAP, UNDEFINED,  NULL_MAPPING,      equalTo(UNDEFINED) },
            { "flatMap null result on value",           FLAT_MAP, JSON_VALUE, NULL_RESULT,       instanceOf(NullPointerException.class) },
            { "flatMap null result on null",            FLAT_MAP, JSON_NULL,  NULL_RESULT,       instanceOf(NullPointerException.class) },
            { "flatMap null result on undefined",       FLAT_MAP, UNDEFINED,  NULL_RESULT,       equalTo(UNDEFINED) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class IsUndefinedTest extends NoArgumentsTest {
    static Function<JsonNullable<String>, Boolean> IS_UNDEFINED = JsonNullable::isUndefined;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "isUndefined on value",      IS_UNDEFINED, JSON_VALUE, equalTo(false) },
            { "isUndefined on null",       IS_UNDEFINED, JSON_NULL,  equalTo(false) },
            { "isUndefined on undefined",  IS_UNDEFINED, UNDEFINED,  equalTo(true) },
        });
      }
  }

  @RunWith(Parameterized.class)
  public static class StreamTest extends NoArgumentsTest {
    static Function<JsonNullable<String>, Stream<String>> STREAM = JsonNullable::stream;
    static Function<JsonNullable<String>, List<String>> STREAM_TO_LIST = STREAM.andThen(s->s.collect(Collectors.toList()));

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "stream on value",     STREAM_TO_LIST, JSON_VALUE, contains(VALUE) },
            { "stream on null",      STREAM_TO_LIST, JSON_NULL,  contains(NULL) },
            { "stream on undefined", STREAM_TO_LIST, UNDEFINED,  empty() }
        });
      }
  }

  public static class IfPresentOrElseTest {
    @SuppressWarnings("unchecked")
    Consumer<String> whenPresent = mock(Consumer.class);
    Runnable whenNotPresent = mock(Runnable.class);

    @Before
    public void resetMocks() {
      Mockito.reset(whenPresent, whenNotPresent);
    }

    @Test
    public void whenValuePresent() {
      JSON_VALUE.ifPresentOrElse(whenPresent, whenNotPresent);

      verify(whenPresent).accept(VALUE);
      verifyNoMoreInteractions(whenPresent, whenNotPresent);
    }

    @Test
    public void whenNullPresent() {
      JSON_NULL.ifPresentOrElse(whenPresent, whenNotPresent);

      verify(whenPresent).accept(NULL);
      verifyNoMoreInteractions(whenPresent, whenNotPresent);
    }

    @Test
    public void whenUndefined() {
      UNDEFINED.ifPresentOrElse(whenPresent, whenNotPresent);

      verify(whenNotPresent).run();
      verifyNoMoreInteractions(whenPresent, whenNotPresent);
    }

    @Test(expected = NullPointerException.class)
    public void whenPresentAndActionNull() {
      try {
        JSON_VALUE.ifPresentOrElse(null, whenNotPresent);
      } finally {
        verifyNoMoreInteractions(whenPresent, whenNotPresent);
      }
    }

    @Test(expected = NullPointerException.class)
    public void whenUndefinedAndActionNull() {
      try {
        UNDEFINED.ifPresentOrElse(whenPresent, null);
      } finally {
        verifyNoMoreInteractions(whenPresent, whenNotPresent);
      }
    }
  }

  /*
   * A utility method to create simple static suppliers for testing.
   */
  public static <T> Supplier<T> supplier(T value) {
    return ()->value;
  }

  public static interface BiFunctionWithThrows<T, U, R> {
    public R apply(T t, U u)
      throws Throwable;
  }

  public static abstract class BaseStreamingApiMethodTest<F> {
    @Parameter(0)
    public String name;

    @Parameter(1)
    public F f;

    @Parameter(2)
    public JsonNullable<String> value;

    public abstract Object apply()
      throws Throwable;
    
    public abstract Matcher<Object> matcher();

    @Test
    public void call() {
      Object actual = null;
      try {
        actual = apply();
      } catch (Throwable t) {
        assertThat(t, matcher());
        return;
      }
      assertThat(actual, matcher());
    }

  }

  public static abstract class NoArgumentsTest
    extends BaseStreamingApiMethodTest<Function<JsonNullable<String>, Object>> {

    @Parameter(3)
    public Matcher<Object> assertion;

    @Override
    public Object apply() throws Throwable {
      return f.apply(value);
    }

    @Override
    public Matcher<Object> matcher() {
      return assertion;
    }
  }

  public static abstract class OneArgumentTest
    extends BaseStreamingApiMethodTest<BiFunction<JsonNullable<String>, Object, Object>> {

    @Parameter(3)
    public Object argument;

    @Parameter(4)
    public Matcher<Object> assertion;

    @Override
    public Object apply() throws Throwable {
      return f.apply(value, argument);
    }

    @Override
    public Matcher<Object> matcher() {
      return assertion;
    }
  }

  public static abstract class OneArgumentWithThrowsTest
    extends BaseStreamingApiMethodTest<BiFunctionWithThrows<JsonNullable<String>, Object, Object>> {

    @Parameter(3)
    public Object argument;

    @Parameter(4)
    public Matcher<Object> assertion;
  
    @Override
    public Object apply() throws Throwable {
      return f.apply(value, argument);
    }

    @Override
    public Matcher<Object> matcher() {
      return assertion;
    }
  }
}
