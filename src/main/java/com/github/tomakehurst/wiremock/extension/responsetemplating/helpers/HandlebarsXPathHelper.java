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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ListOrSingle;
import com.github.tomakehurst.wiremock.common.xml.*;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RenderCache;
import java.io.IOException;

/**
 * This class uses javax.xml.xpath.* for reading a xml via xPath so that the result can be used for
 * response templating.
 */
public class HandlebarsXPathHelper extends HandlebarsHelper<String> {

  @Override
  public Object apply(final String inputXml, final Options options) throws IOException {
    if (inputXml == null) {
      return "";
    }

    if (options.param(0, null) == null) {
      return handleError("The XPath expression cannot be empty");
    }

    final String xPathInput = options.param(0);

    XmlDocument xmlDocument;
    try {
      xmlDocument = getXmlDocument(inputXml, options);
    } catch (XmlException e) {
      return handleError(inputXml + " is not valid XML");
    }

    try {
      ListOrSingle<XmlNode> xmlNodes =
          getXmlNodes(getXPathPrefix() + xPathInput, xmlDocument, options);

      if (xmlNodes == null || xmlNodes.isEmpty()) {
        return "";
      }

      return xmlNodes;
    } catch (XPathException e) {
      return handleError(xPathInput + " is not a valid XPath expression", e);
    }
  }

  private ListOrSingle<XmlNode> getXmlNodes(
      String xPathExpression, XmlDocument doc, Options options) {
    RenderCache renderCache = getRenderCache(options);
    RenderCache.Key cacheKey = RenderCache.Key.keyFor(XmlDocument.class, xPathExpression, doc);
    ListOrSingle<XmlNode> nodes = renderCache.get(cacheKey);

    if (nodes == null) {
      nodes = doc.findNodes(xPathExpression);
      renderCache.put(cacheKey, nodes);
    }

    return nodes;
  }

  private XmlDocument getXmlDocument(String xml, Options options) {
    RenderCache renderCache = getRenderCache(options);
    RenderCache.Key cacheKey = RenderCache.Key.keyFor(XmlDocument.class, xml);
    XmlDocument document = renderCache.get(cacheKey);
    if (document == null) {
      document = Xml.parse(xml);
      renderCache.put(cacheKey, document);
    }

    return document;
  }

  /**
   * No prefix by default. It allows to extend this class with a specified prefix. Just overwrite
   * this method to do so.
   *
   * @return a prefix which will be applied before the specified xpath.
   */
  protected String getXPathPrefix() {
    return "";
  }
}
