package de.cgoit.logback.elasticsearch;

import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;
import de.cgoit.logback.elasticsearch.writer.SafeWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ElasticsearchOutputAggregator extends Writer {

    private final Settings settings;
    private final ErrorReporter errorReporter;
    private final List<SafeWriter> writers;

    public ElasticsearchOutputAggregator(Settings settings, ErrorReporter errorReporter) {
        this.writers = new ArrayList<>();
        this.settings = settings;
        this.errorReporter = errorReporter;
    }

    public void addWriter(SafeWriter writer) {
        writers.add(writer);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (SafeWriter writer : writers) {
            writer.write(cbuf, off, len);
        }
    }

    public boolean hasPendingData() {
        for (SafeWriter writer : writers) {
            if (writer.hasPendingData()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOutputs() {
        return !writers.isEmpty();
    }

    public Set<Integer> sendData() throws IOException {
        Set<Integer> failedIndices = Collections.emptySet();
        for (SafeWriter writer : writers) {
            try {
                Set<Integer> fi = writer.sendData();
                if (fi != null) {
                    failedIndices = fi;
                }
            } catch (IOException e) {
                errorReporter.logWarning("Failed to send events to Elasticsearch: " + e.getMessage());
                if (settings.isErrorsToStderr()) {
                    System.err.println("[" + new Date().toString() + "] Failed to send events to Elasticsearch: " + e.getMessage());
                }
                throw e;
            }
        }
        return failedIndices;
    }

    @Override
    public void flush() throws IOException {
        // No-op
    }

    @Override
    public void close() throws IOException {
        // No-op
    }

    public void clearData()
    {
        for (SafeWriter writer : writers) {
            writer.clearData();
        }
    }

}
