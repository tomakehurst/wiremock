package com.github.tomakehurst.wiremock.extension.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ArgumentDefinition {

    private final String type;
    private final String value;
    
    @JsonCreator
    public ArgumentDefinition(@JsonProperty("type")String type, @JsonProperty("value")String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

}
