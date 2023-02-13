package de.cgoit.logback.elasticsearch.writer;

import java.util.Set;

public class StdErrWriter implements SafeWriter {

    public void write(char[] cbuf, int off, int len) {
        System.err.println(new String(cbuf, 0, len));
    }

    public Set<Integer> sendData() {
        // No-op
        return null;
    }

    public boolean hasPendingData() {
        return false;
    }

    @Override
    public void clearData() {
        // Nothing to do
    }
}
