package com.github.tomakehurst.wiremock.extension.plugin;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ExtensionFile {

    private final List<HelperDefinition> helpers;

    private final List<ExtensionDefinition> extensionList;

    @JsonCreator
    public ExtensionFile(@JsonProperty("helpers") List<HelperDefinition> helpers,
            @JsonProperty("extensionList") List<ExtensionDefinition> extensionList) {
        this.helpers = helpers;
        this.extensionList = extensionList;
    }

    public List<HelperDefinition> getHelpers() {
        if (helpers == null) {
            return Collections.emptyList();
        }
        return helpers;
    }

    public List<ExtensionDefinition> getExtensionList() {
        if (extensionList == null) {
            return Collections.emptyList();
        }
        return extensionList;
    }

}
