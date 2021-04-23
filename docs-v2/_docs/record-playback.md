---
layout: docs
title: Record and Playback (New)
toc_rank: 70
redirect_from: "/record-playback.html"
description: Recording HTTP exchanges with other APIs and playing them back as stubs - the new way.
---

<div class="mocklab-callout"> 
  <p class="mocklab-callout__text">
    If you want to record stubs and host them in the cloud with minimum hassle, try <strong>MockLab</strong>. It provides a hosted, 100% WireMock compatible mocking service with a friendly web UI.    
  </p>
  <a href="http://get.mocklab.io/?utm_source=wiremock.org&utm_medium=docs-callout&utm_campaign=record-and-playback" title="Learn more" class="mocklab-callout__learn-more-button">Learn more</a>
</div>

WireMock can create stub mappings from requests it has received. Combined with its proxying feature this allows you to "record"
stub mappings from interaction with existing APIs.

Two approaches are available: Recording or snapshotting. The same results can be achieved with either, so which you choose should depend on whatever
fits best with your workflow. If you're new to WireMock, recording is probably the simplest option for getting started.

Both approaches are described in more detail below.

## Quick start

The fastest way to get started with WireMock's recorder is to use the simple web UI provided.

First, start an instance of [WireMock running standalone](/docs/running-standalone).
Once that's running visit the recorder UI page at [http://localhost:8080/__admin/recorder](http://localhost:8080/__admin/recorder)
(assuming you started WireMock on the default port of 8080).

![Recorder UI]({{ base_path }}/images/recorder-screenshot.png)

Enter the URL you wish to record from in the target URL field and click the Record button. You can use `http://example.mocklab.io` to try it out.

Now you need to make a request through WireMock to the target API so that it can be recorded. If you're using the example URL, you can generate a request using curl:
 
```bash
$ curl http://localhost:8080/recordables/123
```

Now click stop. You should see a message indicating that one stub was captured.

