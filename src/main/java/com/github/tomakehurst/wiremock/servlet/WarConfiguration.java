package com.github.tomakehurst.wiremock.servlet;

import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.core.mappings.InMemoryStubMappingsFactory;
import com.github.tomakehurst.wiremock.core.mappings.StubMappingsFactory;
import com.github.tomakehurst.wiremock.http.trafficlistener.DoNothingWiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.core.MappingsSaver;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.github.tomakehurst.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.google.common.base.Optional;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WarConfiguration implements Options {

    private static final String FILE_SOURCE_ROOT_KEY = "WireMockFileSourceRoot";

    private final ServletContext servletContext;

    public WarConfiguration(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public int portNumber() {
        return 0;
    }

    @Override
    public HttpsSettings httpsSettings() {
        return new HttpsSettings.Builder().build();
    }

    @Override
    public JettySettings jettySettings() {
        return null;
    }

    @Override
    public int containerThreads() {
        return 0;
    }

    @Override
    public boolean browserProxyingEnabled() {
        return false;
    }

    @Override
    public ProxySettings proxyVia() {
        return ProxySettings.NO_PROXY;
    }

    @Override
    public FileSource filesRoot() {
        String fileSourceRoot = servletContext.getInitParameter(FILE_SOURCE_ROOT_KEY);
        return new ServletContextFileSource(servletContext, fileSourceRoot);
    }

    @Override
    public MappingsLoader mappingsLoader() {
        return new JsonFileMappingsSource(filesRoot().child("mappings"));
    }

    @Override
    public MappingsSaver mappingsSaver() {
        return new NotImplementedMappingsSaver();
    }

    @Override
    public Notifier notifier() {
        return null;
    }

    @Override
    public boolean requestJournalDisabled() {
        return false;
    }

    @Override
    public Optional<Integer> maxRequestJournalEntries() {
        String str = servletContext.getInitParameter("maxRequestJournalEntries");
        if(str == null) {
            return Optional.absent();
        }
        return Optional.of(Integer.parseInt(str));
    }

    @Override
    public String bindAddress() {
        return null;
    }

    @Override
    public List<CaseInsensitiveKey> matchingHeaders() {
        return Collections.emptyList();
    }

    @Override
    public boolean shouldPreserveHostHeader() {
        return false;
    }

    @Override
    public String proxyHostHeader() {
        return null;
    }

    @Override
    public HttpServerFactory httpServerFactory() {
        return null;
    }

    @Override
    public <T extends Extension> Map<String, T> extensionsOfType(Class<T> extensionType) {
        return Collections.emptyMap();
    }

    @Override
    public WiremockNetworkTrafficListener networkTrafficListener() {
        return new DoNothingWiremockNetworkTrafficListener();
    }

    @Override
    public StubMappingsFactory stubMappingsFactory() {
        return new InMemoryStubMappingsFactory();
    }
}
