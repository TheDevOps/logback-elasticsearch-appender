package de.cgoit.logback.elasticsearch.util;

import ch.qos.logback.classic.spi.LoggingEvent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ContextMapWriterTest {

    @Mock
    private JsonGenerator jsonGenerator;


    private ContextMapWriter contextMapWriter;

    @Before
    public void setup() throws JacksonException {
        contextMapWriter = new ContextMapWriter();
    }

    @Test
    public void should_write_if_last_element_is_map() throws JacksonException {
        LoggingEvent event = new LoggingEvent();
        event.setArgumentArray(new Object[]{"123", ImmutableMap.of("test", 123, "test2", "foo")});
        contextMapWriter.writeContextMap(jsonGenerator, event);
        verify(jsonGenerator, times(1)).writePOJOProperty("context.test", 123);
        verify(jsonGenerator, times(1)).writePOJOProperty("context.test2", "foo");
    }

    @Test
    public void should_not_write_if_arguments_null_or_empty() throws JacksonException {
        LoggingEvent event = new LoggingEvent();
        contextMapWriter.writeContextMap(jsonGenerator, event);
        event.setArgumentArray(new Object[]{});
        contextMapWriter.writeContextMap(jsonGenerator, event);
        verifyNoInteractions(jsonGenerator);
    }

    @Test
    public void should_not_write_if_last_element_not_map() throws JacksonException {
        LoggingEvent event = new LoggingEvent();
        event.setArgumentArray(new Object[]{"23", 3243});
        contextMapWriter.writeContextMap(jsonGenerator, event);
        verifyNoInteractions(jsonGenerator);
    }
}