You should also see that a file has been created called something like `recordables_123-40a93c4a-d378-4e07-8321-6158d5dbcb29.json`
under the `mappings` directory created when WireMock started up, and that a new mapping has appeared at [http://localhost:8080/__admin/mappings](http://localhost:8080/__admin/mappings).

Requesting the same URL again (possibly disabling your wifi first if you want firm proof) will now serve the recorded result:

```
$ curl http://localhost:8080/recordables/123

{
  "message": "Congratulations on your first recording!"
}
```

> **note**
>
> Stub mappings will only be created at the point that the recording is stopped.

> **note**
>
> "Playback" doesn't require any explicit action. Recorded stubs will start being served immediately after recording is stopped.


## Recording

Recording can also be started and stopped via WireMock's JSON API and Java DSL.

Java:

```java
// Static DSL
WireMock.startRecording("http://example.mocklab.io");
List<StubMapping> recordedMappings = WireMock.stopRecording();

// Client instance
WireMock wireMockClient = new WireMock(8080);
wireMockClient.startStubRecording("http://example.mocklab.io");
List<StubMapping> recordedMappings = wireMockClient.stopStubRecording();

// Directly
WireMockServer wireMockServer = new WireMockServer();
wireMockServer.start();
wireMockServer.startRecording("http://example.mocklab.io");
List<StubMapping> recordedMappings = wireMockServer.stopRecording();
```

API:

```json
POST /__admin/recordings/start
{
  "targetBaseUrl": "http://example.mocklab.io"
}
```

```
POST /__admin/recordings/stop
```


## Snapshotting

Snapshotting is effectively "recording after the fact". Rather than starting recording at a specific point, snapshotting allows you to convert requests already received by WireMock
into stub mappings.

An implication of this order of events is that if you want to record an external API, you'll need to have configured proxying before you start generating traffic.
See [Proxying](/docs/proxying) for details on proxy configuration, but in summary this can be achieved by creating a proxy mapping via the API or Java DSL:
  
Java:

```java
stubFor(proxyAllTo("http://example.mocklab.io").atPriority(1));
```

API:

```json
POST /__admin/mappings
{
    "priority": 1,
    "request": {
        "method": "ANY"
    },
    "response": {
        "proxyBaseUrl" : "http://example.mocklab.io"
    }
}
```

> **note**
>
> You can still take snapshots without a proxy stub configured.
> You might want to do this e.g. if you want to capture requests made by your application under test that you can then modify by hand to provide the appropriate responses.

Once you have made some requests through WireMock (which you can view under http://localhost:8080/__admin/requests) you can trigger a snapshot to generate stub mappings:
 
Java:

```java
// Static DSL
List<StubMapping> recordedMappings = WireMock.snapshotRecord();

// Client instance
WireMock wireMockClient = new WireMock(8080);
List<StubMapping> recordedMappings = wireMockClient.takeSnapshotRecording();

// Directly
WireMockServer wireMockServer = new WireMockServer();
wireMockServer.start();
List<StubMapping> recordedMappings = wireMockServer.snapshotRecord();
```

API:

```
POST /__admin/recordings/snapshot
{}
```

## Customising your recordings

The default recording behaviour can be tweaked in a number of ways by passing a "record spec" to the record or snapshot actions.
 
In Java this achieved using the DSL:

```java
startRecording(
      recordSpec()
          .forTarget("http://example.mocklab.io")
          .onlyRequestsMatching(getRequestedFor(urlPathMatching("/api/.*")))
          .captureHeader("Accept")
          .captureHeader("Content-Type", true)
          .extractBinaryBodiesOver(10240)
          .extractTextBodiesOver(2048)
          .makeStubsPersistent(false)
          .ignoreRepeatRequests()
          .transformers("modify-response-header")
          .transformerParameters(Parameters.one("headerValue", "123"))
          .matchRequestBodyWithEqualToJson(false, true)
  );
```

And via the API:

```json
POST /__admin/recordings/start
{
  "targetBaseUrl" : "http://example.mocklab.io",
  "filters" : {
    "urlPathPattern" : "/api/.*",
    "method" : "GET",
    "allowNonProxied": true
  },
  "captureHeaders" : {
    "Accept" : { },
    "Content-Type" : {
      "caseInsensitive" : true
    }
  },
  "requestBodyPattern" : {
    "matcher" : "equalToJson",
    "ignoreArrayOrder" : false,
    "ignoreExtraElements" : true
  },
  "extractBodyCriteria" : {
    "textSizeThreshold" : "2048",
    "binarySizeThreshold" : "10240"
  },
  "persist" : false,
  "repeatsAsScenarios" : false,
  "transformers" : [ "modify-response-header" ],
  "transformerParameters" : {
    "headerValue" : "123"
  }
}
```

The same specification can also be passed when snapshotting:

Java:

```java
snapshotRecord(
      recordSpec()
          .onlyRequestsMatching(getRequestedFor(urlPathMatching("/api/.*")))
          .onlyRequestIds(singletonList(UUID.fromString("40a93c4a-d378-4e07-8321-6158d5dbcb29")))
          .allowNonProxied(true)
          .captureHeader("Accept")
          .captureHeader("Content-Type", true)
          .extractBinaryBodiesOver(10240)
          .extractTextBodiesOver(2048)
          .makeStubsPersistent(false)
          .ignoreRepeatRequests()
          .transformers("modify-response-header")
          .transformerParameters(Parameters.one("headerValue", "123"))
          .chooseBodyMatchTypeAutomatically()
  );
```

API:

```json
POST /__admin/recordings/snapshot
{
  "filters" : {
    "urlPathPattern" : "/api/.*",
    "method" : "GET",
    "ids" : [ "40a93c4a-d378-4e07-8321-6158d5dbcb29" ]
  },
  "captureHeaders" : {
    "Accept" : { },
    "Content-Type" : {
      "caseInsensitive" : true
    }
  },
  "requestBodyPattern" : {
    "matcher" : "equalToJson",
    "ignoreArrayOrder" : false,
    "ignoreExtraElements" : true
  },
  "extractBodyCriteria" : {
    "textSizeThreshold" : "2 kb",
    "binarySizeThreshold" : "1 Mb"
  },
  "outputFormat" : "FULL",
  "persist" : false,
  "repeatsAsScenarios" : false,
  "transformers" : [ "modify-response-header" ],
  "transformerParameters" : {
    "headerValue" : "123"
  }
}
```

The following sections will detail each parameter in turn:

### Filtering

`filters` supports selection of requests to be recorded according to the same [request matcher](/docs/request-matching) format used elsewhere in WireMock.

Additionally, when snapshotting the `ids` parameter allows specific serve events to be selected by ID.

The `allowNonProxied` attribute, when set to `true` will cause requests that did not get proxied to a target service to be recorded/snapshotted. This is useful if
you wish to "teach" WireMock your API by feeding it requests from your app that initially don't match a stub, then snapshotting to generate the correct stubs.  
 

### Capturing request headers

You may want your recorded stub mappings to match on one or more specific request headers.
For instance if you're intending to record from an API that supports both XML and JSON responses via content negotiation,
then you will need to capture the value of the `Accept` header sent in each request.

The `captureHeaders` attribute allows you to specify a map of header names to match parameter objects. Currently the only parameter
available is `caseInsensitive`, which defaults to false if absent. 

 
### Body files extraction size criteria

By default, recorded response bodies will be included directly in the stub mapping response part, via the `body` attribute for text or `base64Body` for binary content.

However, this can be overridden by setting the `textSizeThreshold` and `binarySizeThreshold` values under `extractBodyCriteria`.
The size values are of type string, and support friendly syntax for specifying the order of magnitude e.g.
 
```
"56 kb"
"10 Mb"
"18.2 GB"
"255" // bytes when no magnitude specified
```

In the Java DSL these values are specified as a `long` number of bytes:

```java
recordSpec().extractBinaryBodiesOver(204800)
```

### Output format

By default the stop recording and snapshot API calls will return the full JSON of all mappings captured.
If you only require the IDs of captured stubs you can specify:

```json
{
  "outputFormat": "IDS"
}
```

### Persist stubs

By default generated stubs will be set to persistent, meaning that they will be saved to the file system
(or other back-end if you've implemented your own `MappingsSource`) and will survive calls to reset mappings to default.

Setting `persist` to `false` means that stubs will not be saved and will be deleted on the next reset.


### Recorded File ID Method

By default each stub mapping is assigned a random 5 digit alphanumeric identifier.  However, if you want to have stable
file IDs, you can use the command line option `--file-id-method` when [running standalone](/docs/running-standalone) to
override this behavior.

You can use the [configuration API](/docs/configuration) `fileIdMethod` to set the file ID generation method when running programatically.  


### Repeats as scenarios

What happens when the recorder sees two identical requests that produce different results?

There are two ways to handle this. Setting `repeatsAsScenarios` to `false` means that after the first request, subsequent identical ones will be ignored.

However, when set to `true` (which is the default if omitted), multiple identical requests will be added to a [Scenario](/docs/stateful-behaviour), meaning that when
playing back, a series of requests matching this stub will yield the same series of responses captured during recording. If more requests are made after the end of the series
is reached, the last response will continue to be returned.


### Transforming generated stubs

If you need even more control over how your recorded stubs are generated, you can write one or more custom transformers that will be applied to stubs as they are captured.

A transformer is an implementations of `StubMappingTransformer` and needs to be registered when starting WireMock as described in [Extending WireMock](/docs/extending-wiremock).
   
Transformer implementations supply a name, and this is used to identify them in the `transformers` parameter e.g.

```json
"transformers": ["transformer-one", "transformer-two"]
```

As with other types of WireMock extension, parameters can be supplied. The exact parameters required depend on the specifics of the transformer (or it may not require any).

```json
"transformerParameters": {
    "simpleParam1": "One",
    "arrayParam2": [1, 2, 3],
    ...
}
```


### Request body matching

By default, the body match operator for a recorded stub is based on the `Content-Type` header of the request. For MIME types containing the string "json", the operator will be `equalToJson` with both the `ignoreArrayOrder` and `ignoreExtraElements` options set to `true`. For MIME types containing `xml`, it will use `equalToXml`. Otherwise, it will use `equalTo` with the `caseInsensitive` option set to `false`. 
 
 This behavior can be customized via the `requestBodyPattern` parameter, which accepts a `matcher` (either `equalTo`, `equalToJson`, `equalToXml`, or `auto`) and any relevant matcher options (`ignoreArrayOrder`, `ignoreExtraElements`, or `caseInsensitive`). For example, here's how to preserve the default behavior, but set `ignoreArrayOrder` to `false` when `equalToJson` is used:

```json
"requestBodyPattern" : {
    "matcher": "auto",
    "ignoreArrayOrder" : false
  }
```

If you want to always match request bodies with `equalTo` case-insensitively, regardless of the MIME type, use:
```json
"requestBodyPattern" : {
    "matcher": "equalTo",
    "caseInsensitive" : true
  }
```

> **note**
>
> The `targetBaseUrl` parameter will be ignored when snapshotting and the `filters/ids` parameter will be ignored when recording.


