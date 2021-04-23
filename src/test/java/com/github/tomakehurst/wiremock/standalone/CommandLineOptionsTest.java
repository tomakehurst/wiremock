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
package com.github.tomakehurst.wiremock.standalone;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.ssl.KeyStoreSettings;
import com.github.tomakehurst.wiremock.common.ProxySettings;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.CaseInsensitiveKey;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.http.trafficlistener.ConsoleNotifyingWiremockNetworkTrafficListener;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import com.github.tomakehurst.wiremock.security.Authenticator;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static com.github.tomakehurst.wiremock.common.BrowserProxySettings.DEFAULT_CA_KESTORE_PASSWORD;
import static com.github.tomakehurst.wiremock.common.BrowserProxySettings.DEFAULT_CA_KEYSTORE_PATH;
import static com.github.tomakehurst.wiremock.matching.MockRequest.mockRequest;
import static com.github.tomakehurst.wiremock.testsupport.WireMatchers.matchesMultiLine;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class CommandLineOptionsTest {

	@Test
	public void returnsVerboseTrueWhenOptionPresent() {
		CommandLineOptions options = new CommandLineOptions("--verbose");
		assertThat(options.verboseLoggingEnabled(), is(true));
	}
	
	@Test
	public void returnsVerboseFalseWhenOptionNotPresent() {
		CommandLineOptions options = new CommandLineOptions("");
		assertThat(options.verboseLoggingEnabled(), is(false));
	}

	@Test
	public void returnsRecordMappingsTrueWhenOptionPresent() {
		CommandLineOptions options = new CommandLineOptions("--record-mappings");
		assertThat(options.recordMappingsEnabled(), is(true));
	}

    @Test
    public void returnsHeaderMatchingEnabledWhenOptionPresent() {
    	CommandLineOptions options =  new CommandLineOptions("--match-headers", "Accept,Content-Type");
    	assertThat(options.matchingHeaders(),
                hasItems(CaseInsensitiveKey.from("Accept"), CaseInsensitiveKey.from("Content-Type")));
    }

	@Test
	public void returnsRecordMappingsFalseWhenOptionNotPresent() {
		CommandLineOptions options = new CommandLineOptions("");
		assertThat(options.recordMappingsEnabled(), is(false));
	}

	@Test
     public void setsPortNumberWhenOptionPresent() {
        CommandLineOptions options = new CommandLineOptions("--port", "8086");
        assertThat(options.portNumber(), is(8086));
    }

    @Test
    public void disablesHttpWhenOptionPresentAndHttpsEnabled() {
        CommandLineOptions options = new CommandLineOptions("--disable-http",
                "--https-port", "8443");
        assertThat(options.getHttpDisabled(), is(true));
    }

    @Test
    public void enablesHttpsAndSetsPortNumberWhenOptionPresent() {
        CommandLineOptions options = new CommandLineOptions("--https-port", "8443");
        assertThat(options.httpsSettings().enabled(), is(true));
        assertThat(options.httpsSettings().port(), is(8443));
    }

    @Test
    public void defaultsKeystorePathIfNotSpecifiedWhenHttpsEnabled() {
        CommandLineOptions options = new CommandLineOptions("--https-port", "8443");
        assertThat(options.httpsSettings().keyStorePath(), endsWith("/keystore"));
    }

    @Test
    public void setsRequireClientCert() {
        CommandLineOptions options = new CommandLineOptions("--https-port", "8443",
                "--https-keystore", "/my/keystore",
                "--https-truststore", "/my/truststore",
                "--https-require-client-cert");
        assertThat(options.httpsSettings().needClientAuth(), is(true));
    }

    @Test
    public void setsTrustStoreOptions() {
        CommandLineOptions options = new CommandLineOptions(
                "--https-port", "8443",
                "--https-keystore", "/my/keystore",
                "--https-truststore", "/my/truststore",
                "--truststore-type", "PKCS12",
                "--truststore-password", "sometrustpwd");
        assertThat(options.httpsSettings().trustStorePath(), is("/my/truststore"));
        assertThat(options.httpsSettings().trustStoreType(), is("PKCS12"));
        assertThat(options.httpsSettings().trustStorePassword(), is("sometrustpwd"));
    }

    @Test
    public void setsHttpsKeyStorePathOptions() {
        CommandLineOptions options = new CommandLineOptions(
                "--https-port", "8443",
                "--https-keystore", "/my/keystore",
                "--keystore-type", "pkcs12",
                "--keystore-password", "someotherpwd",
                "--key-manager-password", "keymanpass"
        );
        assertThat(options.httpsSettings().keyStorePath(), is("/my/keystore"));
        assertThat(options.httpsSettings().keyStoreType(), is("pkcs12"));
        assertThat(options.httpsSettings().keyStorePassword(), is("someotherpwd"));
        assertThat(options.httpsSettings().keyManagerPassword(), is("keymanpass"));
    }

	@Test(expected=Exception.class)
	public void throwsExceptionWhenPortNumberSpecifiedWithoutNumber() {
		new CommandLineOptions("--port");
	}

    @Test
    public void returnsCorrecteyParsedBindAddress(){
        CommandLineOptions options = new CommandLineOptions("--bind-address", "127.0.0.1");
        assertThat(options.bindAddress(), is("127.0.0.1"));
    }

	@Test
	public void setsProxyAllRootWhenOptionPresent() {
		CommandLineOptions options = new CommandLineOptions("--proxy-all", "http://someotherhost.com/site");
		assertThat(options.specifiesProxyUrl(), is(true));
		assertThat(options.proxyUrl(), is("http://someotherhost.com/site"));
	}

    @Test
    public void setsProxyHostHeaderWithTrailingPortInformation() {
        CommandLineOptions options = new CommandLineOptions("--proxy-all", "http://someotherhost.com:8080/site");
        assertThat(options.proxyHostHeader(), is("someotherhost.com:8080"));
    }

    @Test(expected=Exception.class)
	public void throwsExceptionWhenProxyAllSpecifiedWithoutUrl() {
		new CommandLineOptions("--proxy-all");
	}

	@Test
	public void returnsBrowserProxyingEnabledWhenOptionSet() {
		CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying");
		assertThat(options.browserProxyingEnabled(), is(true));
		assertThat(options.browserProxySettings().enabled(), is(true));
	}

    @Test
    public void returnsBrowserProxyingDisabledWhenOptionNoSet() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.browserProxyingEnabled(), is(false));
        assertThat(options.browserProxySettings().enabled(), is(false));
    }

	@Test
	public void setsAll() {
		CommandLineOptions options = new CommandLineOptions("--verbose", "--record-mappings", "--port", "8088", "--proxy-all", "http://somewhere.com");
		assertThat(options.verboseLoggingEnabled(), is(true));
		assertThat(options.recordMappingsEnabled(), is(true));
		assertThat(options.portNumber(), is(8088));
		assertThat(options.specifiesProxyUrl(), is(true));
		assertThat(options.proxyUrl(), is("http://somewhere.com"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void returnsHelpText() {
		CommandLineOptions options = new CommandLineOptions("--help");
		assertThat(options.helpText(), allOf(containsString("verbose")));
	}

    @Test
    public void returnsCorrectlyParsedProxyViaParameter() {
        CommandLineOptions options = new CommandLineOptions("--proxy-via", "somehost.mysite.com:8080");
        assertThat(options.proxyVia().host(), is("somehost.mysite.com"));
        assertThat(options.proxyVia().port(), is(8080));
        assertThat(options.proxyVia().getUsername(), isEmptyOrNullString());
        assertThat(options.proxyVia().getPassword(), isEmptyOrNullString());
    }

    @Test
    public void returnsCorrectlyParsedProxyViaParameterWithCredentials() {
        CommandLineOptions options = new CommandLineOptions("--proxy-via", "user:password@somehost.mysite.com:8080");
        assertThat(options.proxyVia().host(), is("somehost.mysite.com"));
        assertThat(options.proxyVia().port(), is(8080));
        assertThat(options.proxyVia().getUsername(), is("user"));
        assertThat(options.proxyVia().getPassword(), is("password"));
    }


    @Test
    public void returnsNoProxyWhenNoProxyViaSpecified() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.proxyVia(), is(ProxySettings.NO_PROXY));
    }

    @Test
    public void returnsDisabledRequestJournal() {
        CommandLineOptions options = new CommandLineOptions("--no-request-journal");
        assertThat(options.requestJournalDisabled(), is(true));
    }

    @Test
    public void returnsMaxRequestJournalEntries() {
        CommandLineOptions options = new CommandLineOptions("--max-request-journal-entries", "2");
        assertThat(options.maxRequestJournalEntries(), is(Optional.of(2)));
        CommandLineOptions optionsNoMax = new CommandLineOptions("");
        assertThat(optionsNoMax.maxRequestJournalEntries().isPresent(), is(false));
    }

    @Test
    public void returnPreserveHostHeaderTrueWhenPresent() {
        CommandLineOptions options = new CommandLineOptions("--preserve-host-header");
        assertThat(options.shouldPreserveHostHeader(), is(true));
    }

    @Test
    public void returnPreserveHostHeaderFalseWhenNotPresent() {
        CommandLineOptions options = new CommandLineOptions("--port", "8080");
        assertThat(options.shouldPreserveHostHeader(), is(false));
    }

    @Test
    public void returnsCorrectlyParsedNumberOfThreads() {
        CommandLineOptions options = new CommandLineOptions("--container-threads", "300");
        assertThat(options.containerThreads(), is(300));
    }

    @Test
    public void defaultsContainerThreadsTo25() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.containerThreads(), is(25));
    }

    @Test
    public void returnsCorrectlyParsedJettyAcceptorThreads() {
        CommandLineOptions options = new CommandLineOptions("--jetty-acceptor-threads", "400");
        assertThat(options.jettySettings().getAcceptors().get(), is(400));
    }

    @Test
    public void returnsCorrectlyParsedJettyAcceptQueueSize() {
        CommandLineOptions options = new CommandLineOptions("--jetty-accept-queue-size", "10");
        assertThat(options.jettySettings().getAcceptQueueSize().get(), is(10));
    }

    @Test
    @Deprecated
    public void returnsCorrectlyParsedJettyHeaderBufferSize() {
        CommandLineOptions options = new CommandLineOptions("--jetty-header-buffer-size", "16384");
        assertThat(options.jettySettings().getRequestHeaderSize().get(), is(16384));
    }

    @Test
    public void returnsCorrectlyParsedJettyHeaderRequestSize() {
        CommandLineOptions options = new CommandLineOptions("--jetty-header-request-size", "16384");
        assertThat(options.jettySettings().getRequestHeaderSize().get(), is(16384));
    }

    @Test
    public void returnsCorrectlyParsedJettyHeaderResponseSize() {
        CommandLineOptions options = new CommandLineOptions("--jetty-header-response-size", "16384");
        assertThat(options.jettySettings().getResponseHeaderSize().get(), is(16384));
    }

    @Test
    public void returnsCorrectlyParsedJettyStopTimeout() {
        CommandLineOptions options = new CommandLineOptions("--jetty-stop-timeout", "1000");
        assertThat(options.jettySettings().getStopTimeout().get(), is(1000L));
    }

    @Test
    public void returnsAbsentIfJettyAcceptQueueSizeNotSet() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.jettySettings().getAcceptQueueSize().isPresent(), is(false));
    }

    @Test
    public void returnsAbsentIfJettyAcceptorsNotSet() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.jettySettings().getAcceptors().isPresent(), is(false));
    }

    @Test
    public void returnsAbsentIfJettyHeaderBufferSizeNotSet() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.jettySettings().getRequestHeaderSize().isPresent(), is(false));
    }

    @Test
    public void returnsAbsentIfJettyStopTimeoutNotSet() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.jettySettings().getStopTimeout().isPresent(), is(false));
    }

    @Test(expected=IllegalArgumentException.class)
    public void preventsRecordingWhenRequestJournalDisabled() {
        new CommandLineOptions("--no-request-journal", "--record-mappings");
    }

    @Test
    public void returnsExtensionsSpecifiedAsClassNames() {
        CommandLineOptions options = new CommandLineOptions(
                "--extensions",
                "com.github.tomakehurst.wiremock.standalone.CommandLineOptionsTest$ResponseDefinitionTransformerExt1,com.github.tomakehurst.wiremock.standalone.CommandLineOptionsTest$ResponseDefinitionTransformerExt2,com.github.tomakehurst.wiremock.standalone.CommandLineOptionsTest$RequestExt1");
        Map<String, ResponseDefinitionTransformer> extensions = options.extensionsOfType(ResponseDefinitionTransformer.class);
        assertThat(extensions.entrySet(), hasSize(2));
        assertThat(extensions.get("ResponseDefinitionTransformer_One"), instanceOf(ResponseDefinitionTransformerExt1.class));
        assertThat(extensions.get("ResponseDefinitionTransformer_Two"), instanceOf(ResponseDefinitionTransformerExt2.class));
    }

    @Test
    public void returnsRequestMatcherExtensionsSpecifiedAsClassNames() {
        CommandLineOptions options = new CommandLineOptions(
                        "--extensions",
                        "com.github.tomakehurst.wiremock.standalone.CommandLineOptionsTest$RequestExt1,com.github.tomakehurst.wiremock.standalone.CommandLineOptionsTest$ResponseDefinitionTransformerExt1");
        Map<String, RequestMatcherExtension> extensions = options.extensionsOfType(RequestMatcherExtension.class);
        assertThat(extensions.entrySet(), hasSize(1));
        assertThat(extensions.get("RequestMatcherExtension_One"), instanceOf(RequestExt1.class));
    }

    @Test
    public void returnsEmptySetForNoExtensionsSpecifiedAsClassNames() {
        CommandLineOptions options = new CommandLineOptions();
        Map<String, RequestMatcherExtension> extensions = options.extensionsOfType(RequestMatcherExtension.class);
        assertThat(extensions.entrySet(), hasSize(0));
    }

    @Test
    public void returnsAConsoleNotifyingListenerWhenOptionPresent() {
        CommandLineOptions options = new CommandLineOptions("--print-all-network-traffic");
        assertThat(options.networkTrafficListener(), is(instanceOf(ConsoleNotifyingWiremockNetworkTrafficListener.class)));
    }

    @Test
    public void enablesGlobalResponseTemplating() {
        CommandLineOptions options = new CommandLineOptions("--global-response-templating");
        Map<String, ResponseTemplateTransformer> extensions = options.extensionsOfType(ResponseTemplateTransformer.class);
        assertThat(extensions.entrySet(), hasSize(1));
        assertThat(extensions.get("response-template").applyGlobally(), is(true));
    }

    @Test
    public void enablesLocalResponseTemplating() {
        CommandLineOptions options = new CommandLineOptions("--local-response-templating");
        Map<String, ResponseTemplateTransformer> extensions = options.extensionsOfType(ResponseTemplateTransformer.class);
        assertThat(extensions.entrySet(), hasSize(1));
        assertThat(extensions.get("response-template").applyGlobally(), is(false));
    }

    @Test
    public void supportsAdminApiBasicAuth() {
        CommandLineOptions options = new CommandLineOptions("--admin-api-basic-auth", "user:pass");
        Authenticator authenticator = options.getAdminAuthenticator();

        String correctAuthHeader = new BasicCredentials("user", "pass").asAuthorizationHeaderValue();
        String incorrectAuthHeader = new BasicCredentials("user", "wrong_pass").asAuthorizationHeaderValue();
        assertThat(authenticator.authenticate(mockRequest().header("Authorization", correctAuthHeader)), is(true));
        assertThat(authenticator.authenticate(mockRequest().header("Authorization", incorrectAuthHeader)), is(false));
    }

    @Test
    public void canRequireHttpsForAdminApi() {
        CommandLineOptions options = new CommandLineOptions("--admin-api-require-https");
        assertThat(options.getHttpsRequiredForAdminApi(), is(true));
    }

    @Test
    public void defaultsToNotRequiringHttpsForAdminApi() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.getHttpsRequiredForAdminApi(), is(false));
    }

    @Test
    public void enablesAsynchronousResponse() {
        CommandLineOptions options = new CommandLineOptions("--async-response-enabled", "true");
        assertThat(options.getAsynchronousResponseSettings().isEnabled(), is(true));
    }

    @Test
    public void disablesAsynchronousResponseByDefault() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.getAsynchronousResponseSettings().isEnabled(), is(false));
    }

    @Test
    public void setsNumberOfAsynchronousResponseThreads() {
        CommandLineOptions options = new CommandLineOptions("--async-response-threads", "20");
        assertThat(options.getAsynchronousResponseSettings().getThreads(), is(20));
    }

    @Test
    public void setsChunkedEncodingPolicy() {
        CommandLineOptions options = new CommandLineOptions("--use-chunked-encoding", "always");
        assertThat(options.getChunkedEncodingPolicy(), is(Options.ChunkedEncodingPolicy.ALWAYS));
    }

    @Test
    public void setsDefaultNumberOfAsynchronousResponseThreads() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.getAsynchronousResponseSettings().getThreads(), is(10));
    }

    @Test
    public void configuresMaxTemplateCacheEntriesIfSpecified() {
        CommandLineOptions options = new CommandLineOptions("--global-response-templating", "--max-template-cache-entries", "5");
        Map<String, ResponseTemplateTransformer> extensions = options.extensionsOfType(ResponseTemplateTransformer.class);
        ResponseTemplateTransformer transformer = extensions.get(ResponseTemplateTransformer.NAME);

        assertThat(transformer.getMaxCacheEntries(), is(5L));
    }

    @Test
    public void configuresMaxTemplateCacheEntriesToNullIfNotSpecified() {
        CommandLineOptions options = new CommandLineOptions("--global-response-templating");
        Map<String, ResponseTemplateTransformer> extensions = options.extensionsOfType(ResponseTemplateTransformer.class);
        ResponseTemplateTransformer transformer = extensions.get(ResponseTemplateTransformer.NAME);

        assertThat(transformer.getMaxCacheEntries(), nullValue());
    }

    @Test
    public void configuresPermittedSystemKeysIfSpecified() {
        CommandLineOptions options = new CommandLineOptions("--global-response-templating", "--permitted-system-keys", "java*,path*");
        assertThat(options.getPermittedSystemKeys(), hasItems("java*", "path*"));
    }

    @Test
    public void returnsEmptyPermittedKeysIfNotSpecified() {
        CommandLineOptions options = new CommandLineOptions("--global-response-templating");
        assertThat(options.getPermittedSystemKeys(), emptyCollectionOf(String.class));
    }

    @Test
    public void disablesGzip() {
        CommandLineOptions options = new CommandLineOptions("--disable-gzip");
        assertThat(options.getGzipDisabled(), is(true));
    }

    @Test
    public void defaultsToGzipEnabled() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.getGzipDisabled(), is(false));
    }

    @Test
    public void disablesRequestLogging() {
	    CommandLineOptions options = new CommandLineOptions("--disable-request-logging");
	    assertThat(options.getStubRequestLoggingDisabled(), is(true));
    }

    @Test
    public void defaultsToRequestLoggingEnabled() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.getStubRequestLoggingDisabled(), is(false));
    }

    @Test
    public void printsTheActualPortOnlyWhenHttpsDisabled() {
	    CommandLineOptions options = new CommandLineOptions();
	    options.setActualHttpPort(5432);

	    String dump = options.toString();

	    assertThat(dump, matchesMultiLine(".*port:.*5432.*"));
	    assertThat(dump, not(containsString("https-port")));
    }

    @Test
    public void enablesStubCors() {
        CommandLineOptions options = new CommandLineOptions("--enable-stub-cors");
        assertThat(options.getStubCorsEnabled(), is(true));
    }

    @Test
    public void defaultsToNoStubCors() {
        CommandLineOptions options = new CommandLineOptions();
        assertThat(options.getStubCorsEnabled(), is(false));
    }

    @Test
    public void trustAllProxyTargets() {
        CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying", "--trust-all-proxy-targets");
        assertThat(options.browserProxySettings().trustAllProxyTargets(), is(true));
    }

    @Test
    public void defaultsToNotTrustingAllProxyTargets() {
        CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying");
        assertThat(options.browserProxySettings().trustAllProxyTargets(), is(false));
    }

    @Test
    public void trustsOneProxyTarget1() {
        CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying", "--trust-proxy-target", "localhost");
        assertThat(options.browserProxySettings().trustedProxyTargets(), is(singletonList("localhost")));
    }

    @Test
    public void trustsManyProxyTargets() {
        CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying", "--trust-proxy-target=localhost", "--trust-proxy-target", "wiremock.org", "--trust-proxy-target=www.google.com");
        assertThat(options.browserProxySettings().trustedProxyTargets(), is(asList("localhost", "wiremock.org", "www.google.com")));
    }

    @Test
    public void defaultsToNoTrustedProxyTargets() {
        CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying");
        assertThat(options.browserProxySettings().trustedProxyTargets(), is(Collections.<String>emptyList()));
    }

    @Test
    public void setsCaKeyStorePathAndPassword() {
        CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying", "--ca-keystore", "/my/keystore", "--ca-keystore-password", "someotherpwd", "--ca-keystore-type", "pkcs12");
        KeyStoreSettings caKeyStore = options.browserProxySettings().caKeyStore();
        assertThat(caKeyStore.path(), is("/my/keystore"));
        assertThat(caKeyStore.password(), is("someotherpwd"));
        assertThat(caKeyStore.type(), is("pkcs12"));
    }

    @Test
    public void defaultsCaKeyStorePathAndPassword() {
        CommandLineOptions options = new CommandLineOptions("--enable-browser-proxying");
        KeyStoreSettings caKeyStore = options.browserProxySettings().caKeyStore();
        assertThat(caKeyStore.path(), is(DEFAULT_CA_KEYSTORE_PATH));
        assertThat(caKeyStore.password(), is(DEFAULT_CA_KESTORE_PASSWORD));
        assertThat(caKeyStore.type(), is("jks"));
    }

    @Test
    public void printsBothActualPortsOnlyWhenHttpsEnabled() {
	    CommandLineOptions options = new CommandLineOptions();
	    options.setActualHttpPort(5432);
	    options.setActualHttpsPort(2345);

	    String dump = options.toString();

	    assertThat(dump, matchesMultiLine(".*port:.*5432.*"));
	    assertThat(dump, matchesMultiLine(".*https-port:.*2345.*"));
    }

    @Test
    public void toStringWithTrustAllProxyTargetsWorks() {
        String options = new CommandLineOptions("--enable-browser-proxying", "--trust-all-proxy-targets").toString();
        assertThat(options, matchesMultiLine(".*enable-browser-proxying: *true.*"));
        assertThat(options, matchesMultiLine(".*trust-all-proxy-targets: *true.*"));
    }

    @Test
    public void toStringWithTrustProxyTarget() {
        String options = new CommandLineOptions("--enable-browser-proxying", "--trust-proxy-target", "localhost", "--trust-proxy-target", "example.com").toString();
        assertThat(options, matchesMultiLine(".*enable-browser-proxying: *true.*"));
        assertThat(options, matchesMultiLine(".*trust-proxy-target: *localhost, example\\.com.*"));
    }

    public static class ResponseDefinitionTransformerExt1 extends ResponseDefinitionTransformer {
        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) { return null; }

        @Override
        public String getName() { return "ResponseDefinitionTransformer_One"; }
    }

    public static class ResponseDefinitionTransformerExt2 extends ResponseDefinitionTransformer {
        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) { return null; }

        @Override
        public String getName() { return "ResponseDefinitionTransformer_Two"; }
    }

    public static class RequestExt1 extends RequestMatcherExtension {

        @Override
        public MatchResult match(Request request, Parameters parameters) {
            return MatchResult.noMatch();
        }

        @Override
        public String getName() { return "RequestMatcherExtension_One"; }
    }
}
