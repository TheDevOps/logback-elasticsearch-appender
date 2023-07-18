package de.cgoit.logback.elasticsearch.config;

import java.util.ArrayList;
import java.util.List;

public class ElasticsearchProperties {

    private final List<EsProperty> properties;

    public ElasticsearchProperties() {
        this.properties = new ArrayList<>();
    }

    public List<EsProperty> getProperties() {
        return properties;
    }

    public void addProperty(EsProperty property) {
        properties.add(property);
    }

}
