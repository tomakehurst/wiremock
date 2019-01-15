package com.github.tomakehurst.wiremock.extension.plugin;

public class PluginInitializationException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -3897578913140850064L;

    public PluginInitializationException(String message) {
        super(message);
    }

    public PluginInitializationException(String message, Throwable e) {
        super(message, e);
    }
}
