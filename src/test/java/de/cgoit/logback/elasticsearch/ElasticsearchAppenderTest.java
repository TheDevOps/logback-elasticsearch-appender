package de.cgoit.logback.elasticsearch;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import de.cgoit.logback.elasticsearch.config.BasicAuthentication;
import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchAppenderTest {


    @Mock
    private ClassicElasticsearchPublisher elasticsearchPublisher;
    @Mock
    private ErrorReporter errorReporter;
    @Mock
    private Settings settings;
    @Mock
    private ElasticsearchProperties elasticsearchProperties;
    @Mock
    private Context mockedContext;

    private boolean publisherSet = false;
    private boolean errorReporterSet = false;
    private AbstractElasticsearchAppender<ILoggingEvent> appender;

    @Before
    public void setUp() {

        appender = new ElasticsearchAppender() {
            @Override
            protected ClassicElasticsearchPublisher buildElasticsearchPublisher() throws IOException {
                publisherSet = true;
                return elasticsearchPublisher;
            }

            @Override
            protected ErrorReporter getErrorReporter() {
                errorReporterSet = true;
                return errorReporter;
            }
        };
    }

    @Test
    public void should_set_the_collaborators_when_started() {
        appender.start();


        assertThat(publisherSet, is(true));
        assertThat(errorReporterSet, is(true));
    }

    @Test
    public void should_throw_error_when_publisher_setup_fails_during_startup() {
        ElasticsearchAppender app = new ElasticsearchAppender() {
            @Override
            protected ClassicElasticsearchPublisher buildElasticsearchPublisher() throws IOException {
                throw new IOException("Failed to start Publisher");
            }
        };

        try {
            app.start();
        } catch (Exception e) {
            assertThat(e, IsInstanceOf.instanceOf(RuntimeException.class));
            assertThat(e.getMessage(), is("java.io.IOException: Failed to start Publisher"));
        }


    }

    @Test
    public void should_not_publish_events_when_logger_set() {
        String loggerName = "elastic-debug-log";
        ILoggingEvent eventToLog = mock(ILoggingEvent.class);
        given(eventToLog.getLoggerName()).willReturn(loggerName);


        appender.setLoggerName(loggerName);
        appender.start();


        appender.append(eventToLog);

        verifyNoInteractions(elasticsearchPublisher);
    }


    @Test
    public void should_not_publish_events_when_errorlogger_set() {
        String errorLoggerName = "elastic-error-log";
        ILoggingEvent eventToLog = mock(ILoggingEvent.class);
        given(eventToLog.getLoggerName()).willReturn(errorLoggerName);


        appender.setErrorLoggerName(errorLoggerName);
        appender.start();


        appender.append(eventToLog);

        verifyNoInteractions(elasticsearchPublisher);
    }


    @Test
    public void should_publish_events_when_loggername_is_null() {
        ILoggingEvent eventToPublish = mock(ILoggingEvent.class);
        given(eventToPublish.getLoggerName()).willReturn(null);
        String errorLoggerName = "es-error";

        appender.setErrorLoggerName(errorLoggerName);
        appender.start();


        appender.append(eventToPublish);

        verify(elasticsearchPublisher, times(1)).addEvent(eventToPublish);
    }


    @Test
    public void should_publish_events_when_loggername_is_different_from_the_elasticsearch_loggers() {
        ILoggingEvent eventToPublish = mock(ILoggingEvent.class);
        String differentLoggerName = "different-logger";
        String errorLoggerName = "es-errors";
        given(eventToPublish.getLoggerName()).willReturn(differentLoggerName);


        appender.setErrorLoggerName(errorLoggerName);
        appender.start();


        appender.append(eventToPublish);

        verify(elasticsearchPublisher, times(1)).addEvent(eventToPublish);
    }

    @Test
    public void should_create_error_reporter_with_same_context() {
        ElasticsearchAppender app = new ElasticsearchAppender() {
            @Override
            public Context getContext() {
                return mockedContext;
            }
        };

        ErrorReporter ar = app.getErrorReporter();

        assertThat(ar.getContext(), is(mockedContext));
    }

    @Test
    public void should_delegate_setters_to_settings() throws MalformedURLException {
        ElasticsearchAppender app = new ElasticsearchAppender(settings);
        boolean includeCallerData = false;
        boolean errorsToStderr = false;
        boolean rawJsonMessage = false;
        boolean includeMdc = true;
        String excludedMdcKeys = "stacktrace,url";
        String index = "app-logs";
        String type = "appenderType";
        int maxQueueSize = 10;
        String logger = "es-logger";
        String url = "http://myelasticsearch.mycompany.com";
        String errorLogger = "es-error-logger";
        String failedEventsLogger = "es-failed-events";
        int maxRetries = 10000;
        int aSleepTime = 10000;
        int aSleepTimeAfterError = 10000;
        int readTimeout = 10000;
        int connectTimeout = 5000;
        boolean enableContextMap = true;
        int maxEvents = 1000;
        BasicAuthentication ba = new BasicAuthentication();
        int maxMessageSize = -1;

        app.setIncludeCallerData(includeCallerData);
        app.setSleepTime(aSleepTime);
        app.setSleepTimeAfterError(aSleepTimeAfterError);
        app.setReadTimeout(readTimeout);
        app.setErrorsToStderr(errorsToStderr);
        app.setLogsToStderr(errorsToStderr);
        app.setMaxQueueSize(maxQueueSize);
        app.setIndex(index);
        app.setType(type);
        app.setUrl(url);
        app.setLoggerName(logger);
        app.setErrorLoggerName(errorLogger);
        app.setFailedEventsLoggerName(failedEventsLogger);
        app.setMaxRetries(maxRetries);
        app.setConnectTimeout(connectTimeout);
        app.setRawJsonMessage(rawJsonMessage);
        app.setIncludeMdc(includeMdc);
        app.setExcludedMdcKeys(excludedMdcKeys);
        app.setEnableContextMap(enableContextMap);
        app.setMaxEvents(maxEvents);
        app.setAuthentication(ba);
        app.setMaxMessageSize(maxMessageSize);

        verify(settings, times(1)).setReadTimeout(readTimeout);
        verify(settings, times(1)).setSleepTime(aSleepTime);
        verify(settings, times(1)).setSleepTimeAfterError(aSleepTimeAfterError);
        verify(settings, times(1)).setIncludeCallerData(includeCallerData);
        verify(settings, times(1)).setErrorsToStderr(errorsToStderr);
        verify(settings, times(1)).setLogsToStderr(errorsToStderr);
        verify(settings, times(1)).setMaxQueueSize(maxQueueSize);
        verify(settings, times(1)).setIndex(index);
        verify(settings, times(1)).setType(type);
        verify(settings, times(1)).setUrl(URI.create(url).toURL());
        verify(settings, times(1)).setLoggerName(logger);
        verify(settings, times(1)).setErrorLoggerName(errorLogger);
        verify(settings, times(1)).setFailedEventsLoggerName(failedEventsLogger);
        verify(settings, times(1)).setMaxRetries(maxRetries);
        verify(settings, times(1)).setConnectTimeout(connectTimeout);
        verify(settings, times(1)).setRawJsonMessage(rawJsonMessage);
        verify(settings, times(1)).setIncludeMdc(includeMdc);
        verify(settings, times(1)).setExcludedMdcKeys(excludedMdcKeys);
        verify(settings, times(1)).setEnableContextMap(enableContextMap);
        verify(settings, times(1)).setMaxEvents(maxEvents);
        verify(settings, times(1)).setAuthentication(ba);
        verify(settings, times(1)).setMaxMessageSize(maxMessageSize);
    }


}