package de.cgoit.logback.elasticsearch.config;

import java.util.ArrayList;
import java.util.List;

public class ElasticsearchProperties {

    private final List<EsProperty> properties;

    public ElasticsearchProperties() {
        properties = new ArrayList<>();
    }

    public List<EsProperty> getProperties() {
        return properties;
    }

    public void addProperty(EsProperty property) {
        properties.add(property);
    }

    public void addEsProperty(EsProperty property) {
        properties.add(property);
    }

}
