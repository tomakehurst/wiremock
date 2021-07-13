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
package com.github.tomakehurst.wiremock.recording;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.*;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

public class SnapshotStubMappingBodyExtractor {
  private final FileSource fileSource;

  public SnapshotStubMappingBodyExtractor(FileSource fileSource) {
    this.fileSource = fileSource;
  }

  /**
   * Extracts body of the ResponseDefinition to a file written to the files source. Modifies the
   * ResponseDefinition to point to the file in-place.
   *
   * @param stubMapping Stub mapping to extract
   */
  public void extractInPlace(StubMapping stubMapping) {
    byte[] body = stubMapping.getResponse().getByteBody();
    HttpHeaders responseHeaders = stubMapping.getResponse().getHeaders();
    String extension =
        ContentTypes.determineFileExtension(
            stubMapping.getRequest().getUrl(),
            responseHeaders != null
                ? responseHeaders.getContentTypeHeader()
                : ContentTypeHeader.absent(),
            body);

    String bodyFileName = SafeNames.makeSafeFileName(stubMapping, extension);

    // used to prevent ambiguous method call error for withBody()
    String noStringBody = null;
    byte[] noByteBody = null;

    stubMapping.setResponse(
        ResponseDefinitionBuilder.like(stubMapping.getResponse())
            .withBodyFile(bodyFileName)
            .withBody(noStringBody)
            .withBody(noByteBody)
            .withBase64Body(null)
            .build());

    fileSource.writeBinaryFile(bodyFileName, body);
  }
}
