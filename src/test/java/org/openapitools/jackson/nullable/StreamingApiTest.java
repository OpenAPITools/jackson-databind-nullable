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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.AdditionalAnswers;

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
//@DisplayName("Streaming API Tests")
public class StreamingApiTest {

    static ReflectiveService<Optional<Optional<String>>> OPTIONAL = new ReflectiveService<Optional<Optional<String>>>(Optional.class);

    static String VALUE = "value";
    static String OTHER = "other";
    static String NULL = null;
    static JsonNullable<String> JSON_VALUE = of(VALUE);
    static JsonNullable<String> JSON_OTHER = of(OTHER);
    static JsonNullable<String> JSON_NULL  = of(null);
    static JsonNullable<String> UNDEFINED  = undefined();
    static Class<NullPointerException> NPE = NullPointerException.class;
    static Class<TestException> TE = TestException.class;

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public abstract class BaseTest {

        public abstract Stream<Object[]> baseArgs();
        public abstract Callable<?> createCall(Object[] row);
        public abstract Callable<?> createEquivalentCall(Object[] row);

        @ParameterizedTest
        @MethodSource("args")
        public void standardCall(String name, Callable<JsonNullable<String>> callable, Matcher<Object> matcher) {
            Assumptions.assumeTrue(callable!=null);
            Object actual = null;
            try {
                actual = callable.call();
            } catch (Throwable t) {
                assertThat(t, matcher);
                return;
            }
            assertThat(actual, matcher);
        }

        public Stream<Arguments> args() {
            return baseArgs()
                .map(args->new Object[] { args[0], createCall(args), createMatcher(args[args.length - 1])})
                .map(args->Arguments.argumentSet((String)args[0], args));
        }

        @ParameterizedTest
        @MethodSource("equivalentArgs")
        public void equivalentCall(String name, Callable<Optional<Optional<String>>> callable, Matcher<Object> matcher) {
            Assumptions.assumeTrue(callable!=null);
            Object actual = null;
            try {
                actual = callable.call();
            } catch (Throwable t) {
                assertThat(t, matcher);
                return;
            }
            assertThat(actual, matcher);
        }

        public Stream<Arguments> equivalentArgs() {
            return baseArgs()
                .map(args->new Object[] {
                    args[0],
                    createEquivalentCall(args),
                    createMatcher(args[args.length - 1])
                })
                .map(args->Arguments.argumentSet("equivalent "+(String)args[0], args));
        }
    }

