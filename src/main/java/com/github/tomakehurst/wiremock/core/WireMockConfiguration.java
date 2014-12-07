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

import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.google.common.io.Resources;
import org.apache.commons.lang.StringUtils;

import java.util.List;

import static com.google.common.collect.Lists.transform;
import static java.util.Collections.emptyList;

public class WireMockConfiguration implements Options {

    private int portNumber = DEFAULT_PORT;
    private String bindAddress = DEFAULT_BIND_ADDRESS;
    private Integer httpsPort = null;
    private String keyStorePath = null;
    private String keyStorePassword = "password";
    private String trustStorePath = null;
    private String trustStorePassword = "password";
    private boolean needClientAuth = false;
    private boolean browserProxyingEnabled = false;
    private ProxySettings proxySettings;
    private FileSource filesRoot = new SingleRootFileSource("src/test/resources");
    private Notifier notifier = new Slf4jNotifier(false);
    private boolean requestJournalDisabled = false;
    private List<CaseInsensitiveKey> matchingHeaders = emptyList();

    private String proxyUrl;
    private boolean preserveHostHeader;
    private String proxyHostHeader;

    public static WireMockConfiguration wireMockConfig() {
        return new WireMockConfiguration();
    }

    public WireMockConfiguration port(int portNumber) {
        this.portNumber = portNumber;
        return this;
    }

    public WireMockConfiguration httpsPort(Integer httpsPort) {
        this.httpsPort = httpsPort;
        return this;
    }

    public WireMockConfiguration keystore(String keystore) {
        this.keyStorePath = keystore;
        return this;
    }

    public WireMockConfiguration keyPassword(String password) {
        this.keyStorePassword = password;
        return this;
    }

    public WireMockConfiguration truststore(String truststore) {
        this.trustStorePath = truststore;
        return this;
    }

    public WireMockConfiguration trustPassword(String password) {
        this.trustStorePassword = password;
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
        this.filesRoot = new ClasspathFileSource(path);
        return this;
    }

    public WireMockConfiguration fileSource(FileSource fileSource) {
        this.filesRoot = fileSource;
        return this;
    }

    public WireMockConfiguration notifier(Notifier notifier) {
        this.notifier = notifier;
        return this;
    }
    
    public WireMockConfiguration bindAddress(String bindAddress){
        this.bindAddress = bindAddress;
        return this;
    }

    public WireMockConfiguration disableRequestJournal() {
        requestJournalDisabled = true;
        return this;
    }

    public WireMockConfiguration recordRequestHeadersForMatching(List<String> headers) {
    	this.matchingHeaders = transform(headers, CaseInsensitiveKey.TO_CASE_INSENSITIVE_KEYS);
    	return this;
    }

    public WireMockConfiguration withProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
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
    
    @Override
    public int portNumber() {
        return portNumber;
    }

    @Override
    public HttpsSettings httpsSettings() {
        if (httpsPort == null) {
            return HttpsSettings.NO_HTTPS;
        }

        final String keyStorePath;
        if (StringUtils.isEmpty(this.keyStorePath)) {
            keyStorePath = Resources.getResource("keystore").toString();
        } else {
            keyStorePath = this.keyStorePath;
        }


        final String keyStorePassword;
        if (StringUtils.isEmpty(this.keyStorePassword)) {
            keyStorePassword = "password";
        } else {
            keyStorePassword = this.keyStorePassword;
        }

        if (StringUtils.isEmpty(keyStorePath)) {
            throw new IllegalArgumentException("Try to enable HTTPS port but missing a valid Keystore. " +
                    "Please either specify a valid keystore path, " +
                    "or put the keystore in resource with name 'keystore'");
        }

        return new HttpsSettings(httpsPort,
                keyStorePath, keyStorePassword,
                trustStorePath, trustStorePassword,
                needClientAuth);
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
    public Notifier notifier() {
        return notifier;
    }

    public boolean requestJournalDisabled() {
        return requestJournalDisabled;
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
    public String proxyUrl() {
        return proxyUrl;
    }

    @Override
    public boolean shouldPreserveHostHeader() {
        return preserveHostHeader;
    }

    public String proxyHostHeader() {
        return proxyHostHeader;
    }
}
