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
package com.github.tomakehurst.wiremock.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.admin.tasks.SaveMappingsTask;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import java.net.HttpURLConnection;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class SaveMappingsTaskTest {
  private Mockery context;
  private Admin mockAdmin;
  private Request mockRequest;

  private SaveMappingsTask saveMappingsTask;

  @Before
  public void setUp() {
    context = new Mockery();
    mockAdmin = context.mock(Admin.class);
    mockRequest = context.mock(Request.class);

    saveMappingsTask = new SaveMappingsTask();
  }

  @Test
  public void delegatesSavingMappingsToAdmin() {
    context.checking(
        new Expectations() {
          {
            oneOf(mockAdmin).saveMappings();
          }
        });

    saveMappingsTask.execute(mockAdmin, mockRequest, PathParams.empty());
  }

  @Test
  public void returnsOkResponse() {
    context.checking(
        new Expectations() {
          {
            oneOf(mockAdmin).saveMappings();
          }
        });

    ResponseDefinition response =
        saveMappingsTask.execute(mockAdmin, mockRequest, PathParams.empty());

    assertThat(response.getStatus(), is(HttpURLConnection.HTTP_OK));
  }
}
