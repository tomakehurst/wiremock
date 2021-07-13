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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Sets.newTreeSet;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.tomakehurst.wiremock.common.ListOrSingle;
import com.github.tomakehurst.wiremock.common.Pair;
import com.github.tomakehurst.wiremock.common.xml.*;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;

@JsonSerialize(using = XPathPatternJsonSerializer.class)
public class MatchesXPathPattern extends PathPattern {

  private final Map<String, String> xpathNamespaces;

  public MatchesXPathPattern(String xpath) {
    this(xpath, null, null);
  }

  public MatchesXPathPattern(String xpath, StringValuePattern valuePattern) {
    this(xpath, null, valuePattern);
  }

  public MatchesXPathPattern(String xpath, Map<String, String> namespaces) {
    this(xpath, namespaces, null);
  }

  public MatchesXPathPattern(
      @JsonProperty("matchesXPath") String xpath,
      @JsonProperty("namespaces") Map<String, String> namespaces,
      @JsonProperty("valuePattern") StringValuePattern valuePattern) {
    super(xpath, valuePattern);
    xpathNamespaces = namespaces == null || namespaces.isEmpty() ? null : namespaces;
  }

  public MatchesXPathPattern withXPathNamespace(String name, String namespaceUri) {
    Map<String, String> namespaceMap =
        ImmutableMap.<String, String>builder()
            .putAll(firstNonNull(xpathNamespaces, Collections.<String, String>emptyMap()))
            .put(name, namespaceUri)
            .build();
    return new MatchesXPathPattern(expectedValue, namespaceMap);
  }

  public String getMatchesXPath() {
    return expectedValue;
  }

  @JsonGetter("xPathNamespaces")
  public Map<String, String> getXPathNamespaces() {
    return xpathNamespaces;
  }

  @Override
  protected MatchResult isSimpleMatch(String value) {
    ListOrSingle<XmlNode> nodeList = findXmlNodes(value);
    return MatchResult.of(nodeList != null && nodeList.size() > 0);
  }

  @Override
  protected MatchResult isAdvancedMatch(String value) {
    ListOrSingle<XmlNode> nodeList = findXmlNodes(value);
    if (nodeList == null || nodeList.size() == 0) {
      return MatchResult.noMatch();
    }

    SortedSet<MatchResult> results = newTreeSet();
    for (XmlNode node : nodeList) {
      results.add(valuePattern.match(node.toString()));
    }

    return results.last();
  }

  @Override
  public String getExpressionResult(String value) {
    ListOrSingle<XmlNode> nodeList = findXmlNodes(value);
    if (nodeList == null || nodeList.size() == 0) {
      return null;
    }

    SortedSet<Pair<XmlNode, MatchResult>> results =
        newTreeSet(
            new Comparator<Pair<XmlNode, MatchResult>>() {
              @Override
              public int compare(Pair<XmlNode, MatchResult> one, Pair<XmlNode, MatchResult> two) {
                return one.b.compareTo(two.b);
              }
            });

    for (XmlNode node : nodeList) {
      results.add(new Pair<>(node, valuePattern.match(node.toString())));
    }

    return results.last().a.toString();
  }

  private ListOrSingle<XmlNode> findXmlNodes(String value) {
    // For performance reason, don't try to parse non XML value
    if (value == null || !value.trim().startsWith("<")) {
      notifier().info(String.format("Warning: failed to parse the XML document\nXML: %s", value));
      return null;
    }

    try {
      XmlDocument xmlDocument = Xml.parse(value);
      return xmlDocument.findNodes(expectedValue, xpathNamespaces);
    } catch (XmlException e) {
      notifier()
          .info(
              String.format(
                  "Warning: failed to parse the XML document. Reason: %s\nXML: %s",
                  e.getMessage(), value));
      return null;
    } catch (XPathException e) {
      notifier().info("Warning: failed to evaluate the XPath expression " + expectedValue);
      return null;
    }
  }
}
