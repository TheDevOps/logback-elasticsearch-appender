package de.cgoit.logback.elasticsearch;

import ch.qos.logback.core.Context;
import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties;
import de.cgoit.logback.elasticsearch.config.HttpRequestHeaders;
import de.cgoit.logback.elasticsearch.config.EsProperty;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.AbstractPropertyAndEncoder;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;
import de.cgoit.logback.elasticsearch.writer.ElasticsearchWriter;
import de.cgoit.logback.elasticsearch.writer.FailedEventsWriter;
import de.cgoit.logback.elasticsearch.writer.LoggerWriter;
import de.cgoit.logback.elasticsearch.writer.StdErrWriter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractElasticsearchPublisher<T> implements Runnable {

    public static final String THREAD_NAME_PREFIX = "es-writer-";
    private static final ThreadLocal<DateFormat> DATE_FORMAT = ThreadLocal.withInitial(() ->
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    );
    private final Object lock;
    private final PropertySerializer<T> propertySerializer;
    private final ElasticsearchOutputAggregator outputAggregator;
    private final ElasticsearchWriter elasticWriter;
    private final List<AbstractPropertyAndEncoder<T>> propertyList;
    private final AbstractPropertyAndEncoder<T> indexPattern;
    private final JsonMapper jsonMapper;
    private final JsonFactory jf;
    private final JsonGenerator jsonGenerator;
    private final JsonGenerator failedEventsJsonGenerator;
    private final FailedEventsWriter failedEventsWriter;
    private final ErrorReporter errorReporter;
    protected Settings settings;
    private volatile List<T> events;
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private final AtomicBoolean working = new AtomicBoolean(false);
    private final AtomicLong workingTimestamp = new AtomicLong(0);
    private Long inactiveTimeLimit = 15 * 60 * 1000L;

    protected AbstractElasticsearchPublisher(Context context, ErrorReporter errorReporter, Settings settings, ElasticsearchProperties properties, HttpRequestHeaders headers) throws JacksonException {
        this.errorReporter = errorReporter;
        events = new LinkedList<>();
        lock = new Object();
        this.settings = settings;

        if (settings.getUrl() != null) {
            elasticWriter = new ElasticsearchWriter(errorReporter, settings, headers);
        }
        else
        {
            elasticWriter = null;
        }
        outputAggregator = configureOutputAggregator(settings, errorReporter, elasticWriter);

        jf = JsonFactory.builder().rootValueSeparator((String) null).build();
        jsonMapper = JsonMapper.builder(jf).build();
        jsonGenerator = jsonMapper.createGenerator(outputAggregator);
        if (settings.getFailedEventsLoggerName() != null) {
            failedEventsWriter = new FailedEventsWriter(settings.getFailedEventsLoggerName());
            failedEventsJsonGenerator =  jsonMapper.createGenerator(failedEventsWriter);
        } else {
            failedEventsWriter = null;
            failedEventsJsonGenerator = null;
        }


        indexPattern = buildPropertyAndEncoder(context, new EsProperty("<index>", settings.getIndex(), false, null));
        propertyList = generatePropertyList(context, properties);

        propertySerializer = new PropertySerializer<>();

        if (settings.getSleepTime() * 10 > inactiveTimeLimit)
        {
            inactiveTimeLimit = settings.getSleepTime() * 10L;
        }
    }

    private static ElasticsearchOutputAggregator configureOutputAggregator(Settings settings, ErrorReporter errorReporter, ElasticsearchWriter elasticWriter) {
        ElasticsearchOutputAggregator spigot = new ElasticsearchOutputAggregator(settings, errorReporter);

        if (settings.isLogsToStderr()) {
            spigot.addWriter(new StdErrWriter());
        }

        if (settings.getLoggerName() != null) {
            spigot.addWriter(new LoggerWriter(settings.getLoggerName()));
        }

        if (settings.getUrl() != null) {
            spigot.addWriter(elasticWriter);
        }

        return spigot;
    }

    protected static String getTimestamp(long timestamp) {
        return DATE_FORMAT.get().format(new Date(timestamp));
    }

    private List<AbstractPropertyAndEncoder<T>> generatePropertyList(Context context, ElasticsearchProperties properties) {
        List<AbstractPropertyAndEncoder<T>> list = new ArrayList<>();
        if (properties != null) {
            for (EsProperty property : properties.getProperties()) {
                list.add(buildPropertyAndEncoder(context, property));
            }
        }
        return list;
    }

    protected abstract AbstractPropertyAndEncoder<T> buildPropertyAndEncoder(Context context, EsProperty property);

    public void addEvent(T event) {
        if (!outputAggregator.hasOutputs()) {
            return;
        }

        int max = settings.getMaxEvents();

        synchronized (lock) {
            events.add(event);
            if (max > 0 && events.size() > max) {
                errorReporter.logWarning("Max events in queue reached - log messages will be lost until the queue is processed");
                ((LinkedList<T>) events).removeFirst();
            }
            // in case the working thread has not performed any work for min 15 minutes or else 10x the sleep time if it
            // is higher than 15 minutes (which it really shouldn't) we assume the worker thread died and try to spawn
            // a new one
            if (workingTimestamp.get() < System.currentTimeMillis() - inactiveTimeLimit)
            {
                working.set(false);
            }
            if (!working.get()) {
                workingTimestamp.set(System.currentTimeMillis());
                Thread thread = new Thread(this, THREAD_NAME_PREFIX + settings.getIndex() + "-" + threadCounter.incrementAndGet());
                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    @Override
    public void run() {
        int threadId = 0;
        synchronized (lock) {
            if (!working.compareAndSet(false, true)) {
                return;
            }
            threadId = threadCounter.get();
        }
        DATE_FORMAT.remove();
        int currentTry = 0;
        int maxRetries = settings.getMaxRetries();
        long lastErrorTime = 0;
        long processStartTime = System.currentTimeMillis();
        while (true) {
            try {
                // if this threads ID is lower than the most recent one the only explanation is that it was some old
                // thread that for some unexplainable reason was inactive for a very long time only to suddenly start
                // working again but it already got replaced by a newly started one, so we can let it rest
                if (threadId != threadCounter.get())
                {
                    System.out.println("Exiting thread: " + Thread.currentThread().getName());
                    return;
                }
                workingTimestamp.set(processStartTime);
                Thread.sleep(settings.getWriteSleepTime());
                List<T> eventsCopy = null;
                synchronized (lock) {
                    if (!events.isEmpty()) {
                        eventsCopy = events;
                        events = new LinkedList<>();
                    }
                }

                if (eventsCopy != null) {
                    serializeEvents(jsonGenerator, eventsCopy, propertyList);
                }

                long threadRuntime = System.currentTimeMillis() - processStartTime;
                // check if the current thread is still in the general sleep time or in a previous error backoff
                if (threadRuntime < settings.getSleepTime() || isInErrorBackoff(lastErrorTime, currentTry)) {
                    continue;
                }

                if (currentTry >= maxRetries) {
                    // Oh well, better luck next time
                    errorReporter.logWarning("Error sending data to elastic within " + maxRetries + " attempts. Giving up and considering data lost.");
                    // Remove the data, it really doesn't matter much if old or new logs are lost if elastic
                    // is unreachable for an extended amount of time, and this way we usually will prevent
                    // too big file from being sent at once
                    outputAggregator.clearData();
                    processStartTime = System.currentTimeMillis();
                    currentTry = 0;
                    lastErrorTime = 0;
                    continue;
                }

                try {
                    Set<Integer> failedIndices = outputAggregator.sendData();
                    if (!failedIndices.isEmpty() && eventsCopy != null && failedEventsJsonGenerator != null) {
                        for (Integer idx : failedIndices) {
                            if (idx < eventsCopy.size() - 1) {
                                T event = eventsCopy.get(idx);
                                serializeIndexString(failedEventsJsonGenerator, event);
                                failedEventsJsonGenerator.writeRaw('\n');
                                serializeEvent(failedEventsJsonGenerator, event, propertyList);
                                failedEventsJsonGenerator.writeRaw('\n');
                                failedEventsJsonGenerator.flush();
                            }
                        }
                    }
                } catch (IOException e) {
                    // Fatal error in sendData, increase counter and flag error backoff
                    currentTry++;
                    lastErrorTime = System.currentTimeMillis();
                }
            } catch (InterruptedException interruptedException) {
                synchronized (lock) {
                    working.set(false);
                }
                DATE_FORMAT.remove();
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                errorReporter.logError("Internal error handling log data: " + e.getMessage(), e);
                currentTry++;
                lastErrorTime = System.currentTimeMillis();
            }
            processStartTime = System.currentTimeMillis();
        }
    }

    private boolean isInErrorBackoff(long lastErrorTime, int currentTry) {
        // if no previous error time is given
        if (lastErrorTime == 0) {
            return false;
        }
        // otherwise check if the current desired sleep time is bigger than the time since the last error
        return settings.getSleepTimeAfterError() * (long) currentTry > System.currentTimeMillis() - lastErrorTime;
    }

    private void serializeEvents(JsonGenerator gen, List<T> eventsCopy, List<AbstractPropertyAndEncoder<T>> propertyList) throws IOException {
        for (T event : eventsCopy) {
            serializeIndexString(gen, event);
            gen.writeRaw('\n');
            serializeEvent(gen, event, propertyList);
            gen.writeRaw('\n');
            gen.flush();
            if (elasticWriter != null) {
                elasticWriter.checkBufferExceeded();
            }
        }
    }

    private void serializeIndexString(JsonGenerator gen, T event) throws JacksonException {
        gen.writeStartObject();
        gen.writeObjectPropertyStart("create");
        gen.writePOJOProperty("_index", indexPattern.encode(event));
        gen.writeEndObject();
        gen.writeEndObject();
    }

    private void serializeEvent(JsonGenerator gen, T event, List<AbstractPropertyAndEncoder<T>> propertyList) throws IOException {
        gen.writeStartObject();

        serializeCommonFields(gen, event);

        for (AbstractPropertyAndEncoder<T> pae : propertyList) {
            propertySerializer.serializeProperty(gen, event, pae);
        }

        gen.writeEndObject();
    }

    protected abstract void serializeCommonFields(JsonGenerator gen, T event) throws IOException;

    public List<T> getEvents() {
        return events;
    }
}
