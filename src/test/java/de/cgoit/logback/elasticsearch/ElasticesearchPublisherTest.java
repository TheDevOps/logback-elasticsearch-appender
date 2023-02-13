package de.cgoit.logback.elasticsearch;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ElasticesearchPublisherTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticesearchPublisherTest.class);
    private static final String LOGGER_NAME = "es-logger";
    private static final int MAX_EVENTS = 10_000;
    @Mock
    private ClassicElasticsearchPublisher elasticsearchPublisher;
    @Mock
    private ElasticsearchProperties elasticsearchProperties;
    @Mock
    private Context mockedContext;

    @Test
    public void should_remove_the_right_element_if_max_elements_is_reached() throws IOException {
        setupPublisher(MAX_EVENTS);

        publishEvents(0);

        int maxI = MAX_EVENTS;
        assertEquals(String.format("Event count should be %s", MAX_EVENTS), MAX_EVENTS, elasticsearchPublisher.getEvents().size());
        assertEquals("First event should have message 'Message 1'", "Message 1", elasticsearchPublisher.getEvents().get(0).getMessage());
        assertEquals(String.format("Last event should have message 'Message %s'", maxI), "Message " + maxI, elasticsearchPublisher.getEvents().get(elasticsearchPublisher.getEvents().size() - 1).getMessage());

        // Add one more event. First event (Message 1) should be removed from list
        LOGGER.info("Publish another additional message.");
        publishEvents(maxI);

        maxI = maxI + MAX_EVENTS;
        assertEquals(String.format("Event count should be %s", MAX_EVENTS), MAX_EVENTS, elasticsearchPublisher.getEvents().size());
        assertEquals(String.format("First event should have message 'Message %s'", MAX_EVENTS + 1), "Message " + (MAX_EVENTS + 1), elasticsearchPublisher.getEvents().get(0).getMessage());
        assertEquals(String.format("Last event should have message 'Message %s'", maxI), "Message " + maxI, elasticsearchPublisher.getEvents().get(elasticsearchPublisher.getEvents().size() - 1).getMessage());
    }

    @Test
    public void should_not_remove_any_element_if_max_elements_is_not_set() throws IOException {
        setupPublisher(-1);

        publishEvents(0);

        int maxI = MAX_EVENTS;
        assertEquals(String.format("Event count should be %s", MAX_EVENTS), MAX_EVENTS, elasticsearchPublisher.getEvents().size());
        assertEquals("First event should have message 'Message 1'", "Message 1", elasticsearchPublisher.getEvents().get(0).getMessage());
        assertEquals(String.format("Last event should have message 'Message %s'", maxI), "Message " + maxI, elasticsearchPublisher.getEvents().get(elasticsearchPublisher.getEvents().size() - 1).getMessage());

        // Add one more event. First event (Message 1) should be removed from list
        LOGGER.info("Publish another additional message.");
        publishEvents(maxI);

        maxI = maxI + MAX_EVENTS;
        assertEquals(String.format("Event count should be %s", maxI), maxI, elasticsearchPublisher.getEvents().size());
        assertEquals(String.format("First event should have message 'Message %s'", 1), "Message 1", elasticsearchPublisher.getEvents().get(0).getMessage());
        assertEquals(String.format("Last event should have message 'Message %s'", maxI), "Message " + maxI, elasticsearchPublisher.getEvents().get(elasticsearchPublisher.getEvents().size() - 1).getMessage());
    }

    private void setupPublisher(int maxEvents) throws IOException {
        Settings settings = new Settings();
        settings.setSleepTime(1000 * 60 * 60); // since we don't want to really publish the events
        settings.setWriteSleepTime(1000 * 60 * 60); // since we don't want to really publish the events
        settings.setLoggerName(LOGGER_NAME);
        settings.setMaxEvents(maxEvents);

        ErrorReporter errorReporter = new ErrorReporter(settings, mockedContext);

        elasticsearchPublisher = new ClassicElasticsearchPublisher(mockedContext, errorReporter, settings, elasticsearchProperties, null);
    }

    private void publishEvents(int offset) {
        LOGGER.info("Try to publish {} events.", MAX_EVENTS);
        long start = System.currentTimeMillis();
        int maxI = offset + MAX_EVENTS;
        for (int i = offset + 1; i <= maxI; i++) {
            ILoggingEvent eventToPublish = mock(ILoggingEvent.class);
            given(eventToPublish.getLoggerName()).willReturn(LOGGER_NAME);
            given(eventToPublish.getMessage()).willReturn(String.format("Message %s", i));
            elasticsearchPublisher.addEvent(eventToPublish);
        }
        LOGGER.info("Messages published. Time={}ms", System.currentTimeMillis() - start);
    }
}
