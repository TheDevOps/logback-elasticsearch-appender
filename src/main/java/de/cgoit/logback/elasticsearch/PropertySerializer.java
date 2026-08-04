package de.cgoit.logback.elasticsearch;

import de.cgoit.logback.elasticsearch.util.AbstractPropertyAndEncoder;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

class PropertySerializer<T> {
    void serializeProperty(JsonGenerator jsonGenerator, T event, AbstractPropertyAndEncoder<T> propertyAndEncoder) throws JacksonException {
        String value = propertyAndEncoder.encode(event);
        if (propertyAndEncoder.allowEmpty() || value != null && !value.isEmpty()) {
            switch (propertyAndEncoder.getType()) {
                case INT:
                    serializeIntField(jsonGenerator, propertyAndEncoder, value);
                    break;
                case FLOAT:
                    serializeFloatField(jsonGenerator, propertyAndEncoder, value);
                    break;
                case BOOLEAN:
                    serializeBooleanField(jsonGenerator, propertyAndEncoder, value);
                    break;
                case OBJECT:
                    serializeJsonObjectField(jsonGenerator, propertyAndEncoder, value);
                    break;
                default:
                    serializeStringField(jsonGenerator, propertyAndEncoder, value);
            }
        }
    }

    private void serializeStringField(JsonGenerator jsonGenerator, AbstractPropertyAndEncoder<T> propertyAndEncoder, String value) throws JacksonException {
        String writtenValue = value;
        if (writtenValue != null && writtenValue.length() > 999980)
        {
            writtenValue = writtenValue.substring(0, 999980) + "... (abrv.)";
        }
        jsonGenerator.writePOJOProperty(propertyAndEncoder.getName(), writtenValue);
    }

    private void serializeIntField(JsonGenerator jsonGenerator, AbstractPropertyAndEncoder<T> propertyAndEncoder, String value) throws JacksonException {
        try {
            jsonGenerator.writeNumberProperty(propertyAndEncoder.getName(), Integer.parseInt(value));
        } catch (NumberFormatException e) {
            serializeStringField(jsonGenerator, propertyAndEncoder, value);
        }
    }

    private void serializeFloatField(JsonGenerator jsonGenerator, AbstractPropertyAndEncoder<T> propertyAndEncoder, String value) throws JacksonException {
        try {
            jsonGenerator.writeNumberProperty(propertyAndEncoder.getName(), Float.parseFloat(value));
        } catch (NumberFormatException e) {
            serializeStringField(jsonGenerator, propertyAndEncoder, value);
        }
    }

    private void serializeBooleanField(JsonGenerator jsonGenerator, AbstractPropertyAndEncoder<T> propertyAndEncoder, String value) throws JacksonException {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            jsonGenerator.writeBooleanProperty(propertyAndEncoder.getName(), Boolean.parseBoolean(value));
        } else {
            serializeStringField(jsonGenerator, propertyAndEncoder, value);
        }
    }

    private void serializeJsonObjectField(JsonGenerator jsonGenerator, AbstractPropertyAndEncoder<T> propertyAndEncoder, String value) throws JacksonException {
        String trimmed = value != null ? value.trim() : "";
        if ("".equals(value)) {
            jsonGenerator.writeName(propertyAndEncoder.getName());
            jsonGenerator.writeRawValue("{}");
        } else if (trimmed.startsWith("{") && trimmed.endsWith("}")
                || trimmed.startsWith("[") && trimmed.endsWith("]")) {
            jsonGenerator.writeName(propertyAndEncoder.getName());
            jsonGenerator.writeRawValue(trimmed);
        } else {
            serializeStringField(jsonGenerator, propertyAndEncoder, value);
        }
    }
}