package de.cgoit.logback.elasticsearch.util;

import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import de.cgoit.logback.elasticsearch.config.EsProperty;

public abstract class AbstractPropertyAndEncoder<T> {
    private final EsProperty property;
    private final PatternLayoutBase<T> layout;

    protected AbstractPropertyAndEncoder(EsProperty property, Context context) {
        this.property = property;

        layout = getLayout();
        layout.setContext(context);
        layout.setPattern(property.getValue());
        layout.setPostCompileProcessor(null);
        layout.start();
    }

    protected abstract PatternLayoutBase<T> getLayout();

    public String encode(T event) {
        if (event instanceof ILoggingEvent loggingEvent && property.getIgnoredLoggers().contains(loggingEvent.getLoggerName()))
        {
            return null;
        }
        return layout.doLayout(event);
    }

    public String getName() {
        return property.getName();
    }

    public boolean allowEmpty() {
        return property.isAllowEmpty();
    }

    public EsProperty.Type getType() {
        return property.getType();
    }

    public List<String> getIngnoredLoggers() {
        return property.getIgnoredLoggers();
    }
}
