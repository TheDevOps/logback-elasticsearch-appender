package de.cgoit.logback.elasticsearch.writer;

import java.io.IOException;
import java.util.Set;

public interface SafeWriter {

    void write(char[] cbuf, int off, int len);

    Set<Integer> sendData() throws IOException;

    boolean hasPendingData();

    void clearData();
}