    @Nested
    @DisplayName("Streaming API Tests or(Supplier)")
    public class OrTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            Supplier<Throwable> NULL_SUPPLIER = null;
            return Arrays.asList(new Object[][] {
                { "or value supplied on value"    , JSON_VALUE, supplier(JSON_OTHER), JSON_VALUE },
                { "or value supplied on null"     , JSON_NULL , supplier(JSON_OTHER), JSON_NULL  },
                { "or value supplied on undefined", UNDEFINED , supplier(JSON_OTHER), JSON_OTHER },
                { "or null supplied on value"     , JSON_VALUE, supplier(NULL)      , JSON_VALUE },
                { "or null supplied on null"      , JSON_NULL , supplier(NULL)      , JSON_NULL  },
                { "or null supplied on undefined" , UNDEFINED , supplier(NULL)      , NPE        },
                { "or null supplier on value"     , JSON_VALUE, NULL_SUPPLIER       , NPE        },
                { "or null supplier on null"      , JSON_NULL , NULL_SUPPLIER       , NPE        },
                { "or null supplier on undefined" , UNDEFINED , NULL_SUPPLIER       , NPE        },
            }).stream();
        }

        @Override
        public Callable<?> createCall(Object[] row) {
            return biFunctionCall(
                JsonNullable<String>::or,
                asJson(row[1]),
                asSupplier(row[2])
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return biFunctionCall(
                OPTIONAL.forName("or")
                  .<Supplier<Optional<Optional<String>>>>argument(Supplier.class)
                  .<Optional<Optional<String>>>getBiFunction()
                    .map(f->f.andThen(StreamingApiTest::optionalToNullable))
                    .orElse(null),
                nullableToOptional(row[1]),
                nullableSupplierToOptionalSupplier(asSupplier(row[2]))
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests orElse(Object)")
    public class OrElseTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            return Arrays.asList(new Object[][] {
                { "orElse value on value"    , JSON_VALUE, OTHER, VALUE },
                { "orElse value on null"     , JSON_NULL , OTHER, NULL  },
                { "orElse value on undefined", UNDEFINED , OTHER, OTHER },
                { "orElse null on value"     , JSON_VALUE, NULL , VALUE },
                { "orElse null on null"      , JSON_NULL , NULL , NULL  },
                { "orElse null on undefined" , UNDEFINED , NULL , NULL  },
            }).stream();
        }

        @Override
        public Callable<?> createCall(Object[] row) {
            return biFunctionCall(
                JsonNullable<String>::orElse,
                asJson(row[1]),
                (String)row[2]
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return biFunctionCall(
                OPTIONAL.forName("orElse")
                  .<Optional<String>>argument(Object.class)
                  .<Optional<String>>getBiFunction()
                    .map(f->f.andThen(StreamingApiTest::optionalValueToNullableValue))
                    .orElse(null),
                nullableToOptional(row[1]),
                nullableValueToOptionalValue(row[2])
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests orElseGet(Supplier)")
    public class OrElseGetTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            Supplier<String> NULL_SUPPLIER = null;
            return Arrays.asList(new Object[][] {
                { "orElseGet value supplied on value"    , JSON_VALUE, supplier(OTHER), VALUE },
                { "orElseGet value supplied on null"     , JSON_NULL , supplier(OTHER), NULL  },
                { "orElseGet value supplied on undefined", UNDEFINED , supplier(OTHER), OTHER },
                { "orElseGet null supplier on value"     , JSON_VALUE, NULL_SUPPLIER  , VALUE },
                { "orElseGet null supplier on null"      , JSON_NULL , NULL_SUPPLIER  , NULL  },
                { "orElseGet null supplier on undefined" , UNDEFINED , NULL_SUPPLIER  , NPE   },
            }).stream();
        }

        @Override
        public Callable<?> createCall(Object[] row) {
            return biFunctionCall(
                JsonNullable<String>::orElseGet,
                asJson(row[1]),
                asSupplier(row[2])
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return biFunctionCall(
                OPTIONAL.forName("orElseGet")
                  .<Supplier<Optional<String>>>argument(Supplier.class)
                  .<Optional<String>>getBiFunction()
                    .map(f->f.andThen(StreamingApiTest::optionalValueToNullableValue))
                    .orElse(null),
                nullableToOptional(row[1]),
                nullableValueSupplierToOptionalValueSupplier(asSupplier(row[2]))
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests orElseThrow()")
    public class OrElseThrowTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            Class<NoSuchElementException> NSEE = NoSuchElementException.class;
            return Arrays.asList(new Object[][] {
                { "orElseThrow on value"    , JSON_VALUE, VALUE },
                { "orElseThrow on null"     , JSON_NULL , NULL  },
                { "orElseThrow on undefined", UNDEFINED , NSEE, },
            }).stream();
        }

        @Override
        public Callable<?> createCall(Object[] row) {
            return functionCall(
                JsonNullable<String>::orElseThrow,
                asJson(row[1])
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return functionCall(
                OPTIONAL.forName("orElseThrow")
                    .<Optional<String>>getFunction()
                    .map(f->f.andThen(StreamingApiTest::optionalValueToNullableValue))
                    .orElse(null),
                nullableToOptional(row[1])
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests orElseThrow(Supplier)")
    public class OrElseThrowWithSupplierTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            TestSupplier<Throwable> NULL_SUPPLIER = null;
            TestSupplier<Throwable> NULL_SUPPLIED = ()->null;
            TestSupplier<Throwable> TEST_EXCEPTION = TestException::new;
            return Arrays.asList(new Object[][] {
                { "orElseThrow with supplier on value"    , JSON_VALUE, TEST_EXCEPTION, VALUE },
                { "orElseThrow with supplier on null"     , JSON_NULL , TEST_EXCEPTION, NULL  },
                { "orElseThrow with supplier on undefined", UNDEFINED , TEST_EXCEPTION, TE    },
                { "orElseThrow null supplier on value"    , JSON_VALUE, NULL_SUPPLIER , VALUE },
                { "orElseThrow null supplier on null"     , JSON_NULL , NULL_SUPPLIER , NULL  },
                { "orElseThrow null supplier on undefined", UNDEFINED , NULL_SUPPLIER , NPE   },
                { "orElseThrow null supplied on value"    , JSON_VALUE, NULL_SUPPLIED , VALUE },
                { "orElseThrow null supplied on null"     , JSON_NULL , NULL_SUPPLIED , NULL  },
                { "orElseThrow null supplied on undefined", UNDEFINED , NULL_SUPPLIED , NPE   },
            }).stream();
        }

        @Override
        public Callable<?> createCall(Object[] row) {
            return biFunctionWithThrowsCall(
                JsonNullable<String>::orElseThrow,
                asJson(row[1]),
                asExceptionSupplier(row[2])
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return biFunctionWithThrowsCall(
                OPTIONAL.forName("orElseThrow")
                  .<Supplier<Exception>>argument(Supplier.class)
                  .<Exception>throwing(Exception.class)
                  .<Optional<String>>getBiFunctionWithThrows()
                    .map(f->f.andThen(StreamingApiTest::optionalValueToNullableValue))
                    .orElse(null),
                nullableToOptional(row[1]),
                asExceptionSupplier(row[2])
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests map(Function)")
    public class MapTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            Function<String, String> MAPPING = value->value!=null?value+" mapped":"null mapped";
            Function<String, String> NULL_MAPPING = null;
            return Arrays.asList(new Object[][] {
                { "map on value"                 , JSON_VALUE, MAPPING     , of("value mapped") },
                { "map on null"                  , JSON_NULL , MAPPING     , of("null mapped")  },
                { "map on undefined"             , UNDEFINED , MAPPING     , UNDEFINED                },
                { "map null mapping on value"    , JSON_VALUE, NULL_MAPPING, NPE                      },
                { "map null mapping on null"     , JSON_NULL , NULL_MAPPING, NPE                      },
                { "map null mapping on undefined", UNDEFINED , NULL_MAPPING, NPE                      },
            }).stream();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Callable<?> createCall(Object[] row) {
            return biFunctionCall(
                JsonNullable<String>::map,
                asJson(row[1]),
                (Function<String, String>)row[2]
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return biFunctionCall(
                OPTIONAL.forName("map")
                  .<Function<Optional<String>, Optional<String>>>argument(Function.class)
                  .<Optional<Optional<String>>>getBiFunction()
                    .map(f->f.andThen(StreamingApiTest::optionalToNullable))
                    .orElse(null),
                nullableToOptional(asJson(row[1])),
                nullableValueToOptionalValue(asFunction(row[2]))
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests filter(Predicate)")
    public class FilterTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            TestPredicate<?> KEEP_ALL       = value->true;
            TestPredicate<?> KEEP_NULL      = value->value==null;
            TestPredicate<?> KEEP_VALUE     = value->value!=null;
            TestPredicate<?> KEEP_NONE      = value->false;
            TestPredicate<?> NULL_PREDICATE = null;
            return Arrays.asList(new Object[][] {
                { "filter keep all on value"          , JSON_VALUE, KEEP_ALL      , JSON_VALUE    },
                { "filter keep all on null"           , JSON_NULL , KEEP_ALL      , JSON_NULL     },
                { "filter keep all on undefined"      , UNDEFINED , KEEP_ALL      , UNDEFINED     },
                { "filter keep value on value"        , JSON_VALUE, KEEP_VALUE    , JSON_VALUE    },
                { "filter keep value on null"         , JSON_NULL , KEEP_VALUE    , UNDEFINED     },
                { "filter keep value on undefined"    , UNDEFINED , KEEP_VALUE    , UNDEFINED     },
                { "filter keep null on value"         , JSON_VALUE, KEEP_NULL     , UNDEFINED     },
                { "filter keep null on null"          , JSON_NULL , KEEP_NULL     , JSON_NULL     },
                { "filter keep null on undefined"     , UNDEFINED , KEEP_NULL     , UNDEFINED     },
                { "filter remove on value"            , JSON_VALUE, KEEP_NONE     , UNDEFINED     },
                { "filter remove on null"             , JSON_NULL , KEEP_NONE     , UNDEFINED     },
                { "filter remove on undefined"        , UNDEFINED , KEEP_NONE     , UNDEFINED     },
                { "filter null predicate on value"    , JSON_VALUE, NULL_PREDICATE, NPE           },
                { "filter null predicate on null"     , JSON_NULL , NULL_PREDICATE, NPE           },
                { "filter null predicate on undefined", UNDEFINED , NULL_PREDICATE, NPE           },
            }).stream();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Callable<?> createCall(Object[] row) {
            return biFunctionCall(
                JsonNullable<String>::filter,
                asJson(row[1]),
                (Predicate<String>)row[2]
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return biFunctionCall(
                OPTIONAL.forName("filter")
                  .<Predicate<Optional<String>>>argument(Predicate.class)
                  .<Optional<Optional<String>>>getBiFunction()
                    .map(f->f.andThen(StreamingApiTest::optionalToNullable))
                    .orElse(null),
                nullableToOptional(row[1]),
                nullableValueToOptionalValue(asPredicate(row[2]))
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests flatMap(Function)")
    public class FlatMapTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            Function<String, JsonNullable<String>> ANY_TO_UNDEFINED = value->UNDEFINED;
            Function<String, JsonNullable<String>> NULL_TO_UNDEFINED = value->value!=null?of(value):UNDEFINED;
            Function<String, JsonNullable<String>> ANY_TO_OTHER = value->JSON_OTHER;
            Function<String, JsonNullable<String>> NULL_MAPPING = null;
            Function<String, JsonNullable<String>> NULL_RESULT = value->null;
            return Arrays.asList(new Object[][] {
                { "flatMap any to undefined on value"     , JSON_VALUE, ANY_TO_UNDEFINED , UNDEFINED  },
                { "flatMap any to undefined on null"      , JSON_NULL , ANY_TO_UNDEFINED , UNDEFINED  },
                { "flatMap any to undefined on undefined" , UNDEFINED , ANY_TO_UNDEFINED , UNDEFINED  },
                { "flatMap null to undefined on value"    , JSON_VALUE, NULL_TO_UNDEFINED, JSON_VALUE },
                { "flatMap null to undefined on null"     , JSON_NULL , NULL_TO_UNDEFINED, UNDEFINED  },
                { "flatMap null to undefined on undefined", UNDEFINED , NULL_TO_UNDEFINED, UNDEFINED  },
                { "flatMap any to other on value"         , JSON_VALUE, ANY_TO_OTHER     , JSON_OTHER },
                { "flatMap any to other on null"          , JSON_NULL , ANY_TO_OTHER     , JSON_OTHER },
                { "flatMap any to other on undefined"     , UNDEFINED , ANY_TO_OTHER     , UNDEFINED  },
                { "flatMap null mapping on value"         , JSON_VALUE, NULL_MAPPING     , NPE        },
                { "flatMap null mapping on null"          , JSON_NULL , NULL_MAPPING     , NPE        },
                { "flatMap null mapping on undefined"     , UNDEFINED , NULL_MAPPING     , NPE        },
                { "flatMap null result on value"          , JSON_VALUE, NULL_RESULT      , NPE        },
                { "flatMap null result on null"           , JSON_NULL , NULL_RESULT      , NPE        },
                { "flatMap null result on undefined"      , UNDEFINED , NULL_RESULT      , UNDEFINED  },
            }).stream();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<Arguments> args() {
            return baseArgs()
                .map(args->new Object[] { args[0], biFunctionCall(JsonNullable<String>::flatMap, asJson(args[1]), (Function<String, JsonNullable<String>>)args[2]), createMatcher(args[3])})
                .map(args->Arguments.argumentSet((String)args[0], args));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Callable<?> createCall(Object[] row) {
            return biFunctionCall(
                JsonNullable<String>::flatMap,
                asJson(row[1]),
                (Function<String, JsonNullable<String>>)row[2]
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return biFunctionCall(
                OPTIONAL.forName("flatMap")
                  .<Function<Optional<String>, Optional<Optional<String>>>>argument(Function.class)
                  .<Optional<Optional<String>>>getBiFunction()
                    .map(f->f.andThen(StreamingApiTest::optionalToNullable))
                    .orElse(null),
                nullableToOptional(row[1]),
                Optional.ofNullable(asFunction(row[2]))
                    .map(f->f
                        .compose(StreamingApiTest::optionalValueToNullableValue)
                        .andThen(StreamingApiTest::nullableToOptional))
                    .orElse(null)
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests isUndefined()")
    public class IsUndefinedTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            return Arrays.asList(new Object[][] {
                { "isUndefined on value"    , JSON_VALUE, false },
                { "isUndefined on null"     , JSON_NULL , false },
                { "isUndefined on undefined", UNDEFINED , true  },
            }).stream();
        }

        @Override
        public Callable<?> createCall(Object[] row) {
            return functionCall(
                JsonNullable<String>::isUndefined,
                asJson(row[1])
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return functionCall(
                OPTIONAL.forName("isEmpty")
                    .<Boolean>getFunction()
                    .orElse(null),
                nullableToOptional(row[1])
            );
        }
    }

    @Nested
    @DisplayName("Streaming API Tests stream()")
    public class StreamTest extends BaseTest {
        public Stream<Object[]> baseArgs() {
            return Arrays.asList(new Object[][] {
                { "stream on value"    , JSON_VALUE, new Object[] {VALUE} },
                { "stream on null"     , JSON_NULL , new Object[] {NULL}  },
                { "stream on undefined", UNDEFINED , new Object[] {}      }
            }).stream();
        }

        @Override
        public Callable<?> createCall(Object[] row) {
            return functionCall(
                ((Function<JsonNullable<String>, Stream<String>>)JsonNullable<String>::stream)
                    .andThen(s->s.collect(Collectors.toList())),
                asJson(row[1])
            );
        }

        @Override
        public Callable<?> createEquivalentCall(Object[] row) {
            return functionCall(
                OPTIONAL.forName("stream")
                    .<Stream<Optional<String>>>getFunction()
                    .map(f->f
                        .andThen(s->s.map(StreamingApiTest::optionalValueToNullableValue))
                        .andThen(s->s.collect(Collectors.toList())))
                    .orElse(null),
                nullableToOptional(row[1])
            );
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Streaming API Tests ifPresentOrElse(Consumer, Runnable)")
    public class IfPresentOrElseTest {

        public Stream<Object[]> baseArgs() {
            TestConsumer<String> ACTION = (value)->{};
            TestConsumer<String> NULL_ACTION = null;
            Runnable UNDEFINED_ACTION = ()->{};
            Runnable NULL_UNDEFINED_ACTION = null;
            Function<String, BiConsumer<TestConsumer<String>, Runnable>> ACTION_CALLED = (value)->(c, r)->{verify(c).accept(value);};
            BiConsumer<TestConsumer<String>, Runnable> UNDEFINED_ACTION_CALLED = (c, r)->{verify(r).run();};
            BiConsumer<TestConsumer<String>, Runnable> NOTHING_CALLED = (c, r)->{};
            return Arrays.asList(new Object[][] {
                { "ifPresentOrElse on value with action and undefined action"              , JSON_VALUE, ACTION     , UNDEFINED_ACTION     , null, ACTION_CALLED.apply(VALUE) },
                { "ifPresentOrElse on null with action and undefined action"               , JSON_NULL , ACTION     , UNDEFINED_ACTION     , null, ACTION_CALLED.apply(NULL)  },
                { "ifPresentOrElse on undefined with action and undefined action"          , UNDEFINED , ACTION     , UNDEFINED_ACTION     , null, UNDEFINED_ACTION_CALLED    },
                { "ifPresentOrElse on value with null action and undefined action"         , JSON_VALUE, NULL_ACTION, UNDEFINED_ACTION     , NPE , NOTHING_CALLED             },
                { "ifPresentOrElse on null with null action and undefined action"          , JSON_NULL , NULL_ACTION, UNDEFINED_ACTION     , NPE , NOTHING_CALLED             },
                { "ifPresentOrElse on undefined with null action and undefined action"     , UNDEFINED , NULL_ACTION, UNDEFINED_ACTION     , null, UNDEFINED_ACTION_CALLED    },
                { "ifPresentOrElse on value with action and null undefined action"         , JSON_VALUE, ACTION     , NULL_UNDEFINED_ACTION, null, ACTION_CALLED.apply(VALUE) },
                { "ifPresentOrElse on null with action and null undefined action"          , JSON_NULL , ACTION     , NULL_UNDEFINED_ACTION, null, ACTION_CALLED.apply(NULL)  },
                { "ifPresentOrElse on undefined with action and null undefined action"     , UNDEFINED , ACTION     , NULL_UNDEFINED_ACTION, NPE , NOTHING_CALLED             },
                { "ifPresentOrElse on value with null action and null undefined action"    , JSON_VALUE, NULL_ACTION, NULL_UNDEFINED_ACTION, NPE , NOTHING_CALLED             },
                { "ifPresentOrElse on null with null action and null undefined action"     , JSON_NULL , NULL_ACTION, NULL_UNDEFINED_ACTION, NPE , NOTHING_CALLED             },
                { "ifPresentOrElse on undefined null with action and null undefined action", UNDEFINED , NULL_ACTION, NULL_UNDEFINED_ACTION, NPE , NOTHING_CALLED             },
            }).stream();
        }

        public Stream<Arguments> args() {
            return baseArgs()
                .map(args->new Object[] {
                    args[0],
                    args[1],
                    args[2] != null ? mock(TestConsumer.class, AdditionalAnswers.delegatesTo((asConsumer(args[2])))) : null,
                    args[3] != null ? mock(Runnable.class, AdditionalAnswers.delegatesTo((Runnable)args[3])) : null,
                    args[4],
                    args[5]
                })
                .map(args->new Object[] {
                    args[0],
                    triConsumerCall(JsonNullable<String>::ifPresentOrElse, asJson(args[1]), asConsumer(args[2]), (Runnable)args[3]),
                    createMatcher(args[4]),
                    new Runnable() {
                        public void run() {
                            asBiConsumer(args[5]).accept(asConsumer(args[2]), (Runnable)args[3]);
                            if ( args[2] != null ) verifyNoMoreInteractions(args[2]);
                            if ( args[3] != null ) verifyNoMoreInteractions(args[3]);
                        }
                    }
                })
                .map(args->Arguments.argumentSet((String)args[0], args));
        }

        public Stream<Arguments> equivalentArgs() {
            return baseArgs()
                .map(args->new Object[] {
                    args[0],
                    args[1],
                    args[2] != null ? mock(TestConsumer.class, AdditionalAnswers.delegatesTo((asConsumer(args[2])))) : null,
                    args[3] != null ? mock(Runnable.class, AdditionalAnswers.delegatesTo((Runnable)args[3])) : null,
                    args[4],
                    args[5]
                })
                .map(args->new Object[] {
                    args[0],
                    triConsumerCall(
                        OPTIONAL.forName("ifPresentOrElse")
                            .<Consumer<Optional<String>>>argument(Consumer.class)
                            .<Runnable>argument(Runnable.class)
                            .getTriConsumer()
                            .orElse(null),
                        nullableToOptional(args[1]),
                        (TestConsumer<Optional<String>>)(value)->(asConsumer(args[2])).accept(optionalValueToNullableValue(value)),
                        (Runnable)args[3]
                    ),
                    createMatcher(args[4]),
                    new Runnable() {
                        public void run() {
                            asBiConsumer(args[5]).accept(asConsumer(args[2]), (Runnable)args[3]);
                            if ( args[2] != null ) verifyNoMoreInteractions(args[2]);
                            if ( args[3] != null ) verifyNoMoreInteractions(args[3]);
                        }
                    }
                })
                .map(args->Arguments.argumentSet("equivalent "+(String)args[0], args));
        }

        @ParameterizedTest
        @MethodSource("args")
        public void standardCall(String name, Callable<JsonNullable<String>> callable, Matcher<Object> matcher, Runnable verify) {
            Assumptions.assumeTrue(callable!=null);
            Object actual = null;
            try {
                actual = callable.call();
            } catch (Throwable t) {
                assertThat(t, matcher);
                verify.run();
                return;
            }
            assertThat(actual, matcher);
            verify.run();
        }

        @ParameterizedTest
        @MethodSource("equivalentArgs")
        public void equivalentCall(String name, Callable<Optional<Optional<String>>> callable, Matcher<Object> matcher, Runnable verify) {
            Assumptions.assumeTrue(callable!=null);
            Object actual = null;
            try {
                actual = callable.call();
            } catch (Throwable t) {
                assertThat(t, matcher);
                verify.run();
                return;
            }
            assertThat(actual, matcher);
            verify.run();
        }
    }

    //
    // These static methods form a two-way correspondence between JsonNullable<String> and
    // Optional<Optional<String>>
    //
    static Optional<Optional<String>> nullableToOptional(JsonNullable<String> value) {
        if ( value.isUndefined() ) {
            return Optional.empty();
        } else {
            return Optional.of(nullableValueToOptionalValue(value.get()));
        }
    }

    @SuppressWarnings("unchecked")
    static Optional<Optional<String>> nullableToOptional(Object value) {
        if( value != null && !(value instanceof JsonNullable) ) throw new IllegalArgumentException();
        return nullableToOptional((JsonNullable<String>)value);
    }

    static Optional<String> nullableValueToOptionalValue(String value) {
        return Optional.ofNullable(value);
    }

    static Optional<String> nullableValueToOptionalValue(Object value) {
        if( value != null && !(value instanceof String)) throw new IllegalArgumentException();
        return Optional.ofNullable((String)value);
    }

    static JsonNullable<String> optionalToNullable(Optional<Optional<String>> value) {
        if( !value.isPresent() ) {
            return JsonNullable.undefined();
        }
        return JsonNullable.of(optionalValueToNullableValue(value.get()));
    }

    static String optionalValueToNullableValue( Optional<String> value ) {
        return value.orElse(null);
    }

    public static TestPredicate<Optional<String>> nullableValueToOptionalValue( TestPredicate<String> test ) {
        if( test == null ) return null;
        return test.compose(StreamingApiTest::optionalValueToNullableValue);
    }

    public static Function<Optional<String>, Optional<String>> nullableValueToOptionalValue(Function<String, String> f) {
        if( f == null ) return null;
        return f
            .compose(StreamingApiTest::optionalValueToNullableValue)
            .andThen(StreamingApiTest::nullableValueToOptionalValue);
    }

    public Supplier<Optional<Optional<String>>> nullableSupplierToOptionalSupplier(Supplier<JsonNullable<String>> supplier) {
        if( supplier == null ) return null;
        return ()->{
            return nullableToOptional(supplier.get());
        };
    }

    public static Supplier<Optional<String>> nullableValueSupplierToOptionalValueSupplier(Supplier<String> supplier) {
        if( supplier == null ) return null;
        return ()->{
            return nullableValueToOptionalValue(supplier.get());
        };
    }

    //
    // This service provides reflective method calls using functional interfaces.
    // In the case that a method is not defined in the current JDK version,
    // Optional.empty() is returned.
    //
    public static class ReflectiveService<T> {
        Class<? super T> clazz;
        public ReflectiveService(Class<? super T> clazz) {
            this.clazz = clazz;
        }

        public Optional<Method> getMethod(String methodName, Class<?>... parameterTypes) {
            try {
                return Optional.of(clazz.getMethod(methodName, parameterTypes));
            } catch ( Exception e ) {
                return Optional.empty();
            }
        }

        public NoArguments forName(String name) {
            return new NoArguments(name);
        }

        public class NoArguments {
            String methodName;
            public NoArguments(String methodName) {
                this.methodName = methodName;
            }
            @SuppressWarnings("unchecked")
            public <R> Optional<Function<T, R>> getFunction() {
                return getMethod(methodName)
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

            public <A> OneArgument<A> argument(Class<? super A> firstType) {
                return new OneArgument<A>(firstType);
            }

            public class OneArgument<A> {
                private Class<? super A> firstType;

                public OneArgument(Class<? super A> firstType) {
                    this.firstType = firstType;
                }

                public <E extends Exception> OneArgumentWithThrows<A, E> throwing(Class<E> thrownType) {
                    return new OneArgumentWithThrows<A, E>(firstType);
                }

                public <A2> TwoArguments<A, A2> argument(Class<? super A2> secondType) {
                    return new TwoArguments<A, A2>(firstType, secondType);
                }

                @SuppressWarnings("unchecked")
                public <R> Optional<BiFunction<T, A, R>> getBiFunction() {
                    return getMethod(methodName, firstType)
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
            }

            public class OneArgumentWithThrows<A, E extends Throwable> {
                private Class<? super A> firstType;

                public OneArgumentWithThrows(Class<? super A> firstType) {
                    this.firstType = firstType;
                }

                @SuppressWarnings("unchecked")
                public <R> Optional<BiFunctionWithThrows<T, A, R, E>> getBiFunctionWithThrows() {
                    return getMethod(methodName, firstType)
                        .map(method->{
                            return (optional, argument)->{
                                try {
                                    return (R)method.invoke(optional, argument);
                                } catch ( InvocationTargetException ite ) {
                                    if( ite.getCause() instanceof RuntimeException ) {
                                        throw (RuntimeException)ite.getCause();
                                    } else if ( ite.getCause() instanceof Error ) {
                                        throw (Error)ite.getCause();
                                    } else if ( ite.getCause() instanceof Exception ) {
                                        throw (E)ite.getCause();
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
            }

            public class TwoArguments<A1, A2> {
                private Class<? super A1> firstType;
                private Class<? super A2> secondType;

                public TwoArguments( Class<? super A1> firstType, Class<? super A2> secondType) {
                    this.firstType = firstType;
                    this.secondType = secondType;
                }
                public Optional<TriConsumer<T, A1, A2>> getTriConsumer() {
                    return getMethod(methodName, firstType, secondType)
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
            }
        }
    }

    /*
     * Methods that aid in casting objects
     */
    @SuppressWarnings("unchecked")
    public static JsonNullable<String> asJson(Object value) {
        return (JsonNullable<String>)value;
    }

    @SuppressWarnings("unchecked")
    public static <T> TestConsumer<T> asConsumer(Object o) {
        return (TestConsumer<T>)o;
    }

    @SuppressWarnings("unchecked")
    public static <T, U> BiConsumer<T, U> asBiConsumer(Object o) {
        return (BiConsumer<T, U>)o;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Function<T, R> asFunction(Object o) {
        return (Function<T, R>)o;
    }

    @SuppressWarnings("unchecked")
    public static <T> TestSupplier<T> asSupplier(Object o) {
        return (TestSupplier<T>)o;
    }

    @SuppressWarnings("unchecked")
    public static <T> TestPredicate<T> asPredicate(Object o) {
        return (TestPredicate<T>)o;
    }

    @SuppressWarnings("unchecked")
    public static TestSupplier<Exception> asExceptionSupplier(Object value) {
        return (TestSupplier<Exception>)value;
    }

    /*
     * These functions create calls for different method signatures.
     */
    public static <A, R> Callable<R> functionCall(Function<A, R> f, A a) {
        if(f == null) return null;
        return ()->{
            return f.apply((A)a);
        };
    }

    public static <A, B, R> Callable<R> biFunctionCall(BiFunction<A, B, R> f, A a, B b) {
        if(f == null) return null;
        return ()->{
            return f.apply((A)a, (B)b);
        };
    }

    public static <A, B, R, X extends Throwable> Callable<R> biFunctionWithThrowsCall(BiFunctionWithThrows<A, B, R, X> f, A a, B b) {
        if(f == null) return null;
        return ()->{
            try {
                return f.apply((A)a, (B)b);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw e;
            } catch (Throwable t) {
                throw new IllegalStateException(t);
            }
        };
    }

    public static <A, B, C> Callable<Void> triConsumerCall(TriConsumer<A, B, C> f, A a, B b, C c) {
        if(f == null) return null;
        return ()->{
            f.accept(a, b, c);
            return null;
        };
    }

    public static Matcher<?> createMatcher(Object value) {
        if ( value == null ) {
            return nullValue();
        }
        if ( value instanceof Class ) {
            return instanceOf((Class<?>)value);
        }
        if ( value.getClass().isArray() ) {
            Object[] array = (Object[])value;
            return array.length > 0 ? contains(array) : empty();
        }
        return equalTo(value);
    }

    public static <T> TestSupplier<T> supplier(T value) {
        return ()->value;
    }

    //
    // Interfaces and classes created to aid testing.
    //
    public static interface TestPredicate<T> extends Predicate<T> {
        public default <U> TestPredicate<U> compose( Function<? super U, ? extends T> f ) {
            return (value)->{
                return test(f.apply(value));
            };
        }
    }

    public static interface TestSupplier<T> extends Supplier<T> {
        public default <U> TestSupplier<U> andThen(Function<? super T, ? extends U> mapping) {
            return ()->{
                return mapping.apply(get());
            };
        }
    }

    public static interface TestConsumer<T> extends Consumer<T> {
        public default <U> TestConsumer<U> compose( Function<? super U, ? extends T> f) {
            return (value)->{
                accept(f.apply(value));
            };
        }
    }

    public static interface TriConsumer<A, B, C> {
        public void accept(A a, B b, C c);
    }

    public static interface BiFunctionWithThrows<T, U, R, X extends Throwable> {
        public R apply(T t, U u)
            throws X;

        public default <W> BiFunctionWithThrows<T, U, W, X> andThen(Function<? super R, ? extends W> f) {
            return (T t, U u)->{
                return f.apply(apply(t, u));
            };
        }
    }

    public static class TestException extends Exception {}
}
