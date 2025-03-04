package de.cgoit.logback.elasticsearch.config;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EsProperty {
    private String name;
    private String value;
    private boolean allowEmpty;
    private List<String> ignoredLoggers = new LinkedList<>();

    private Type type = Type.STRING;

    public EsProperty() {
    }

    public EsProperty(String name, String value, boolean allowEmpty) {
        this(name, value, allowEmpty, null);
    }

    public EsProperty(String name, String value, boolean allowEmpty, String ignoredLoggers) {
        this.name = name;
        this.value = value;
        this.allowEmpty = allowEmpty;
        setIgnoredLoggers(ignoredLoggers);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isAllowEmpty() {
        return allowEmpty;
    }

    public void setAllowEmpty(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
    }

    public Type getType() {
        return type;
    }

    public void setType(String type) {
        try {
            this.type = Enum.valueOf(Type.class, type.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.type = Type.STRING;
        }
    }

    public List<String> getIgnoredLoggers()
    {
        return ignoredLoggers;
    }

    public void setIgnoredLoggers(String ignoredLoggers)
    {
        if (ignoredLoggers != null)
        {
            this.ignoredLoggers = Stream.of(ignoredLoggers.split(",")).collect(Collectors.toList());
        }
        else
        {
            this.ignoredLoggers.clear();
        }
    }

    public enum Type {
        STRING, INT, FLOAT, BOOLEAN, OBJECT
    }
}
