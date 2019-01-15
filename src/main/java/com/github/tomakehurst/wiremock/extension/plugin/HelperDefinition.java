package com.github.tomakehurst.wiremock.extension.plugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class HelperDefinition {

    private final String helperName;
    private final String helperClass;

    @JsonCreator
    public HelperDefinition(@JsonProperty("helperName") String helperName,
            @JsonProperty("helperClass") String helperClass) {
        this.helperName = helperName;
        this.helperClass = helperClass;
    }

    public String getHelperName() {
        return helperName;
    }

    public String getHelperClass() {
        return helperClass;
    }

}
