package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;

public class Jackson2Processor implements JsonProcessor {

    ObjectMapper mapper;

    public Jackson2Processor() {
    }

    @Override
    public JsonProcessor mapperWithModule() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return this;
    }

    @Override
    public JsonProcessor setDateFormat(SimpleDateFormat simpleDateFormat) {
        mapper.setDateFormat(simpleDateFormat);
        return this;
    }

    @Override
    public JsonProcessor setDefaultPropertyInclusion(JsonInclude.Include incl) {
        mapper.setDefaultPropertyInclusion(incl);
        return this;
    }

    @Override
    public JsonProcessor setDefaultPropertyInclusion(JsonInclude.Value incl) {
        mapper.setDefaultPropertyInclusion(incl);
        return this;
    }

    @Override
    public JsonProcessor objectAndNonConcreteTyping() {
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
        return this;
    }

    @Override
    public String writeValueAsString(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    @Override
    public <T> T readValue(String string, Class<T> type) throws JsonProcessingException {
        return mapper.readValue(string, type);
    }

    @Override
    public <T> T readValue(String string, Object typeReference) throws Exception {
        return mapper.readValue(string, (TypeReference<T>) typeReference);
    }

    public static class Jackson2TypeDescriptor implements TypeDescriptor {

        private final JavaType javaType;

        public Jackson2TypeDescriptor(JavaType javaType) {
            this.javaType = javaType;
        }

        @Override
        public boolean isReferenceType() {
            return javaType.isReferenceType();
        }

        @Override
        public Class<?> getRawClass() {
            return javaType.getRawClass();
        }
    }

    @Override
    public TypeDescriptor constructType(Class<?> type) {
        return new Jackson2TypeDescriptor(mapper.constructType(type));
    }

    public static class Jackson2CaseChangingStringWrapper implements CaseChangingStringWrapper {
        @JsonSerialize(contentUsing = UpperCasingSerializer.class)
        @JsonDeserialize(contentUsing = LowerCasingDeserializer.class)
        public JsonNullable<String> value = JsonNullable.undefined();

        public Jackson2CaseChangingStringWrapper() {
        }

        public Jackson2CaseChangingStringWrapper(String value) {
            this.value = JsonNullable.of(value);
        }

        @Override
        public JsonNullable<?> getValue() {
            return this.value;
        }
    }

    public static class UpperCasingSerializer extends StdScalarSerializer<String> {
        public UpperCasingSerializer() {
            super(String.class);
        }

        @Override
        public void serialize(String value, JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeString(value.toUpperCase());
        }
    }

    public static class LowerCasingDeserializer extends StdScalarDeserializer<String> {
        public LowerCasingDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            return p.getText().toLowerCase();
        }
    }

    @Override
    public CaseChangingStringWrapper getCaseChangingStringWrapper(String str) {
        return new Jackson2CaseChangingStringWrapper(str);
    }

    @Override
    public CaseChangingStringWrapper getCaseChangingStringWrapper() {
        return new Jackson2CaseChangingStringWrapper();
    }

    @Override
    public Class<? extends CaseChangingStringWrapper> getCaseChangingStringWrapperClass() {
        return Jackson2CaseChangingStringWrapper.class;
    }

    public static class Jackson2AbstractJsonNullable implements AbstractJsonNullable {
        @JsonDeserialize(contentAs=Integer.class)
        public JsonNullable<java.io.Serializable> value;

        @Override
        public JsonNullable<Serializable> getValue() {
            return this.value;
        }
    }

    @Override
    public Class<? extends AbstractJsonNullable> getAbstractJsonNullableClass() {
        return Jackson2AbstractJsonNullable.class;
    }
}
