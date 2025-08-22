package org.openapitools.jackson.nullable;

import junit.framework.TestCase;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class JsonNullableStreamApiTest {


    @Test
    public void shouldReturnUndefinedAfterStream() {
        final BigDto bigDto = new BigDto(null);

        final JsonNullable<String> accessedValue = JsonNullable.ofMissable(bigDto)
                .map(BigDto::getChild)
                .map(ChildOfBigDto::getGrandChild);

        assertEquals(accessedValue, JsonNullable.undefined());
    }


    @Test
    public void shouldReturnNullAfterStream() {
        final BigDto bigDto = new BigDto(null);

        // we want to receive null state in the end
        JsonNullable.ofMissable(bigDto)
                // that is why we explicitly allow JsonNullable to produce NULL state in the flow
                .flatMap(dto -> JsonNullable.of(dto.getChild()))
                .map(ChildOfBigDto::getGrandChild)
                .ifPresent(TestCase::assertNull);
    }

    @Test
    public void consumeNotNullValue() {
        final String goldenEgg = "goldenEgg";
        final ChildOfBigDto child = new ChildOfBigDto("goldenEgg");
        final BigDto testedDto = new BigDto(child);

        JsonNullable.ofMissable(testedDto)
                .map(BigDto::getChild)
                .map(ChildOfBigDto::getGrandChild)
                .ifNotNull(grandChild -> assertEquals(grandChild, goldenEgg));
    }


    private static class BigDto {
        private ChildOfBigDto child;

        public BigDto(ChildOfBigDto child) {
            this.child = child;
        }

        public ChildOfBigDto getChild() {
            return this.child;
        }
    }

    private static class ChildOfBigDto {
        private String grandChild;

        public ChildOfBigDto(String grandChild) {
            this.grandChild = grandChild;
        }

        public String getGrandChild() {
            return grandChild;
        }
    }
}
