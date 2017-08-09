---
layout: docs
title: Running as a Standalone Process
toc_rank: 41
redirect_from: "/running-standalone.html"
description: Running WireMock as a standalone mock server.
---

The WireMock server can be run in its own process, and configured via
the Java API, JSON over HTTP or JSON files.

Once you have [downloaded the standalone JAR](http://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-standalone/{{ site.wiremock_version }}/wiremock-standalone-{{ site.wiremock_version }}.jar) you can run it simply by doing this:

```bash
$ java -jar wiremock-standalone-{{ site.wiremock_version }}.jar
```

## Command line options

The following can optionally be specified on the command line:

`--port`: Set the HTTP port number e.g. `--port 9999`

`--https-port`: If specified, enables HTTPS on the supplied port.

`--bind-address`: The IP address the WireMock server should serve from. Binds to all local network adapters if unspecified.

`--https-keystore`: Path to a keystore file containing an SSL
certificate to use with HTTPS. The keystore must have a password of
"password". This option will only work if `--https-port` is specified.
If this option isn't used WireMock will default to its own self-signed
certificate.

`--keystore-password`: Password to the keystore, if something other than
"password".

`--https-truststore`: Path to a keystore file containing client
certificates. See https and proxy-client-certs for details.

`--truststore-password`: Optional password to the trust store. Defaults
to "password" if not specified.

`--https-require-client-cert`: Force clients to authenticate with a
client certificate. See https for details.

`--verbose`: Turn on verbose logging to stdout

`--root-dir`: Sets the root directory, under which `mappings` and
`__files` reside. This defaults to the current directory.

`--record-mappings`: Record incoming requests as stub mappings. See
record-playback.

`--match-headers`: When in record mode, capture request headers with the
keys specified. See record-playback.

`--proxy-all`: Proxy all requests through to another base URL e.g.
`--proxy-all="http://api.someservice.com"` Typically used in conjunction
with `--record-mappings` such that a session on another service can be
recorded.

`--preserve-host-header`: When in proxy mode, it passes the Host header
as it comes from the client through to the proxied service. When this
option is not present, the Host header value is deducted from the proxy
URL. This option is only available if the `--proxy-all` option is
specified.

`--proxy-via`: When proxying requests (either by using --proxy-all or by
creating stub mappings that proxy to other hosts), route via another
proxy server (useful when inside a corporate network that only permits
internet access via an opaque proxy). e.g.
`--proxy-via webproxy.mycorp.com` (defaults to port 80) or
`--proxy-via webproxy.mycorp.com:8080`

`--enable-browser-proxying`: Run as a browser proxy. See
browser-proxying.

`--no-request-journal`: Disable the request journal, which records
incoming requests for later verification. This allows WireMock to be run
(and serve stubs) for long periods (without resetting) without
exhausting the heap. The `--record-mappings` option isn't available if
this one is specified.

`--container-threads`: The number of threads created for incoming
requests. Defaults to 14.

`--max-request-journal-entries`: Set maximum number of entries in
request journal (if enabled). When this limit is reached oldest entries
will be discarded.

`--jetty-acceptor-threads`: The number of threads Jetty uses for
accepting requests.

`--jetty-accept-queue-size`: The Jetty queue size for accepted requests.

`--jetty-header-buffer-size`: The Jetty buffer size for request headers,
e.g. `--jetty-header-buffer-size 16384`, defaults to 8192K.

`--extensions`: Extension class names e.g.
com.mycorp.HeaderTransformer,com.mycorp.BodyTransformer. See extending-wiremock.

`--print-all-network-traffic`: Print all raw incoming and outgoing network traffic to console.

`--global-response-templating`: Render all response definitions using Handlebars templates.
`--local-response-templating`: Enable rendering of response definitions using Handlebars templates for specific stub mappings.

`--help`: Show command line help

## Configuring WireMock using the Java client

The WireMock Java API can be used against a running server on a different host if required. If you're only planning to configure a single remote instance from within your program you can configure the static DSL to point to it:

```java
WireMock.configureFor("my.remote.host", 8000);

// or for HTTPS
WireMock.configureFor("https", "my.remote.host", 8443);
```

Alternatively you can create an instance of the client (or as many as there are servers to configure):

```java
WireMock wireMock1 = new WireMock("1st.remote.host", 8000);
WireMock wireMock2 = new WireMock("https", "2nd.remote.host", 8001);
```

## Configuring via JSON over HTTP

You can create a stub mapping by posting to WireMock's HTTP API:

```bash
$ curl -X POST \
--data '{ "request": { "url": "/get/this", "method": "GET" }, "response": { "status": 200, "body": "Here it is!\n" }}' \
http://localhost:8080/__admin/mappings/new
```

And then fetch it back:

```bash
$ curl http://localhost:8080/get/this
Here it is!
```

The full stubbing API syntax is described in [Stubbing](/docs/stubbing/).

## JSON file configuration

You can also use the JSON API via files. When the WireMock server starts
it creates two directories under the current one: `mappings` and
`__files`.

To create a stub like the one above by this method, drop a file with a
`.json` extension under `mappings` with the following content:

```json
{
    "request": {
        "method": "GET",
        "url": "/api/mytest"
    },
    "response": {
        "status": 200,
        "body": "More content\n"
    }
}
```

After restarting the server you should be able to do this:

```bash
$ curl http://localhost:8080/api/mytest
More content
```

See [stubbing](/docs/stubbing/) and [verifying](/docs/verifying/) for more on the JSON API.


## Pushing JSON files to a remote WireMock instance
You can push a collection of mappings to a remote  


## File serving

When running the standalone JAR, files placed under the `__files` directory will
be served up as if from under the docroot, except if stub mapping
matching the URL exists. For example if a file exists
`__files/things/myfile.html` and no stub mapping will match
`/things/myfile.html` then hitting
`http://<host>:<port>/things/myfile.html` will serve the file.

### Shutting Down

To shutdown the server, either call `WireMock.shutdownServer()` or post
a request with an empty body to `http://<host>:<port>/__admin/shutdown`.
