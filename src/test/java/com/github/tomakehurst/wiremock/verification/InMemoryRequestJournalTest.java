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
package com.github.tomakehurst.wiremock.verification;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.matching.RequestPattern.everything;
import static com.github.tomakehurst.wiremock.testsupport.MockRequestBuilder.aRequest;
import static com.github.tomakehurst.wiremock.verification.LoggedRequest.createFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.base.Optional;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class InMemoryRequestJournalTest {

  private ServeEvent serveEvent1, serveEvent2, serveEvent3;

  @Before
  public void createTestRequests() {
    Mockery context = new Mockery();
    serveEvent1 =
        ServeEvent.of(createFrom(aRequest(context, "log1").withUrl("/logging1").build()), null);
    serveEvent2 =
        ServeEvent.of(createFrom(aRequest(context, "log2").withUrl("/logging2").build()), null);
    serveEvent3 =
        ServeEvent.of(createFrom(aRequest(context, "log3").withUrl("/logging3").build()), null);
  }

  @Test
  public void returnsAllLoggedRequestsWhenNoJournalSizeLimit() {
    RequestJournal journal = new InMemoryRequestJournal(Optional.<Integer>absent());

    journal.requestReceived(serveEvent1);
    journal.requestReceived(serveEvent1);
    journal.requestReceived(serveEvent2);

    assertThat(
        journal.countRequestsMatching(getRequestedFor(urlEqualTo("/logging1")).build()), is(2));
    assertThat(
        journal.countRequestsMatching(getRequestedFor(urlEqualTo("/logging2")).build()), is(1));
  }

  @Test
  public void resettingTheJournalClearsAllEntries() throws Exception {
    Mockery context = new Mockery();
    LoggedRequest loggedRequest = createFrom(aRequest(context).withUrl("/for/logging").build());

    RequestJournal journal = new InMemoryRequestJournal(Optional.of(1));
    journal.requestReceived(ServeEvent.of(loggedRequest, null));
    assertThat(journal.countRequestsMatching(everything()), is(1));
    journal.reset();
    assertThat(journal.countRequestsMatching(everything()), is(0));
  }

  @Test
  public void discardsOldRequestsWhenJournalSizeIsLimited() throws Exception {
    RequestJournal journal = new InMemoryRequestJournal(Optional.of(2));

    journal.requestReceived(serveEvent1);
    journal.requestReceived(serveEvent2);

    assertThat(journal.countRequestsMatching(everything()), is(2));
    assertThat(
        journal.countRequestsMatching(getRequestedFor(urlEqualTo("/logging1")).build()), is(1));
    assertThat(
        journal.countRequestsMatching(getRequestedFor(urlEqualTo("/logging2")).build()), is(1));

    journal.requestReceived(serveEvent3);
    assertOnlyLastTwoRequestsLeft(journal);
  }

  private void assertOnlyLastTwoRequestsLeft(RequestJournal journal) {
    assertThat(
        journal.countRequestsMatching(getRequestedFor(urlEqualTo("/logging1")).build()), is(0));
    assertThat(
        journal.countRequestsMatching(getRequestedFor(urlEqualTo("/logging2")).build()), is(1));
    assertThat(
        journal.countRequestsMatching(getRequestedFor(urlEqualTo("/logging3")).build()), is(1));
  }
}
