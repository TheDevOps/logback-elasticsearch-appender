package de.cgoit.logback.elasticsearch;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import de.cgoit.logback.elasticsearch.config.Authentication;
import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties;
import de.cgoit.logback.elasticsearch.config.HttpRequestHeaders;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

public abstract class AbstractElasticsearchAppender<T> extends UnsynchronizedAppenderBase<T> {

    protected Settings settings;
    protected ElasticsearchProperties elasticsearchProperties;
    protected AbstractElasticsearchPublisher<T> publisher;
    protected ErrorReporter errorReporter;
    protected HttpRequestHeaders headers;

    protected AbstractElasticsearchAppender() {
        settings = new Settings();
        headers = new HttpRequestHeaders();
    }

    protected AbstractElasticsearchAppender(Settings settings) {
        this.settings = settings;
        headers = new HttpRequestHeaders();
    }

    @Override
    public void start() {
        super.start();
        errorReporter = getErrorReporter();
        try {
            publisher = buildElasticsearchPublisher();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop()
    {
        super.stop();
        try
        {
            // Sleep for 2 times the configured sleep time to allow the publisher thread to finish processing events
            // before the appender is fully stopped
            Thread.sleep(2L * settings.getSleepTime());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while waiting for publisher to finish", e);
        }
    }

    protected void publishEvent(T eventObject) {
        publisher.addEvent(eventObject);
    }

    //VisibleForTesting
    protected ErrorReporter getErrorReporter() {
        return new ErrorReporter(settings, getContext());
    }

    //VisibleForTesting
    protected abstract AbstractElasticsearchPublisher<T> buildElasticsearchPublisher() throws IOException;

    @Override
    protected void append(T eventObject) {
        appendInternal(eventObject);
    }

    protected abstract void appendInternal(T eventObject);

    public void setProperties(ElasticsearchProperties elasticsearchProperties) {
        setElasticsearchProperties(elasticsearchProperties);
    }

    public void setElasticsearchProperties(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    public ElasticsearchProperties getElasticsearchProperties() {
        return elasticsearchProperties;
    }

    public void setSleepTime(int sleepTime) {
        settings.setSleepTime(sleepTime);
    }

    public void setWriteSleepTime(int writeSleepTime) {
        settings.setSleepTime(writeSleepTime);
    }

    public void setSleepTimeAfterError(int sleepTimeAfterError) {
        settings.setSleepTimeAfterError(sleepTimeAfterError);
    }

    public void setMaxRetries(int maxRetries) {
        settings.setMaxRetries(maxRetries);
    }

    public void setConnectTimeout(int connectTimeout) {
        settings.setConnectTimeout(connectTimeout);
    }

    public void setReadTimeout(int readTimeout) {
        settings.setReadTimeout(readTimeout);
    }

    public void setIncludeCallerData(boolean includeCallerData) {
        settings.setIncludeCallerData(includeCallerData);
    }

    public void setErrorsToStderr(boolean errorsToStderr) {
        settings.setErrorsToStderr(errorsToStderr);
    }

    public void setLogsToStderr(boolean logsToStderr) {
        settings.setLogsToStderr(logsToStderr);
    }

    public void setMaxQueueSize(int maxQueueSize) {
        settings.setMaxQueueSize(maxQueueSize);
    }

    public void setIndex(String index) {
        settings.setIndex(index);
    }

    public void setType(String type) {
        settings.setType(type);
    }

    public void setUrl(String url) throws MalformedURLException {
        URI uri = URI.create(url);
        settings.setUrl(uri.toURL());
    }

    public void setLoggerName(String logger) {
        settings.setLoggerName(logger);
    }

    public void setErrorLoggerName(String logger) {
        settings.setErrorLoggerName(logger);
    }

    public void setFailedEventsLoggerName(String logger) {
        settings.setFailedEventsLoggerName(logger);
    }

    public void setHeaders(HttpRequestHeaders httpRequestHeaders) {
        headers = httpRequestHeaders;
    }

    public void setRawJsonMessage(boolean rawJsonMessage) {
        settings.setRawJsonMessage(rawJsonMessage);
    }

    public void setIncludeMdc(boolean includeMdc) {
        settings.setIncludeMdc(includeMdc);
    }

    public void setExcludedMdcKeys(String setExcludedMdcKeys) {
        settings.setExcludedMdcKeys(setExcludedMdcKeys);
    }

    public void setAuthentication(Authentication auth) {
        settings.setAuthentication(auth);
    }

    public void setMaxMessageSize(int maxMessageSize) {
        settings.setMaxMessageSize(maxMessageSize);
    }

    public void setEnableContextMap(boolean enableContextMap) {
        settings.setEnableContextMap(enableContextMap);
    }

    public void setMaxEvents(int maxEvents) {
        settings.setMaxEvents(maxEvents);
    }

    public Settings getSettings() {
        return settings;
    }

    public HttpRequestHeaders getHeaders()
    {
        return headers;
    }
}
