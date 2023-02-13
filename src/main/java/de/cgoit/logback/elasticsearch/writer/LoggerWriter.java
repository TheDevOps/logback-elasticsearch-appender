package de.cgoit.logback.elasticsearch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class LoggerWriter implements SafeWriter {

    private final String loggerName;

    private Logger logger;

    public LoggerWriter(String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        if (logger == null) {
            logger = LoggerFactory.getLogger(loggerName);
        }

        if (logger.isInfoEnabled()) {
            logger.info(new String(cbuf, 0, len));
        }
    }

    @Override
    public Set<Integer> sendData() {
        // No-op
        return null;
    }

    @Override
    public boolean hasPendingData() {
        return false;
    }

    @Override
    public void clearData() {
        // Nothing to do
    }
}
