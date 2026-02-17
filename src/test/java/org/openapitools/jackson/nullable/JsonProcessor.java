package org.openapitools.jackson.nullable;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.text.SimpleDateFormat;

public interface JsonProcessor {
    JsonProcessor mapperWithModule();

    JsonProcessor setDateFormat(SimpleDateFormat simpleDateFormat);

    JsonProcessor setDefaultPropertyInclusion(JsonInclude.Include incl);

    JsonProcessor setDefaultPropertyInclusion(JsonInclude.Value incl);

    JsonProcessor objectAndNonConcreteTyping();

    String writeValueAsString(Object obj) throws Exception;

    <T> T readValue(String string, Class<T> type) throws Exception;
    <T> T readValue(String string, Object typeReference) throws Exception;

    interface TypeDescriptor {
        boolean isReferenceType();

        Class<?> getRawClass();
    }

    TypeDescriptor constructType(Class<?> type);

    interface CaseChangingStringWrapper {
        JsonNullable<?> getValue();
    }

    CaseChangingStringWrapper getCaseChangingStringWrapper(String str);
    CaseChangingStringWrapper getCaseChangingStringWrapper();

    Class<? extends CaseChangingStringWrapper> getCaseChangingStringWrapperClass();

    interface AbstractJsonNullable {
        JsonNullable<java.io.Serializable> getValue();
    }

    Class<? extends AbstractJsonNullable> getAbstractJsonNullableClass();
}
