package de.cgoit.logback.elasticsearch;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import com.fasterxml.jackson.core.JsonGenerator;
import de.cgoit.logback.elasticsearch.config.ElasticsearchProperties;
import de.cgoit.logback.elasticsearch.config.HttpRequestHeaders;
import de.cgoit.logback.elasticsearch.config.EsProperty;
import de.cgoit.logback.elasticsearch.config.Settings;
import de.cgoit.logback.elasticsearch.util.AbstractPropertyAndEncoder;
import de.cgoit.logback.elasticsearch.util.ClassicPropertyAndEncoder;
import de.cgoit.logback.elasticsearch.util.ContextMapWriter;
import de.cgoit.logback.elasticsearch.util.ErrorReporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassicElasticsearchPublisher extends AbstractElasticsearchPublisher<ILoggingEvent> {

    protected ContextMapWriter contextMapWriter;

    public ClassicElasticsearchPublisher(Context context, ErrorReporter errorReporter, Settings settings, ElasticsearchProperties properties, HttpRequestHeaders headers) throws IOException {
        super(context, errorReporter, settings, properties, headers);
        contextMapWriter = new ContextMapWriter();
    }

    @Override
    protected AbstractPropertyAndEncoder<ILoggingEvent> buildPropertyAndEncoder(Context context, EsProperty property) {
        return new ClassicPropertyAndEncoder(property, context);
    }

    @Override
    protected void serializeCommonFields(JsonGenerator gen, ILoggingEvent event) throws IOException {
        gen.writeObjectField("@timestamp", getTimestamp(event.getTimeStamp()));
        String type = settings.getType();
        if (type != null) {
            gen.writeObjectField("type", type);
        }

        if (settings.isRawJsonMessage()) {
            gen.writeFieldName("message");
            gen.writeRawValue(event.getFormattedMessage());
        } else {
            String formattedMessage = event.getFormattedMessage();
            if (formattedMessage != null && settings.getMaxMessageSize() > 0 && formattedMessage.length() > settings.getMaxMessageSize()) {
                formattedMessage = formattedMessage.substring(0, settings.getMaxMessageSize()) + "... (abrv.)";
            }
            gen.writeObjectField("message", formattedMessage);
        }

        if (settings.isIncludeMdc()) {
            List<String> excludedKeys = getExcludedMdcKeys();
            for (Map.Entry<String, String> entry : event.getMDCPropertyMap().entrySet()) {
                if (!excludedKeys.contains(entry.getKey())) {
                    String writtenValue = entry.getValue();
                    if (writtenValue != null && writtenValue.length() > 999980)
                    {
                        writtenValue = writtenValue.substring(0, 999980) + "... (abrv.)";
                    }
                    gen.writeObjectField(entry.getKey(), writtenValue);
                }
            }
        }

        if (settings.isEnableContextMap()) {
            contextMapWriter.writeContextMap(gen, event);
        }
    }

    private List<String> getExcludedMdcKeys() {
        /*
         * using a List instead of a Map because the assumption is that
         * the number of excluded keys will be very small and not cause
         * a performance issue
         */
        List<String> result = new ArrayList<>();
        if (settings.getExcludedMdcKeys() != null) {
            String[] parts = settings.getExcludedMdcKeys().split(",");
            for (String part : parts) {
                result.add(part.trim());
            }
        }
        return result;
    }
}
