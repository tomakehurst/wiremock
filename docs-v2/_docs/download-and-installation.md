---
layout: docs
title: Download and Installation
toc_rank: 13
description: Getting WireMock into your project
---

WireMock is distributed in two flavours - a standard JAR containing just WireMock, and a standalone fat JAR containing
WireMock plus all its dependencies.

Most of the standalone JAR's dependencies are shaded i.e. they are hidden in alternative packages. This allows WireMock to be used in projects with
conflicting versions of its dependencies. The standalone JAR is also runnable (see [Running as a Standalone Process](/docs/running-standalone/)).

Additionally, versions of these JARs are distributed for both Java 7 and Java 8+. 

The Java 7 distribution is aimed primarily at Android developers and enterprise Java teams still using JRE7. Some of its
dependencies are not set to the latest versions e.g. Jetty 9.2.x is used, as this is the last minor version to retain Java 7 compatibility.

The Java 8+ build endeavours to track the latest version of all its major dependencies. This is usually the version you should choose by default.

## Maven dependencies 

Java 7:

```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock</artifactId>
    <version>{{ site.wiremock_version }}</version>
    <scope>test</scope>
</dependency>
```

Java 7 standalone:

```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>{{ site.wiremock_version }}</version>
    <scope>test</scope>
</dependency>
```

Java 8:

```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8</artifactId>
    <version>{{ site.wiremock_version }}</version>
    <scope>test</scope>
</dependency>
```

Java 8 standalone:

```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8-standalone</artifactId>
    <version>{{ site.wiremock_version }}</version>
    <scope>test</scope>
</dependency>
```

## Gradle dependencies

Java 7:

```groovy
testImplementation "com.github.tomakehurst:wiremock:{{ site.wiremock_version }}"
```

Java 7 standalone:

```groovy
testImplementation "com.github.tomakehurst:wiremock-standalone:{{ site.wiremock_version }}"
```

Java 8:

```groovy
testImplementation "com.github.tomakehurst:wiremock-jre8:{{ site.wiremock_version }}"
```

Java 8 standalone:

```groovy
testImplementation "com.github.tomakehurst:wiremock-jre8-standalone:{{ site.wiremock_version }}"
```

## Direct download

If you want to run WireMock as a standalone process you can [download the standalone JAR from
here](https://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-jre8-standalone/{{ site.wiremock_version }}/wiremock-jre8-standalone-{{ site.wiremock_version }}.jar).
