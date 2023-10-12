package de.cgoit.logback.elasticsearch.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import de.cgoit.logback.elasticsearch.config.HttpRequestHeader;
import de.cgoit.logback.elasticsearch.config.HttpRequestHeaders;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.dto.Response;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ElasticsearchWriter implements SafeWriter {

    private static final ObjectReader objectReader;

    static {
        ObjectMapper mapper = new ObjectMapper();
        objectReader = mapper.readerFor(Map.class);
    }

    private StringBuilder sendBuffer;
    private final ErrorReporter errorReporter;
    private final Settings settings;
    private final Collection<HttpRequestHeader> headerList;
    private boolean bufferExceeded;

    public ElasticsearchWriter(ErrorReporter errorReporter, Settings settings, HttpRequestHeaders headers) {
        this.errorReporter = errorReporter;
        this.settings = settings;
        headerList = headers != null && headers.getHeaders() != null
                ? headers.getHeaders()
                : Collections.emptyList();

        sendBuffer = new StringBuilder();
    }

    private static String slurpErrors(HttpURLConnection urlConnection) {
        try (InputStream stream = urlConnection.getErrorStream()) {
            if (stream == null) {
                return "<no data>";
            }

            StringBuilder builder = new StringBuilder();

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                char[] buf = new char[2048];
                int numRead;
                while ((numRead = reader.read(buf)) > 0) {
                    builder.append(buf, 0, numRead);
                }
            }
            return builder.toString();
        } catch (Exception e) {
            return "<error retrieving data: " + e.getMessage() + ">";
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        if (bufferExceeded) {
            return;
        }

        sendBuffer.append(cbuf, off, len);
    }

    @Override
    public Set<Integer> sendData() throws IOException {
        Set<Integer> failures = null;
        if (sendBuffer.length() <= 0) {
            return failures;
        }

        HttpURLConnection urlConnection = (HttpURLConnection) settings.getUrl().openConnection();
        try {
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(settings.getReadTimeout());
            urlConnection.setConnectTimeout(settings.getConnectTimeout());
            urlConnection.setRequestMethod("POST");

            String body = sendBuffer.toString();

            if (!headerList.isEmpty()) {
                for (HttpRequestHeader header : headerList) {
                    urlConnection.setRequestProperty(header.getName(), header.getValue());
                }
            }

            if (settings.getAuthentication() != null) {
                settings.getAuthentication().addAuth(urlConnection, body);
            }

            try (Writer writer = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(body);
                writer.flush();
            }

            int rc = urlConnection.getResponseCode();
            if (rc == 200) {
                // Marshal response
                Response response = new Response(objectReader.readValue(urlConnection.getInputStream()));
                if (response.hasErrors()) {
                    Map<Integer, Map<String, Object>> failedItems = response.getFailedItems();
                    errorReporter.logWarning("Errors during send. Failed items: " + failedItems);
                    failures = failedItems.keySet();
                }
            } else {
                String data = slurpErrors(urlConnection);
                throw new IOException("Got response code [" + rc + "] from server with data " + data);
            }
        } finally {
            urlConnection.disconnect();
        }

        sendBuffer.setLength(0);
        if (bufferExceeded) {
            errorReporter.logInfo("Send queue cleared - log messages will no longer be lost");
            bufferExceeded = false;
        }

        return failures;
    }

    @Override
    public boolean hasPendingData() {
        return sendBuffer.length() != 0;
    }

    public void checkBufferExceeded() {
        if (!bufferExceeded && sendBuffer.length() >= settings.getMaxQueueSize()) {
            errorReporter.logWarning("Send queue maximum size exceeded - log messages will be lost until the buffer is cleared");
            bufferExceeded = true;
        }
    }

    @Override
    public void clearData() {
        sendBuffer.setLength(0);
        if (bufferExceeded) {
            errorReporter.logInfo("Send queue cleared - log messages will no longer be lost");
            bufferExceeded = false;
        }
    }

}
