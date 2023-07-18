package de.cgoit.logback.elasticsearch;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import com.fasterxml.jackson.core.JsonGenerator;
import de.cgoit.logback.elasticsearch.config.EsProperty;
import de.cgoit.logback.elasticsearch.util.ClassicPropertyAndEncoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class EsPropertySerializerTest {
    private final PropertySerializer<ILoggingEvent> propertySerializer = new PropertySerializer<>();
    @Mock
    private Context context;
    @Mock
    private JsonGenerator jsonGenerator;
    @Mock
    private ILoggingEvent loggingEvent;

    @Test
    public void should_default_to_string_type() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("propertyValue");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        assertThat(property.getType(), is(EsProperty.Type.STRING));
        verify(jsonGenerator).writeObjectField("Test", "propertyValue");
    }

    @Test
    public void should_serialize_int_as_number() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("123");
        property.setType("int");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeNumberField("Test", 123);
    }

    @Test
    public void should_serialize_object_when_invalid_int() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("A123Z");
        property.setType("int");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeObjectField("Test", "A123Z");
    }

    @Test
    public void should_serialize_float_as_number() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("12.30");
        property.setType("float");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeNumberField("Test", 12.30f);
    }

    @Test
    public void should_serialize_object_when_invalid_float() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("A12.30Z");
        property.setType("float");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeObjectField("Test", "A12.30Z");
    }

    @Test
    public void should_serialize_true_as_boolean() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("true");
        property.setType("boolean");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeBooleanField("Test", true);
    }

    @Test
    public void should_serialize_object_when_invalid_boolean() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("AtrueZ");
        property.setType("boolean");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeObjectField("Test", "AtrueZ");
    }

    @Test
    public void should_serialize_object_when_invalid_type() throws Exception {
        // given
        EsProperty property = new EsProperty();
        property.setName("Test");
        property.setValue("value");
        property.setType("invalidType");

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeObjectField("Test", "value");
    }

    @Test
    public void should_serialize_object_as_raw_json() throws Exception {
        EsProperty property = new EsProperty();
        property.setName("args");
        property.setValue("{\"name\": \"value\", \"serial\": 1} ");
        property.setType("object");

        org.mockito.Mockito.reset(jsonGenerator);
        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeRawValue("{\"name\": \"value\", \"serial\": 1}");
    }

    @Test
    public void should_serialize_empty_object() throws Exception {
        EsProperty property = new EsProperty();
        property.setName("args");
        property.setValue("");
        property.setType("object");
        property.setAllowEmpty(true);

        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeRawValue("{}");
    }

    @Test
    public void should_serialize_invalid_object_as_text() throws Exception {
        EsProperty property = new EsProperty();
        property.setName("args2");
        property.setValue("{\"name\": \"value\"");
        property.setType("object");

        reset(jsonGenerator);
        // when
        propertySerializer.serializeProperty(jsonGenerator, loggingEvent, new ClassicPropertyAndEncoder(property, context));

        // then
        verify(jsonGenerator).writeObjectField("args2", "{\"name\": \"value\"");
    }
}