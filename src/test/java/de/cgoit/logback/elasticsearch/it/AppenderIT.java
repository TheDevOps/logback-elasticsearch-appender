package de.cgoit.logback.elasticsearch.it;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Category(Integration.class)
public class AppenderIT extends IntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AppenderIT.class);
    private static final Logger ES_LOG = LoggerFactory.getLogger(ELASTICSEARCH_LOGGER_NAME);
    private static final Logger ES_RAW_LOG = LoggerFactory.getLogger(ELASTICSEARCH_RAW_LOGGER_NAME);

    @Test
    public void testIndexLogEntry() throws IOException {
        final int count = 10000;
        LOG.info("Insert {} entries to es.", count);
        for (int i = 0; i < count / 2; i++) {
            ES_LOG.info("Log some int {} to elasticsearch", i);
        }
        for (int i = count / 2; i < count; i++) {
            ES_LOG.info("Log some int {} to elasticsearch", i, new RuntimeException("This is a runtime exception"));
        }

        checkLogEntries(count);
    }

    @Test
    public void publishInvalidLogEntry() throws IOException {
        final int count = 10000;
        LOG.info("Try to insert an invalid log entry with 'rawJsonMessage=true'");
        String validJsonMsg = "{ \"msg\": \"this is valid\" }";
        String invalidJsonMsg = "{ \"name\": \"my name\", \"var2\": abcd {here it's getting invalid}}}}}";
        for (int i = 0; i < count / 2; i++) {
            ES_RAW_LOG.info(validJsonMsg);
        }
        ES_RAW_LOG.info(invalidJsonMsg);
        for (int i = 0; i < count / 2; i++) {
            ES_RAW_LOG.info(validJsonMsg);
        }

        checkLogEntries(count);
    }
}
