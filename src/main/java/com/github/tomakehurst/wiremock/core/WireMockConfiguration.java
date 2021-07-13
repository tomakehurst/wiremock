/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.core;

import static com.github.tomakehurst.wiremock.common.BrowserProxySettings.DEFAULT_CA_KESTORE_PASSWORD;
import static com.github.tomakehurst.wiremock.common.BrowserProxySettings.DEFAULT_CA_KEYSTORE_PATH;
import static com.github.tomakehurst.wiremock.core.WireMockApp.MAPPINGS_ROOT;
import static com.github.tomakehurst.wiremock.extension.ExtensionLoader.valueAssignableFrom;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.common.ssl.KeyStoreSettings;
import com.github.tomakehurst.wiremock.common.ssl.KeyStoreSourceFactory;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.ExtensionLoader;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.github.tomakehurst.wiremock.http.HttpServerFactory;
import com.github.tomakehurst.wiremock.http.ThreadPoolFactory;
import com.github.tomakehurst.wiremock.http.trafficlistener.DoNothingWiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServerFactory;
import com.github.tomakehurst.wiremock.jetty9.QueuedThreadPoolFactory;
import com.github.tomakehurst.wiremock.security.Authenticator;
import com.github.tomakehurst.wiremock.security.BasicAuthenticator;
import com.github.tomakehurst.wiremock.security.NoAuthenticator;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.github.tomakehurst.wiremock.standalone.MappingsLoader;
import com.github.tomakehurst.wiremock.standalone.MappingsSource;
import com.github.tomakehurst.wiremock.verification.notmatched.NotMatchedRenderer;
import com.github.tomakehurst.wiremock.verification.notmatched.PlainTextStubNotMatchedRenderer;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WireMockConfiguration implements Options {

  private int portNumber = DEFAULT_PORT;
  private boolean httpDisabled = false;
  private String bindAddress = DEFAULT_BIND_ADDRESS;

  private int containerThreads = DEFAULT_CONTAINER_THREADS;

  private int httpsPort = -1;
  private String keyStorePath = Resources.getResource("keystore").toString();
  private String keyStorePassword = "password";
  private String keyManagerPassword = "password";
  private String keyStoreType = "JKS";
  private String trustStorePath;
  private String trustStorePassword = "password";
  private String trustStoreType = "JKS";
  private boolean needClientAuth;

  private boolean browserProxyingEnabled = false;
  private String caKeystorePath = DEFAULT_CA_KEYSTORE_PATH;
  private String caKeystorePassword = DEFAULT_CA_KESTORE_PASSWORD;
  private String caKeystoreType = "JKS";
  private KeyStoreSettings caKeyStoreSettings = null;
  private boolean trustAllProxyTargets = false;
  private final List<String> trustedProxyTargets = new ArrayList<>();

  private ProxySettings proxySettings = ProxySettings.NO_PROXY;
  private FileSource filesRoot = new SingleRootFileSource("src/test/resources");
  private MappingsSource mappingsSource;

  private Notifier notifier = new Slf4jNotifier(false);
  private boolean requestJournalDisabled = false;
  private Optional<Integer> maxRequestJournalEntries = Optional.absent();
  private List<CaseInsensitiveKey> matchingHeaders = emptyList();

  private boolean preserveHostHeader;
  private String proxyHostHeader;
  private HttpServerFactory httpServerFactory = new JettyHttpServerFactory();
  private ThreadPoolFactory threadPoolFactory = new QueuedThreadPoolFactory();
  private Integer jettyAcceptors;
  private Integer jettyAcceptQueueSize;
  private Integer jettyHeaderBufferSize;
  private Long jettyStopTimeout;
  private Long jettyIdleTimeout;

  private Map<String, Extension> extensions = newLinkedHashMap();
  private WiremockNetworkTrafficListener networkTrafficListener =
      new DoNothingWiremockNetworkTrafficListener();

  private Authenticator adminAuthenticator = new NoAuthenticator();
  private boolean requireHttpsForAdminApi = false;

  private NotMatchedRenderer notMatchedRenderer = new PlainTextStubNotMatchedRenderer();
  private boolean asynchronousResponseEnabled;
  private int asynchronousResponseThreads;
  private ChunkedEncodingPolicy chunkedEncodingPolicy;
  private boolean gzipDisabled = false;
  private boolean stubLoggingDisabled = false;
  private String permittedSystemKeys = null;

  private boolean stubCorsEnabled = false;

  private MappingsSource getMappingsSource() {
    if (mappingsSource == null) {
      mappingsSource = new JsonFileMappingsSource(filesRoot.child(MAPPINGS_ROOT));
    }

    return mappingsSource;
  }

  public static WireMockConfiguration wireMockConfig() {
    return new WireMockConfiguration();
  }

  public static WireMockConfiguration options() {
    return wireMockConfig();
  }

  public WireMockConfiguration port(int portNumber) {
    this.portNumber = portNumber;
    return this;
  }

  public WireMockConfiguration dynamicPort() {
    this.portNumber = DYNAMIC_PORT;
    return this;
  }

  public WireMockConfiguration httpDisabled(boolean httpDisabled) {
    this.httpDisabled = httpDisabled;
    return this;
  }

  public WireMockConfiguration httpsPort(Integer httpsPort) {
    this.httpsPort = httpsPort;
    return this;
  }

  public WireMockConfiguration dynamicHttpsPort() {
    this.httpsPort = DYNAMIC_PORT;
    return this;
  }

  public WireMockConfiguration containerThreads(Integer containerThreads) {
    this.containerThreads = containerThreads;
    return this;
  }

  public WireMockConfiguration jettyAcceptors(Integer jettyAcceptors) {
    this.jettyAcceptors = jettyAcceptors;
    return this;
  }

  public WireMockConfiguration jettyAcceptQueueSize(Integer jettyAcceptQueueSize) {
    this.jettyAcceptQueueSize = jettyAcceptQueueSize;
    return this;
  }

  public WireMockConfiguration jettyHeaderBufferSize(Integer jettyHeaderBufferSize) {
    this.jettyHeaderBufferSize = jettyHeaderBufferSize;
    return this;
  }

  public WireMockConfiguration jettyStopTimeout(Long jettyStopTimeout) {
    this.jettyStopTimeout = jettyStopTimeout;
    return this;
  }

  public WireMockConfiguration jettyIdleTimeout(Long jettyIdleTimeout) {
    this.jettyIdleTimeout = jettyIdleTimeout;
    return this;
  }

  public WireMockConfiguration keystorePath(String path) {
    this.keyStorePath = path;
    return this;
  }

  public WireMockConfiguration keystorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
    return this;
  }

  public WireMockConfiguration keyManagerPassword(String keyManagerPassword) {
    this.keyManagerPassword = keyManagerPassword;
    return this;
  }

  public WireMockConfiguration keystoreType(String keyStoreType) {
    this.keyStoreType = keyStoreType;
    return this;
  }

  public WireMockConfiguration caKeystoreSettings(KeyStoreSettings caKeyStoreSettings) {
    this.caKeyStoreSettings = caKeyStoreSettings;
    return this;
  }

  public WireMockConfiguration caKeystorePath(String path) {
    this.caKeystorePath = path;
    return this;
  }

  public WireMockConfiguration caKeystorePassword(String keyStorePassword) {
    this.caKeystorePassword = keyStorePassword;
    return this;
  }

  public WireMockConfiguration caKeystoreType(String caKeystoreType) {
    this.caKeystoreType = caKeystoreType;
    return this;
  }

  public WireMockConfiguration trustStorePath(String truststorePath) {
    this.trustStorePath = truststorePath;
    return this;
  }

  public WireMockConfiguration trustStorePassword(String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
    return this;
  }

  public WireMockConfiguration trustStoreType(String trustStoreType) {
    this.trustStoreType = trustStoreType;
    return this;
  }

  public WireMockConfiguration needClientAuth(boolean needClientAuth) {
    this.needClientAuth = needClientAuth;
    return this;
  }

  public WireMockConfiguration enableBrowserProxying(boolean enabled) {
    this.browserProxyingEnabled = enabled;
    return this;
  }

  public WireMockConfiguration proxyVia(String host, int port) {
    this.proxySettings = new ProxySettings(host, port);
    return this;
  }

  public WireMockConfiguration proxyVia(ProxySettings proxySettings) {
    this.proxySettings = proxySettings;
    return this;
  }

  public WireMockConfiguration withRootDirectory(String path) {
    this.filesRoot = new SingleRootFileSource(path);
    return this;
  }

  public WireMockConfiguration usingFilesUnderDirectory(String path) {
    return withRootDirectory(path);
  }

  public WireMockConfiguration usingFilesUnderClasspath(String path) {
    fileSource(new ClasspathFileSource(path));
    return this;
  }

  public WireMockConfiguration fileSource(FileSource fileSource) {
    this.filesRoot = fileSource;
    return this;
  }

  public WireMockConfiguration mappingSource(MappingsSource mappingsSource) {
    this.mappingsSource = mappingsSource;
    return this;
  }

  public WireMockConfiguration notifier(Notifier notifier) {
    this.notifier = notifier;
    return this;
  }

  public WireMockConfiguration bindAddress(String bindAddress) {
    this.bindAddress = bindAddress;
    return this;
  }

  public WireMockConfiguration disableRequestJournal() {
    requestJournalDisabled = true;
    return this;
  }

  @Deprecated
  /** @deprecated use {@link #maxRequestJournalEntries(int)} instead */
  public WireMockConfiguration maxRequestJournalEntries(
      Optional<Integer> maxRequestJournalEntries) {
    this.maxRequestJournalEntries = maxRequestJournalEntries;
    return this;
  }

  public WireMockConfiguration maxRequestJournalEntries(int maxRequestJournalEntries) {
    this.maxRequestJournalEntries = Optional.of(maxRequestJournalEntries);
    return this;
  }

  public WireMockConfiguration recordRequestHeadersForMatching(List<String> headers) {
    this.matchingHeaders = transform(headers, CaseInsensitiveKey.TO_CASE_INSENSITIVE_KEYS);
    return this;
  }

  public WireMockConfiguration preserveHostHeader(boolean preserveHostHeader) {
    this.preserveHostHeader = preserveHostHeader;
    return this;
  }

  public WireMockConfiguration proxyHostHeader(String hostHeaderValue) {
    this.proxyHostHeader = hostHeaderValue;
    return this;
  }

  public WireMockConfiguration extensions(String... classNames) {
    extensions.putAll(ExtensionLoader.load(classNames));
    return this;
  }

  public WireMockConfiguration extensions(Extension... extensionInstances) {
    extensions.putAll(ExtensionLoader.asMap(asList(extensionInstances)));
    return this;
  }

  public WireMockConfiguration extensions(Class<? extends Extension>... classes) {
    extensions.putAll(ExtensionLoader.load(classes));
    return this;
  }

  public WireMockConfiguration httpServerFactory(HttpServerFactory serverFactory) {
    httpServerFactory = serverFactory;
    return this;
  }

  public WireMockConfiguration threadPoolFactory(ThreadPoolFactory threadPoolFactory) {
    this.threadPoolFactory = threadPoolFactory;
    return this;
  }

  public WireMockConfiguration networkTrafficListener(
      WiremockNetworkTrafficListener networkTrafficListener) {
    this.networkTrafficListener = networkTrafficListener;
    return this;
  }

  public WireMockConfiguration adminAuthenticator(Authenticator authenticator) {
    this.adminAuthenticator = authenticator;
    return this;
  }

  public WireMockConfiguration basicAdminAuthenticator(String username, String password) {
    return adminAuthenticator(new BasicAuthenticator(username, password));
  }

  public WireMockConfiguration requireHttpsForAdminApi() {
    this.requireHttpsForAdminApi = true;
    return this;
  }

  public WireMockConfiguration notMatchedRenderer(NotMatchedRenderer notMatchedRenderer) {
    this.notMatchedRenderer = notMatchedRenderer;
    return this;
  }

  public WireMockConfiguration asynchronousResponseEnabled(boolean asynchronousResponseEnabled) {
    this.asynchronousResponseEnabled = asynchronousResponseEnabled;
    return this;
  }

  public WireMockConfiguration asynchronousResponseThreads(int asynchronousResponseThreads) {
    this.asynchronousResponseThreads = asynchronousResponseThreads;
    return this;
  }

  public WireMockConfiguration useChunkedTransferEncoding(ChunkedEncodingPolicy policy) {
    this.chunkedEncodingPolicy = policy;
    return this;
  }

  public WireMockConfiguration gzipDisabled(boolean gzipDisabled) {
    this.gzipDisabled = gzipDisabled;
    return this;
  }

  public WireMockConfiguration stubRequestLoggingDisabled(boolean disabled) {
    this.stubLoggingDisabled = disabled;
    return this;
  }

  public WireMockConfiguration stubCorsEnabled(boolean enabled) {
    this.stubCorsEnabled = enabled;
    return this;
  }

  public WireMockConfiguration trustAllProxyTargets(boolean enabled) {
    this.trustAllProxyTargets = enabled;
    return this;
  }

  public WireMockConfiguration trustedProxyTargets(String... trustedProxyTargets) {
    return trustedProxyTargets(asList(trustedProxyTargets));
  }

  public WireMockConfiguration trustedProxyTargets(List<String> trustedProxyTargets) {
    this.trustedProxyTargets.addAll(trustedProxyTargets);
    return this;
  }

  @Override
  public int portNumber() {
    return portNumber;
  }

  @Override
  public boolean getHttpDisabled() {
    return httpDisabled;
  }

  @Override
  public int containerThreads() {
    return containerThreads;
  }

  @Override
  public HttpsSettings httpsSettings() {
    return new HttpsSettings.Builder()
        .port(httpsPort)
        .keyStorePath(keyStorePath)
        .keyStorePassword(keyStorePassword)
        .keyManagerPassword(keyManagerPassword)
        .keyStoreType(keyStoreType)
        .trustStorePath(trustStorePath)
        .trustStorePassword(trustStorePassword)
        .trustStoreType(trustStoreType)
        .needClientAuth(needClientAuth)
        .build();
  }

  @Override
  public JettySettings jettySettings() {
    return JettySettings.Builder.aJettySettings()
        .withAcceptors(jettyAcceptors)
        .withAcceptQueueSize(jettyAcceptQueueSize)
        .withRequestHeaderSize(jettyHeaderBufferSize)
        .withStopTimeout(jettyStopTimeout)
        .withIdleTimeout(jettyIdleTimeout)
        .build();
  }

  @Override
  public boolean browserProxyingEnabled() {
    return browserProxyingEnabled;
  }

  @Override
  public ProxySettings proxyVia() {
    return proxySettings;
  }

  @Override
  public FileSource filesRoot() {
    return filesRoot;
  }

  @Override
  public MappingsLoader mappingsLoader() {
    return getMappingsSource();
  }

  @Override
  public MappingsSaver mappingsSaver() {
    return getMappingsSource();
  }

  @Override
  public Notifier notifier() {
    return notifier;
  }

  @Override
  public boolean requestJournalDisabled() {
    return requestJournalDisabled;
  }

  @Override
  public Optional<Integer> maxRequestJournalEntries() {
    return maxRequestJournalEntries;
  }

  @Override
  public String bindAddress() {
    return bindAddress;
  }

  @Override
  public List<CaseInsensitiveKey> matchingHeaders() {
    return matchingHeaders;
  }

  @Override
  public HttpServerFactory httpServerFactory() {
    return httpServerFactory;
  }

  @Override
  public ThreadPoolFactory threadPoolFactory() {
    return threadPoolFactory;
  }

  @Override
  public boolean shouldPreserveHostHeader() {
    return preserveHostHeader;
  }

  @Override
  public String proxyHostHeader() {
    return proxyHostHeader;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Extension> Map<String, T> extensionsOfType(final Class<T> extensionType) {
    return (Map<String, T>) Maps.filterEntries(extensions, valueAssignableFrom(extensionType));
  }

  @Override
  public WiremockNetworkTrafficListener networkTrafficListener() {
    return networkTrafficListener;
  }

  @Override
  public Authenticator getAdminAuthenticator() {
    return adminAuthenticator;
  }

  @Override
  public boolean getHttpsRequiredForAdminApi() {
    return requireHttpsForAdminApi;
  }

  @Override
  public NotMatchedRenderer getNotMatchedRenderer() {
    return notMatchedRenderer;
  }

  @Override
  public AsynchronousResponseSettings getAsynchronousResponseSettings() {
    return new AsynchronousResponseSettings(
        asynchronousResponseEnabled, asynchronousResponseThreads);
  }

  @Override
  public ChunkedEncodingPolicy getChunkedEncodingPolicy() {
    return chunkedEncodingPolicy;
  }

  @Override
  public boolean getGzipDisabled() {
    return gzipDisabled;
  }

  @Override
  public boolean getStubRequestLoggingDisabled() {
    return stubLoggingDisabled;
  }

  @Override
  public boolean getStubCorsEnabled() {
    return stubCorsEnabled;
  }

  @Override
  public BrowserProxySettings browserProxySettings() {
    KeyStoreSettings keyStoreSettings =
        caKeyStoreSettings != null
            ? caKeyStoreSettings
            : new KeyStoreSettings(
                KeyStoreSourceFactory.getAppropriateForJreVersion(
                    caKeystorePath, caKeystoreType, caKeystorePassword.toCharArray()));

    return new BrowserProxySettings.Builder()
        .enabled(browserProxyingEnabled)
        .trustAllProxyTargets(trustAllProxyTargets)
        .trustedProxyTargets(trustedProxyTargets)
        .caKeyStoreSettings(keyStoreSettings)
        .build();
  }
}
