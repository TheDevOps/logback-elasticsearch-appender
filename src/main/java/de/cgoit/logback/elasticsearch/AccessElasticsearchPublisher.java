package de.cgoit.logback.elasticsearch;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Context;
import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties;
import de.cgoit.logback.elasticsearch.config.HttpRequestHeaders;
import de.cgoit.logback.elasticsearch.config.EsProperty;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.AbstractPropertyAndEncoder;
import de.cgoit.logback.elasticsearch.util.AccessPropertyAndEncoder;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;
import tools.jackson.core.JsonGenerator;

import java.io.IOException;

public class AccessElasticsearchPublisher extends AbstractElasticsearchPublisher<IAccessEvent> {

    public AccessElasticsearchPublisher(Context context, ErrorReporter errorReporter, Settings settings, ElasticsearchProperties properties, HttpRequestHeaders httpRequestHeaders) throws IOException {
        super(context, errorReporter, settings, properties, httpRequestHeaders);
    }

    @Override
    protected AbstractPropertyAndEncoder<IAccessEvent> buildPropertyAndEncoder(Context context, EsProperty property) {
        return new AccessPropertyAndEncoder(property, context);
    }

    @Override
    protected void serializeCommonFields(JsonGenerator gen, IAccessEvent event) throws IOException {
        gen.writePOJOProperty("@timestamp", getTimestamp(event.getTimeStamp()));
        String type = settings.getType();
        if (type != null) {
            gen.writePOJOProperty("type", type);
        }
    }
}
