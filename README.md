WireMock - a web service test double for all occasions
======================================================

[![Build Status](https://github.com/tomakehurst/wiremock/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/tomakehurst/wiremock/actions/workflows/build-and-test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.tomakehurst/wiremock-jre8.svg)](https://search.maven.org/artifact/com.github.tomakehurst/wiremock-jre8)


Key Features
------------
	
-	HTTP response stubbing, matchable on URL, header and body content patterns
-	Request verification
-	Runs in unit tests, as a standalone process or as a WAR app
-	Configurable via a fluent Java API, JSON files and JSON over HTTP
-	Record/playback of stubs
-	Fault injection
-	Per-request conditional proxying
-   Browser proxying for request inspection and replacement
-	Stateful behaviour simulation
-	Configurable response delays
 

Full documentation can be found at [wiremock.org](http://wiremock.org/ "wiremock.org")

Questions and Issues
--------------------
If you have a question about WireMock, or are experiencing a problem you're not sure is a bug please post a message to the 
[WireMock mailing list](https://groups.google.com/forum/#!forum/wiremock-user).

On the other hand if you're pretty certain you've found a bug please open an issue.

Contributing
------------
We welcome bug fixes and new features in the form of pull requests. If you'd like to contribute, please be mindful of the
following guidelines:
* All changes should include suitable tests, whether to demonstrate the bug or exercise and document the new feature.
* Please make one change per pull request.
* If the new feature is significantly large/complex/breaks existing behaviour, please first post a summary of your idea
on the mailing list to generate a discussion. This will avoid significant amounts of coding time spent on changes that ultimately get rejected.
* This project utilises the [Spotless Gradle Plugin](https://github.com/diffplug/spotless/tree/HEAD/plugin-gradle) to manage code style. You can
apply changes by running `./gradlew spotlessApply`, or by adding the following configuration as a global Gralde configuration to an initscript file
i.e. `~/.gradle/init.d/spotless.gradle`:

```gradle
allprojects {
  afterEvaluate {
    def spotless = tasks.findByName('spotlessApply')
    if (spotless) {
      tasks.withType(JavaCompile) {
        finalizedBy(spotless)
      }
    }
  }
}
```

Building WireMock locally
-------------------------
To run all of WireMock's tests:
```bash
./gradlew clean test
```

To build both JARs (thin and standalone):
```bash
./gradlew jar shadowJar 
```

The built JAR will be placed under ``build/libs``.

To publish both JARs to your local Maven repository:

```bash
./gradlew publishToMavenLocal
```

