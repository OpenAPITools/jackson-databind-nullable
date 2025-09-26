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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
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
import static org.junit.Assume.assumeNotNull;
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
  static Optional<Optional<String>> EQUIVALENT_VALUE     = equivalentOptional(JSON_VALUE);
  static Optional<Optional<String>> EQUIVALENT_OTHER     = equivalentOptional(JSON_OTHER);
  static Optional<Optional<String>> EQUIVALENT_NULL      = equivalentOptional(JSON_NULL);
  static Optional<Optional<String>> EQUIVALENT_UNDEFINED = equivalentOptional(UNDEFINED);

  static Optional<Optional<String>> equivalentOptional(JsonNullable<String> value) {
      if ( value.isUndefined() ) {
        return Optional.empty();
      } else {
        return Optional.of(Optional.ofNullable(value.get()));
      }
    }

  static Optional<Method> optionalMethod(String methodName, Class<?>... parameterTypes) {
    try {
      return Optional.of(Optional.class.getMethod(methodName, parameterTypes));
    } catch ( Exception e ) {
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  static <R> Optional<Function<Optional<Optional<String>>, R>> optionalFunction(String methodName) {
    return optionalMethod(methodName)
      .map(method->{
        return optional->{
          try {
            return (R)method.invoke(optional);
          } catch ( InvocationTargetException ite ) {
            if( ite.getCause() instanceof RuntimeException ) {
              throw (RuntimeException)ite.getCause();
            } else if ( ite.getCause() instanceof Error ) {
              throw (Error)ite.getCause();
            } else {
              throw new RuntimeException(ite.getCause());
            }
          }
          catch ( IllegalAccessException iae ) {
            throw new RuntimeException(iae);
          }
        };
      });
  }

  @SuppressWarnings("unchecked")
  static <A, R> Optional<BiFunction<Optional<Optional<String>>, A, R>> optionalBiFunction(String methodName, Class<? super A> argumentType) {
    return optionalMethod(methodName, argumentType)
      .map(method->{
        return (optional, argument)->{
          try {
            return (R)method.invoke(optional, argument);
          } catch ( InvocationTargetException ite ) {
            if( ite.getCause() instanceof RuntimeException ) {
              throw (RuntimeException)ite.getCause();
            } else if ( ite.getCause() instanceof Error ) {
              throw (Error)ite.getCause();
            } else {
              throw new RuntimeException(ite.getCause());
            }
          }
          catch ( IllegalAccessException iae ) {
            throw new RuntimeException(iae);
          }
        };
      });
  }

  @SuppressWarnings("unchecked")
  static <A, R> Optional<BiFunctionWithThrows<Optional<Optional<String>>, A, R>> optionalBiFunctionWithThrows(String methodName, Class<A> argumentType) {
    return optionalMethod(methodName, argumentType)
      .map(method->{
        return (optional, argument)->{
          try {
            return (R)method.invoke(optional, argument);
          } catch ( InvocationTargetException ite ) {
            throw ite.getCause();
          }
          catch ( IllegalAccessException iae ) {
            throw new RuntimeException(iae);
          }
        };
      });
  }

  interface TriConsumer<A, B, C> {
    public void accept(A a, B b, C c);
  }

  static <A, B> Optional<TriConsumer<Optional<Optional<String>>, A, B>> optionalTriConsumer(
    String methodName, 
    Class<? super A> argumentAType, 
    Class<? super B> argumentBType
  ) {
    return optionalMethod(methodName, argumentAType, argumentBType)
      .map(method->{
        return (optional, argumentA, argumentB)->{
          try {
            method.invoke(optional, argumentA, argumentB);
          } catch ( InvocationTargetException ite ) {
            if( ite.getCause() instanceof RuntimeException ) {
              throw (RuntimeException)ite.getCause();
            } else if ( ite.getCause() instanceof Error ) {
              throw (Error)ite.getCause();
            } else {
              throw new RuntimeException(ite.getCause());
            }
          }
          catch ( IllegalAccessException iae ) {
            throw new RuntimeException(iae);
          }
        };
      });
  }


  static Matcher<Object> nullPointerException() {
    return instanceOf(NullPointerException.class);
  }

  @RunWith(Parameterized.class)
  public static class OrTest extends OneArgumentTest<Supplier<JsonNullable<String>>, Supplier<Optional<Optional<String>>>> {
    static BiFunction<JsonNullable<String>, Supplier<JsonNullable<String>>, JsonNullable<String>> OR = JsonNullable::or;
    static Optional<BiFunction<Optional<Optional<String>>, Supplier<Optional<Optional<String>>>, Optional<String>>> EQUIVALENT_OR =
      optionalBiFunction("or", Supplier.class); 
    static Supplier<Throwable> NULL_SUPPLIER = null;
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "or value supplied on value"    , JSON_VALUE, supplier(JSON_OTHER), equalTo(JSON_VALUE)   , supplier(EQUIVALENT_OTHER), equalTo(EQUIVALENT_VALUE) },
            { "or value supplied on null"     , JSON_NULL , supplier(JSON_OTHER), equalTo(JSON_NULL)    , supplier(EQUIVALENT_OTHER), equalTo(EQUIVALENT_NULL)  },
            { "or value supplied on undefined", UNDEFINED , supplier(JSON_OTHER), equalTo(JSON_OTHER)   , supplier(EQUIVALENT_OTHER), equalTo(EQUIVALENT_OTHER) },
            { "or null supplied on value"     , JSON_VALUE, supplier(NULL)      , equalTo(JSON_VALUE)   , supplier(NULL)            , equalTo(EQUIVALENT_VALUE) },
            { "or null supplied on null"      , JSON_NULL , supplier(NULL)      , equalTo(JSON_NULL)    , supplier(NULL)            , equalTo(EQUIVALENT_NULL)  },
            { "or null supplied on undefined" , UNDEFINED , supplier(NULL)      , nullPointerException(), supplier(NULL)            , nullPointerException()    },
            { "or null supplier on value"     , JSON_VALUE, NULL_SUPPLIER       , nullPointerException(), NULL_SUPPLIER             , nullPointerException()    },
            { "or null supplier on null"      , JSON_NULL , NULL_SUPPLIER       , nullPointerException(), NULL_SUPPLIER             , nullPointerException()    },
            { "or null supplier on undefined" , UNDEFINED , NULL_SUPPLIER       , nullPointerException(), NULL_SUPPLIER             , nullPointerException()    },
        });
    }

    public OrTest() {
      super(OR, EQUIVALENT_OR.orElse(null));
    }
  }

  @RunWith(Parameterized.class)
  public static class OrElseTest extends OneArgumentTest<String, Optional<String>> {
    static BiFunction<JsonNullable<String>, String, String> OR_ELSE = JsonNullable::orElse;
    static Optional<BiFunction<Optional<Optional<String>>, Optional<String>, Optional<String>>> EQUIVALENT_OR_ELSE = 
      optionalBiFunction("orElse", Object.class);

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElse on value"    , JSON_VALUE, OTHER, equalTo(VALUE), Optional.of(OTHER), equalTo(Optional.of(VALUE)) },
            { "orElse on null"     , JSON_NULL , OTHER, nullValue()   , Optional.of(OTHER), equalTo(Optional.empty())   },
            { "orElse on undefined", UNDEFINED , OTHER, equalTo(OTHER), Optional.of(OTHER), equalTo(Optional.of(OTHER)) },
        });
      }
    
    public OrElseTest() {
      super(OR_ELSE, EQUIVALENT_OR_ELSE.orElse(null));
    }
  }

  @RunWith(Parameterized.class)
  public static class OrElseGetTest extends OneArgumentTest<Supplier<String>, Supplier<String>> {
    static BiFunction<JsonNullable<String>, Supplier<String>, String> OR_ELSE_GET = JsonNullable::orElseGet;
    static Optional<BiFunction<Optional<Optional<String>>, Supplier<String>, Optional<String>>> EQUIVALENT_OR_ELSE_GET = 
      optionalBiFunction("orElseGet", Supplier.class);
    static Supplier<String> NULL_SUPPLIER = null;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElseGet value supplied on value"    , JSON_VALUE, supplier(OTHER), equalTo(VALUE)        , supplier(Optional.of(OTHER)), equalTo(Optional.of(VALUE)) },
            { "orElseGet value supplied on null"     , JSON_NULL , supplier(OTHER), nullValue()           , supplier(Optional.of(OTHER)), equalTo(Optional.empty())   },
            { "orElseGet value supplied on undefined", UNDEFINED , supplier(OTHER), equalTo(OTHER)        , supplier(Optional.of(OTHER)), equalTo(Optional.of(OTHER)) },
            { "orElseGet null supplier on value"     , JSON_VALUE, NULL_SUPPLIER  , equalTo(VALUE)        , NULL_SUPPLIER               , equalTo(Optional.of(VALUE)) },
            { "orElseGet null supplier on null"      , JSON_NULL , NULL_SUPPLIER  , nullValue()           , NULL_SUPPLIER               , equalTo(Optional.empty())   },
            { "orElseGet null supplier on undefined" , UNDEFINED , NULL_SUPPLIER  , nullPointerException(), NULL_SUPPLIER               , nullPointerException()      },
        });
      }
        
    public OrElseGetTest() {
      super(OR_ELSE_GET, EQUIVALENT_OR_ELSE_GET.orElse(null));
    }
  }

  @RunWith(Parameterized.class)
  public static class OrElseThrowTest extends NoArgumentsTest {
    static Function<JsonNullable<String>, String> OR_ELSE_THROW = JsonNullable::orElseThrow;
    static Optional<Function<Optional<Optional<String>>, Optional<String>>> EQUIVALENT_OR_ELSE_THROW = 
      optionalFunction("orElseThrow");

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElseThrow on value"    , JSON_VALUE, equalTo(VALUE)                          , equalTo(Optional.of(VALUE))              },
            { "orElseThrow on null"     , JSON_NULL , nullValue()                             , equalTo(Optional.empty())                },
            { "orElseThrow on undefined", UNDEFINED , instanceOf(NoSuchElementException.class), instanceOf(NoSuchElementException.class) },
        });
    }
  
    public OrElseThrowTest() {
      super(OR_ELSE_THROW, EQUIVALENT_OR_ELSE_THROW.orElse(null));
    }
  }

  @SuppressWarnings("rawtypes")
  @RunWith(Parameterized.class)
  public static class OrElseThrowWithSupplierTest extends OneArgumentWithThrowsTest<Supplier<Throwable>, Supplier> {
    public static class TestException extends Exception {}
    static BiFunctionWithThrows<JsonNullable<String>, Supplier<Throwable>, String> OR_ELSE_THROW = JsonNullable::orElseThrow;
    static Optional<BiFunctionWithThrows<Optional<Optional<String>>, Supplier, Optional<String>>> EQUIVALENT_OR_ELSE_THROW = 
      optionalBiFunctionWithThrows("orElseThrow", Supplier.class);
    static Supplier<Throwable> NULL_SUPPLIER = null;
    static Supplier<Throwable> NULL_SUPPLIED = ()->null;
    static Supplier<Throwable> TEST_EXCEPTION = TestException::new;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "orElseThrow with supplier on value"    , JSON_VALUE, TEST_EXCEPTION, equalTo(VALUE)                 , TEST_EXCEPTION, equalTo(Optional.of(VALUE))     },
            { "orElseThrow with supplier on null"     , JSON_NULL , TEST_EXCEPTION, nullValue()                    , TEST_EXCEPTION, equalTo(Optional.empty())       },
            { "orElseThrow with supplier on undefined", UNDEFINED , TEST_EXCEPTION, instanceOf(TestException.class), TEST_EXCEPTION, instanceOf(TestException.class) },
            { "orElseThrow null supplier on value"    , JSON_VALUE, NULL_SUPPLIER , equalTo(VALUE)                 , NULL_SUPPLIER , equalTo(Optional.of(VALUE))     },
            { "orElseThrow null supplier on null"     , JSON_NULL , NULL_SUPPLIER , nullValue()                    , NULL_SUPPLIER , equalTo(Optional.empty())       },
            { "orElseThrow null supplier on undefined", UNDEFINED , NULL_SUPPLIER , nullPointerException()         , NULL_SUPPLIER , nullPointerException()          },
            { "orElseThrow null supplied on value"    , JSON_VALUE, NULL_SUPPLIED , equalTo(VALUE)                 , NULL_SUPPLIED , equalTo(Optional.of(VALUE))     },
            { "orElseThrow null supplied on null"     , JSON_NULL , NULL_SUPPLIED , nullValue()                    , NULL_SUPPLIED , equalTo(Optional.empty())       },
            { "orElseThrow null supplied on undefined", UNDEFINED , NULL_SUPPLIED , nullPointerException()         , NULL_SUPPLIED , nullPointerException()          },
        });
      }

    public OrElseThrowWithSupplierTest() {
      super(OR_ELSE_THROW, EQUIVALENT_OR_ELSE_THROW.orElse(null));
    }
  }

  @RunWith(Parameterized.class)
  public static class MapTest extends OneArgumentTest<Function<String, Object>, Function<Optional<String>, Object>> {
    static BiFunction<JsonNullable<String>, Function<String, Object>, JsonNullable<Object>> MAP = JsonNullable::map;
    static Optional<BiFunction<Optional<Optional<String>>, Function<Optional<String>, Object>, Optional<Object>>> EQUIVALENT_MAP =
      optionalBiFunction("map", Function.class);

    static Function<String, String> MAPPING = value->value!=null?value+" mapped":"null mapped";
    static Function<String, String> NULL_MAPPING = null;
    static Function<Optional<String>, Optional<String>> EQUIVALENT_MAPPING = value->Optional.of(value.map(v->v+" mapped").orElse("null mapped"));
    static Function<Optional<String>, Optional<String>> EQUIVALENT_NULL_MAPPING = null;    

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "map on value"                 , JSON_VALUE, MAPPING     , equalTo(of("value mapped")) , EQUIVALENT_MAPPING     , equalTo(Optional.of(Optional.of("value mapped"))) },
            { "map on null"                  , JSON_NULL , MAPPING     , equalTo(of("null mapped"))  , EQUIVALENT_MAPPING     , equalTo(Optional.of(Optional.of("null mapped")))  },
            { "map on undefined"             , UNDEFINED , MAPPING     , equalTo(UNDEFINED)                , EQUIVALENT_MAPPING     , equalTo(EQUIVALENT_UNDEFINED)                           },
            { "map null mapping on value"    , JSON_VALUE, NULL_MAPPING, nullPointerException()            , EQUIVALENT_NULL_MAPPING, nullPointerException()                                  },
            { "map null mapping on null"     , JSON_NULL , NULL_MAPPING, nullPointerException()            , EQUIVALENT_NULL_MAPPING, nullPointerException()                                  },
            { "map null mapping on undefined", UNDEFINED , NULL_MAPPING, nullPointerException()            , EQUIVALENT_NULL_MAPPING, nullPointerException()                                  },
        });
      }

      public MapTest() {
        super(MAP, EQUIVALENT_MAP.orElse(null));
      }
  }

  @RunWith(Parameterized.class)
  public static class FilterTest extends OneArgumentTest<Predicate<String>, Predicate<Optional<String>>> {
    static BiFunction<JsonNullable<String>, Predicate<String>, JsonNullable<String>> FILTER = JsonNullable::filter;
    static Optional<BiFunction<Optional<Optional<String>>, Predicate<Optional<String>>, Optional<String>>> EQUIVALENT_FILTER = 
      optionalBiFunction("filter", Predicate.class);

    static Predicate<?> KEEP_ALL       = value->true;
    static Predicate<?> KEEP_NULL      = value->value==null;
    static Predicate<?> KEEP_VALUE     = value->value!=null;
    static Predicate<?> KEEP_NONE      = value->false;
    static Predicate<?> NULL_PREDICATE = null;
    static Predicate<Optional<?>> EQUIVALENT_KEEP_ALL       = value->true;
    static Predicate<Optional<?>> EQUIVALENT_KEEP_NULL      = value->!value.isPresent();
    static Predicate<Optional<?>> EQUIVALENT_KEEP_VALUE     = value->value.isPresent();
    static Predicate<Optional<?>> EQUIVALENT_KEEP_NONE      = value->false;
    static Predicate<Optional<?>> EQUIVALENT_NULL_PREDICATE = null;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "filter keep all on value"          , JSON_VALUE, KEEP_ALL      , equalTo(JSON_VALUE)   , EQUIVALENT_KEEP_ALL      , equalTo(EQUIVALENT_VALUE)     },
            { "filter keep all on null"           , JSON_NULL , KEEP_ALL      , equalTo(JSON_NULL)    , EQUIVALENT_KEEP_ALL      , equalTo(EQUIVALENT_NULL)      },
            { "filter keep all on undefined"      , UNDEFINED , KEEP_ALL      , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_ALL      , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter keep value on value"        , JSON_VALUE, KEEP_VALUE    , equalTo(JSON_VALUE)   , EQUIVALENT_KEEP_VALUE    , equalTo(EQUIVALENT_VALUE)     },
            { "filter keep value on null"         , JSON_NULL , KEEP_VALUE    , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_VALUE    , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter keep value on undefined"    , UNDEFINED , KEEP_VALUE    , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_VALUE    , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter keep null on value"         , JSON_VALUE, KEEP_NULL     , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_NULL     , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter keep null on null"          , JSON_NULL , KEEP_NULL     , equalTo(JSON_NULL)    , EQUIVALENT_KEEP_NULL     , equalTo(EQUIVALENT_NULL)      },
            { "filter keep null on undefined"     , UNDEFINED , KEEP_NULL     , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_NULL     , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter remove on value"            , JSON_VALUE, KEEP_NONE     , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_NONE     , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter remove on null"             , JSON_NULL , KEEP_NONE     , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_NONE     , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter remove on undefined"        , UNDEFINED , KEEP_NONE     , equalTo(UNDEFINED)    , EQUIVALENT_KEEP_NONE     , equalTo(EQUIVALENT_UNDEFINED) },
            { "filter null predicate on value"    , JSON_VALUE, NULL_PREDICATE, nullPointerException(), EQUIVALENT_NULL_PREDICATE, nullPointerException()        },
            { "filter null predicate on null"     , JSON_NULL , NULL_PREDICATE, nullPointerException(), EQUIVALENT_NULL_PREDICATE, nullPointerException()        },
            { "filter null predicate on undefined", UNDEFINED , NULL_PREDICATE, nullPointerException(), EQUIVALENT_NULL_PREDICATE, nullPointerException()        },
        });
      }

      public FilterTest() {
        super(FILTER, EQUIVALENT_FILTER.orElse(null));
      }
  }

  @RunWith(Parameterized.class)
  public static class FlatMapTest extends OneArgumentTest<Function<String, JsonNullable<Object>>, Function<Optional<String>, Optional<Optional<Object>>>>  {
    static BiFunction<JsonNullable<String>, Function<String, JsonNullable<Object>>, JsonNullable<Object>> FLAT_MAP = JsonNullable::flatMap;
    static Optional<BiFunction<Optional<Optional<String>>, Function<Optional<String>, Optional<Optional<Object>>>, Optional<Optional<Object>>>> EQUIVALENT_FLAT_MAP = 
      optionalBiFunction("flatMap", Function.class);

    static Function<String, JsonNullable<String>> ANY_TO_UNDEFINED = value->UNDEFINED;
    static Function<String, JsonNullable<String>> NULL_TO_UNDEFINED = value->value!=null?of(value):UNDEFINED;
    static Function<String, JsonNullable<String>> ANY_TO_OTHER = value->JSON_OTHER;
    static Function<String, JsonNullable<String>> NULL_MAPPING = null;
    static Function<String, JsonNullable<String>> NULL_RESULT = value->null;
    static Function<Optional<String>, Optional<Optional<String>>> EQUIVALENT_ANY_TO_UNDEFINED = value->EQUIVALENT_UNDEFINED;
    static Function<Optional<String>, Optional<Optional<String>>> EQUIVALENT_NULL_TO_UNDEFINED = value->value.isPresent()?Optional.of(value):EQUIVALENT_UNDEFINED;
    static Function<Optional<String>, Optional<Optional<String>>> EQUIVALENT_ANY_TO_OTHER = value->EQUIVALENT_OTHER;
    static Function<Optional<String>, Optional<Optional<String>>> EQUIVALENT_NULL_MAPPING = null;
    static Function<Optional<String>, Optional<Optional<String>>> EQUIVALENT_NULL_RESULT = value->null;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "flatMap any to undefined on value"     , JSON_VALUE, ANY_TO_UNDEFINED , equalTo(UNDEFINED)    , EQUIVALENT_ANY_TO_UNDEFINED , equalTo(EQUIVALENT_UNDEFINED) },
            { "flatMap any to undefined on null"      , JSON_NULL , ANY_TO_UNDEFINED , equalTo(UNDEFINED)    , EQUIVALENT_ANY_TO_UNDEFINED , equalTo(EQUIVALENT_UNDEFINED) },
            { "flatMap any to undefined on undefined" , UNDEFINED , ANY_TO_UNDEFINED , equalTo(UNDEFINED)    , EQUIVALENT_ANY_TO_UNDEFINED , equalTo(EQUIVALENT_UNDEFINED) },
            { "flatMap null to undefined on value"    , JSON_VALUE, NULL_TO_UNDEFINED, equalTo(JSON_VALUE)   , EQUIVALENT_NULL_TO_UNDEFINED, equalTo(EQUIVALENT_VALUE)     },
            { "flatMap null to undefined on null"     , JSON_NULL , NULL_TO_UNDEFINED, equalTo(UNDEFINED)    , EQUIVALENT_NULL_TO_UNDEFINED, equalTo(Optional.empty())     },
            { "flatMap null to undefined on undefined", UNDEFINED , NULL_TO_UNDEFINED, equalTo(UNDEFINED)    , EQUIVALENT_NULL_TO_UNDEFINED, equalTo(EQUIVALENT_UNDEFINED) },
            { "flatMap any to other on value"         , JSON_VALUE, ANY_TO_OTHER     , equalTo(JSON_OTHER)   , EQUIVALENT_ANY_TO_OTHER     , equalTo(EQUIVALENT_OTHER)     },
            { "flatMap any to other on null"          , JSON_NULL , ANY_TO_OTHER     , equalTo(JSON_OTHER)   , EQUIVALENT_ANY_TO_OTHER     , equalTo(EQUIVALENT_OTHER)     },
            { "flatMap any to other on undefined"     , UNDEFINED , ANY_TO_OTHER     , equalTo(UNDEFINED)    , EQUIVALENT_ANY_TO_OTHER     , equalTo(EQUIVALENT_UNDEFINED) },
            { "flatMap null mapping on value"         , JSON_VALUE, NULL_MAPPING     , nullPointerException(), EQUIVALENT_NULL_MAPPING     , nullPointerException()        },
            { "flatMap null mapping on null"          , JSON_NULL , NULL_MAPPING     , nullPointerException(), EQUIVALENT_NULL_MAPPING     , nullPointerException()        },
            { "flatMap null mapping on undefined"     , UNDEFINED , NULL_MAPPING     , nullPointerException(), EQUIVALENT_NULL_MAPPING     , nullPointerException()        },
            { "flatMap null result on value"          , JSON_VALUE, NULL_RESULT      , nullPointerException(), EQUIVALENT_NULL_RESULT      , nullPointerException()        },
            { "flatMap null result on null"           , JSON_NULL , NULL_RESULT      , nullPointerException(), EQUIVALENT_NULL_RESULT      , nullPointerException()        },
            { "flatMap null result on undefined"      , UNDEFINED , NULL_RESULT      , equalTo(UNDEFINED)    , EQUIVALENT_NULL_RESULT      , equalTo(EQUIVALENT_UNDEFINED) },
        });
      }

      public FlatMapTest() {
        super(FLAT_MAP, EQUIVALENT_FLAT_MAP.orElse(null));
      }
  }

  @RunWith(Parameterized.class)
  public static class IsUndefinedTest extends NoArgumentsTest {
    static Function<JsonNullable<String>, Boolean> IS_UNDEFINED = JsonNullable::isUndefined;
    static Optional<Function<Optional<Optional<String>>, Boolean>> EQUIVALENT_IS_UNDEFINED =
      optionalFunction("isEmpty");

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "isUndefined on value"    , JSON_VALUE, equalTo(false) , equalTo(false) },
            { "isUndefined on null"     , JSON_NULL , equalTo(false) , equalTo(false) },
            { "isUndefined on undefined", UNDEFINED , equalTo(true)  , equalTo(true)  },
        });
      }

    public IsUndefinedTest() {
      super(IS_UNDEFINED, EQUIVALENT_IS_UNDEFINED.orElse(null));
    }
  }

  @RunWith(Parameterized.class)
  public static class StreamTest extends NoArgumentsTest {
    static Function<JsonNullable<String>, Stream<String>> STREAM = JsonNullable::stream;
    static Optional<Function<Optional<Optional<String>>, Stream<Optional<String>>>> EQUIVALENT_STREAM = 
      optionalFunction("stream");
    static Function<JsonNullable<String>, List<String>> STREAM_TO_LIST = STREAM.andThen(s->s.collect(Collectors.toList()));
    static Optional<Function<Optional<Optional<String>>, List<Optional<String>>>> EQUIVALENT_STREAM_TO_LIST = 
      EQUIVALENT_STREAM.map(stream->stream.andThen(s->s.collect(Collectors.toList())));
    @SuppressWarnings("unchecked")
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "stream on value"    , JSON_VALUE, contains(VALUE), contains(Optional.of(VALUE)) },
            { "stream on null"     , JSON_NULL , contains(NULL) , contains(Optional.empty())   },
            { "stream on undefined", UNDEFINED , empty()        , empty()                      }
        });
    }

    public StreamTest() {
      super(STREAM_TO_LIST, EQUIVALENT_STREAM_TO_LIST.orElse(null));
    }
  }

  @RunWith(Parameterized.class)
  public static class IfPresentOrElseTest {
    static BiConsumer<Consumer<String>, Runnable> v(BiConsumer<Consumer<String>, Runnable> verify) {
      return verify;
    }
    static Function<String, BiConsumer<Consumer<String>, Runnable>> ACTION_CALLED = (value)->(c, r)->{verify(c).accept(value);};
    static Function<Optional<String>, BiConsumer<Consumer<Optional<String>>, Runnable>> EQUIVALENT_ACTION_CALLED = (value)->(c, r)->{verify(c).accept(value);};
    static BiConsumer<Consumer<String>, Runnable> UNDEFINED_ACTION_CALLED = (c, r)->{verify(r).run();};
    static BiConsumer<Consumer<String>, Runnable> NOTHING_CALLED = (c, r)->{};
    static BiConsumer<Consumer<Optional<String>>, Runnable> EQUIVALENT_NOTHING_CALLED = (c, r)->{};
    static Boolean ACTION_PROVIDED = true;
    static Boolean ACTION_NULL = false;
    static Boolean UNDEFINED_ACTION_PROVIDED = true;
    static Boolean UNDEFINED_ACTION_NULL = false;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "ifPresentOrElse on value with action and undefined action"              , JSON_VALUE, ACTION_PROVIDED, UNDEFINED_ACTION_PROVIDED, ACTION_CALLED.apply(VALUE), nullValue()           , EQUIVALENT_ACTION_CALLED.apply(Optional.of(VALUE)) },
            { "ifPresentOrElse on null with action and undefined action"               , JSON_NULL , ACTION_PROVIDED, UNDEFINED_ACTION_PROVIDED, ACTION_CALLED.apply(NULL) , nullValue()           , EQUIVALENT_ACTION_CALLED.apply(Optional.empty())   },
            { "ifPresentOrElse on undefined with action and undefined action"          , UNDEFINED , ACTION_PROVIDED, UNDEFINED_ACTION_PROVIDED, UNDEFINED_ACTION_CALLED   , nullValue()           , UNDEFINED_ACTION_CALLED },
            { "ifPresentOrElse on value with null action and undefined action"         , JSON_VALUE, ACTION_NULL    , UNDEFINED_ACTION_PROVIDED, NOTHING_CALLED            , nullPointerException(), EQUIVALENT_NOTHING_CALLED },
            { "ifPresentOrElse on null with null action and undefined action"          , JSON_NULL , ACTION_NULL    , UNDEFINED_ACTION_PROVIDED, NOTHING_CALLED            , nullPointerException(), EQUIVALENT_NOTHING_CALLED },
            { "ifPresentOrElse on undefined with null action and undefined action"     , UNDEFINED , ACTION_NULL    , UNDEFINED_ACTION_PROVIDED, UNDEFINED_ACTION_CALLED   , nullValue()           , UNDEFINED_ACTION_CALLED },
            { "ifPresentOrElse on value with action and null undefined action"         , JSON_VALUE, ACTION_PROVIDED, UNDEFINED_ACTION_NULL    , ACTION_CALLED.apply(VALUE), nullValue()           , EQUIVALENT_ACTION_CALLED.apply(Optional.of(VALUE)) },
            { "ifPresentOrElse on null with action and null undefined action"          , JSON_NULL , ACTION_PROVIDED, UNDEFINED_ACTION_NULL    , ACTION_CALLED.apply(NULL) , nullValue()           , EQUIVALENT_ACTION_CALLED.apply(Optional.empty())   },
            { "ifPresentOrElse on undefined with action and null undefined action"     , UNDEFINED , ACTION_PROVIDED, UNDEFINED_ACTION_NULL    , NOTHING_CALLED            , nullPointerException(), EQUIVALENT_NOTHING_CALLED },
            { "ifPresentOrElse on value with null action and null undefined action"    , JSON_VALUE, ACTION_NULL    , UNDEFINED_ACTION_NULL    , NOTHING_CALLED            , nullPointerException(), EQUIVALENT_NOTHING_CALLED },
            { "ifPresentOrElse on null with null action and null undefined action"     , JSON_NULL , ACTION_NULL    , UNDEFINED_ACTION_NULL    , NOTHING_CALLED            , nullPointerException(), EQUIVALENT_NOTHING_CALLED },
            { "ifPresentOrElse on undefined null with action and null undefined action", UNDEFINED , ACTION_NULL    , UNDEFINED_ACTION_NULL    , NOTHING_CALLED            , nullPointerException(), EQUIVALENT_NOTHING_CALLED },
        });
    }
    @SuppressWarnings("unchecked")
    Consumer<String> whenPresent = mock(Consumer.class);
    Runnable whenNotPresent = mock(Runnable.class);

    @SuppressWarnings("rawtypes")
    TriConsumer<Optional<Optional<String>>, Consumer, Runnable> equivalentCall =
      optionalTriConsumer("ifPresentOrElse", Consumer.class, Runnable.class).orElse(null);
    @SuppressWarnings("unchecked")
    Consumer<Optional<String>> equivalentWhenPresent = mock(Consumer.class);

    @Parameter(0)
    public String name;

    @Parameter(1)
    public JsonNullable<String> value;

    @Parameter(2)
    public Boolean actionProvided;

    @Parameter(3)
    public Boolean undefinedActionProvided;

    @Parameter(4)
    public BiConsumer<Consumer<String>, Runnable> verify;

    @Parameter(5)
    public Matcher<Object> exceptionMatcher;

    @Parameter(6)
    public BiConsumer<Consumer<Optional<String>>, Runnable> equivalentVerify;

    @Before
    public void resetMocks() {
      Mockito.reset(whenPresent, whenNotPresent, equivalentWhenPresent);
    }

    @Test
    public void call() {
      Throwable thrown = null;
      Consumer<String> action = actionProvided?whenPresent:null;
      Runnable undefinedAction = undefinedActionProvided?whenNotPresent:null;
      try {
        value.ifPresentOrElse(action, undefinedAction);
      } catch( Throwable t) {
        thrown = t;
      }
      finally {
        verify.accept(whenPresent, whenNotPresent);
        exceptionMatcher.matches(thrown);
        verifyNoMoreInteractions(whenPresent, whenNotPresent);
      }
    }

    @Test
    public void equivalentCall() {
      assumeNotNull(equivalentCall);
      Optional<Optional<String>> equivalentValue = equivalentOptional(value);

      Throwable thrown = null;
      Consumer<Optional<String>> action = actionProvided?equivalentWhenPresent:null;
      Runnable undefinedAction = undefinedActionProvided?whenNotPresent:null;
      try {
        equivalentCall.accept(equivalentValue, action, undefinedAction);
      } catch( Throwable t) {
        thrown = t;
      }
      finally {
        equivalentVerify.accept(equivalentWhenPresent, whenNotPresent);
        exceptionMatcher.matches(thrown);
        verifyNoMoreInteractions(equivalentWhenPresent, whenNotPresent);
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

  public static abstract class BaseStreamingApiMethodTest<F, E> {
    public E equivalentF;
    public F f;

    @Parameter(0)
    public String name;

    @Parameter(1)
    public JsonNullable<String> value;

    public abstract Object apply()
      throws Throwable;
    
    public abstract Matcher<Object> matcher();

    public abstract Object equivalentApply()
      throws Throwable;

    public abstract Matcher<Object> equivalentMatcher();

    public BaseStreamingApiMethodTest(F f, E equivalentF) {
      this.f = f;
      this.equivalentF = equivalentF;
    }

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

    @Test
    public void equivalentCall() {
      assumeNotNull(equivalentF);
      Object equivalentActual = null;
      try {
        equivalentActual = equivalentApply();
      } catch (Throwable t) {
        assertThat(t, equivalentMatcher());
        return;
      }

      assertThat(equivalentActual, equivalentMatcher());
    }
  }

  public static abstract class NoArgumentsTest
    extends BaseStreamingApiMethodTest<Function<JsonNullable<String>, ? extends Object>, Function<Optional<Optional<String>>, ? extends Object>> {

    @Parameter(2)
    public Matcher<Object> assertion;

    @Parameter(3)
    public Matcher<Object> equivalentAssertion;

    public NoArgumentsTest(Function<JsonNullable<String>, ? extends Object> f, Function<Optional<Optional<String>>, ? extends Object> equivalentF) {
      super(f, equivalentF);
    }

    @Override
    public Object apply() throws Throwable {
      return f.apply(value);
    }

    @Override
    public Matcher<Object> matcher() {
      return assertion;
    }

    @Override
    public Object equivalentApply() throws Throwable  {
      if( equivalentF == null ) throw new IllegalStateException("expected equivalent function");
      Optional<Optional<String>> equivalentValue = equivalentOptional(value);
      return equivalentF.apply(equivalentValue);
    }

    @Override
    public Matcher<Object> equivalentMatcher() {
      return equivalentAssertion;
    }
  }

  public static abstract class OneArgumentTest<A, E>
    extends BaseStreamingApiMethodTest<BiFunction<JsonNullable<String>, A, ? extends Object>, BiFunction<Optional<Optional<String>>, E, ? extends Object>> {

    @Parameter(2)
    public A argument;

    @Parameter(3)
    public Matcher<Object> assertion;

    @Parameter(4)
    public E equivalentArgument;

    @Parameter(5)
    public Matcher<Object> equivalentAssertion;

    public OneArgumentTest(BiFunction<JsonNullable<String>, A, ? extends Object> f, BiFunction<Optional<Optional<String>>, E, ? extends Object> equivalentF) {
      super(f, equivalentF);
    }

    @Override
    public Object apply() throws Throwable {
      return f.apply(value, argument);
    }

    @Override
    public Matcher<Object> matcher() {
      return assertion;
    }

    @Override
    public Object equivalentApply() throws Throwable  {
      if( equivalentF == null ) throw new IllegalStateException("expected equivalent function");
      Optional<Optional<String>> equivalentValue = equivalentOptional(value);
      return equivalentF.apply(equivalentValue, equivalentArgument);
    }

    @Override
    public Matcher<Object> equivalentMatcher() {
      return equivalentAssertion;
    }
  }

  public static abstract class OneArgumentWithThrowsTest<A, E>
    extends BaseStreamingApiMethodTest<BiFunctionWithThrows<JsonNullable<String>, A, ? extends Object>, BiFunctionWithThrows<Optional<Optional<String>>, E, ? extends Object>> {

    @Parameter(2)
    public A argument;

    @Parameter(3)
    public Matcher<Object> assertion;

    @Parameter(4)
    public E equivalentArgument;

    @Parameter(5)
    public Matcher<Object> equivalentAssertion;

    public OneArgumentWithThrowsTest(BiFunctionWithThrows<JsonNullable<String>, A, ? extends Object> f, BiFunctionWithThrows<Optional<Optional<String>>, E, ? extends Object> equivalentF) {
      super(f, equivalentF);
    }
  
    @Override
    public Object apply() throws Throwable {
      return f.apply(value, argument);
    }

    @Override
    public Matcher<Object> matcher() {
      return assertion;
    }

    @Override
    public Object equivalentApply() throws Throwable  {
      if( equivalentF == null ) throw new IllegalStateException("expected equivalent function");
      Optional<Optional<String>> equivalentValue = equivalentOptional(value);
      return equivalentF.apply(equivalentValue, equivalentArgument);
    }

    @Override
    public Matcher<Object> equivalentMatcher() {
      return equivalentAssertion;
    }
  }
}
