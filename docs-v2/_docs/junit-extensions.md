---
layout: docs
title: 'The JUnit 4.x Rule'
toc_rank: 20
redirect_from: "/junit-rule.html"
description: The WireMock JUnit rule.
---

The JUnit rule provides a convenient way to include WireMock in your
test cases. It handles the lifecycle for you, starting the server before
each test method and stopping afterwards.

## Basic usage

To make WireMock available to your tests on its default port (8080):

```java
@Rule
public WireMockRule wireMockRule = new WireMockRule();
```

The rule's constructor can take an `Options` instance to override
various settings. An `Options` implementation can be created via the
`WireMockConfiguration.options()` builder:

```java
@Rule
public WireMockRule wireMockRule = new WireMockRule(options().port(8888).httpsPort(8889));
```

See [Configuration](/docs/configuration/) for details.

## Unmatched requests

The JUnit rule will verify that all requests received during the course of a test case are served by a configured stub, rather than the default 404. If any are not
a `VerificationException` is thrown, failing the test. This behaviour can be disabled by passing an extra constructor flag:

```java
@Rule
public WireMockRule wireMockRule = new WireMockRule(options().port(8888), false);
```

## Other @Rule configurations

With a bit more effort you can make the WireMock server continue to run
between test cases. This is easiest in JUnit 4.10:

```java
@ClassRule
@Rule
public static WireMockClassRule wireMockRule = new WireMockClassRule(8089);
```

Unfortunately JUnit 4.11 and above prohibits `@Rule` on static members so a
slightly more verbose form is required:

```java
@ClassRule
public static WireMockClassRule wireMockRule = new WireMockClassRule(8089);

@Rule
public WireMockClassRule instanceRule = wireMockRule;
```


## Accessing the stubbing and verification DSL from the rule

In addition to the static methods on the `WireMock` class, it is also
possible to configure stubs etc. via the rule object directly. There are
two advantages to this - 1) it's a bit faster as it avoids sending
commands over HTTP, and 2) if you want to mock multiple services you can
declare a rule per service but not have to create a client object for
each e.g.

```java
@Rule
public WireMockRule service1 = new WireMockRule(8081);

@Rule
public WireMockRule service2 = new WireMockRule(8082);

@Test
public void bothServicesDoStuff() {
    service1.stubFor(get(urlEqualTo("/blah")).....);
    service2.stubFor(post(urlEqualTo("/blap")).....);

    ...
}
```
