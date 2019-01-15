package com.github.tomakehurst.wiremock.extension.plugin;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ExtensionDefinition {

    private final String extensionClassname;

    private final List<ArgumentDefinition> arguments;

    @JsonCreator
    public ExtensionDefinition(@JsonProperty("extensionClassname") String extensionClassname,
            @JsonProperty("arguments") List<ArgumentDefinition> arguments) {
        this.extensionClassname = extensionClassname;
        this.arguments = arguments;
    }

    public String getExtensionClassname() {
        return extensionClassname;
    }

    public List<ArgumentDefinition> getArguments() {
        if (arguments == null) {
            return Collections.emptyList();
        }
        return arguments;
    }

}
