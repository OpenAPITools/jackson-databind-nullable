package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public class Jackson3Processor implements JsonProcessor {

    JsonMapper.Builder builder;
    ObjectMapper mapper;

    public Jackson3Processor() {
    }

    @Override
    public JsonProcessor mapperWithModule() {
        builder = JsonMapper.builder().addModule(new JsonNullableJackson3Module());
        return this;
    }

    @Override
    public JsonProcessor setDateFormat(SimpleDateFormat simpleDateFormat) {
        builder.defaultDateFormat(simpleDateFormat);
        return this;
    }

    @Override
    public JsonProcessor setDefaultPropertyInclusion(JsonInclude.Include incl) {
        builder.changeDefaultPropertyInclusion(include -> include.withValueInclusion(incl));
        return this;
    }

    @Override
    public JsonProcessor setDefaultPropertyInclusion(JsonInclude.Value incl) {
        builder.changeDefaultPropertyInclusion(include -> include
                .withValueInclusion(incl.getValueInclusion())
                .withContentInclusion(incl.getContentInclusion()));
        return this;
    }

    @Override
    public JsonProcessor objectAndNonConcreteTyping() {
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build();
        builder.activateDefaultTyping(validator, DefaultTyping.OBJECT_AND_NON_CONCRETE);
        return this;
    }

    @Override
    public String writeValueAsString(Object obj) throws Exception {
        mapper = builder.build();
        return mapper.writeValueAsString(obj);
    }

    @Override
    public <T> T readValue(String string, Class<T> type) throws Exception {
        mapper = builder.build();
        return mapper.readValue(string, type);
    }

    @Override
    public <T> T readValue(String string, Object typeReference) throws Exception {
        mapper = builder.build();
        return mapper.readValue(string, (TypeReference<T>) typeReference);
    }

    public static class Jackson3TypeDescriptor implements TypeDescriptor {

        private final JavaType javaType;

        public Jackson3TypeDescriptor(JavaType javaType) {
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
        mapper = builder.build();
        return new Jackson3TypeDescriptor(mapper.constructType(type));
    }

    public static class Jackson3CaseChangingStringWrapper implements CaseChangingStringWrapper {
        @JsonSerialize(contentUsing = UpperCasingSerializer.class)
        @JsonDeserialize(contentUsing = LowerCasingDeserializer.class)
        public JsonNullable<String> value = JsonNullable.undefined();

        public Jackson3CaseChangingStringWrapper() {
        }

        public Jackson3CaseChangingStringWrapper(String value) {
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
                              SerializationContext context) {
            gen.writeString(value.toUpperCase());
        }
    }

    public static class LowerCasingDeserializer extends StdScalarDeserializer<String> {
        public LowerCasingDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getText().toLowerCase();
        }
    }

    @Override
    public CaseChangingStringWrapper getCaseChangingStringWrapper(String str) {
        return new Jackson3CaseChangingStringWrapper(str);
    }

    @Override
    public CaseChangingStringWrapper getCaseChangingStringWrapper() {
        return new Jackson3CaseChangingStringWrapper();
    }

    @Override
    public Class<? extends CaseChangingStringWrapper> getCaseChangingStringWrapperClass() {
        return Jackson3CaseChangingStringWrapper.class;
    }

    public static class Jackson3AbstractJsonNullable implements AbstractJsonNullable {
        @JsonDeserialize(contentAs=Integer.class)
        public JsonNullable<java.io.Serializable> value;

        @Override
        public JsonNullable<Serializable> getValue() {
            return this.value;
        }
    }

    @Override
    public Class<? extends AbstractJsonNullable> getAbstractJsonNullableClass() {
        return Jackson3AbstractJsonNullable.class;
    }
}
