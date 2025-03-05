package de.cgoit.logback.elasticsearch.util;

import ch.qos.logback.access.common.PatternLayout;
import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import de.cgoit.logback.elasticsearch.config.EsProperty;

public class AccessPropertyAndEncoder extends AbstractPropertyAndEncoder<IAccessEvent> {

    public AccessPropertyAndEncoder(EsProperty property, Context context) {
        super(property, context);
    }

    @Override
    protected PatternLayoutBase<IAccessEvent> getLayout() {
        return new PatternLayout();
    }
}
