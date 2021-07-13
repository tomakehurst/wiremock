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
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.xmlunit.diff.ComparisonType.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.common.xml.Xml;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xmlunit.XMLUnitException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;
import org.xmlunit.placeholder.PlaceholderDifferenceEvaluator;

public class EqualToXmlPattern extends StringValuePattern {

  private static Set<ComparisonType> COUNTED_COMPARISONS =
      ImmutableSet.of(
          ELEMENT_TAG_NAME,
          SCHEMA_LOCATION,
          NO_NAMESPACE_SCHEMA_LOCATION,
          NODE_TYPE,
          NAMESPACE_URI,
          TEXT_VALUE,
          PROCESSING_INSTRUCTION_TARGET,
          PROCESSING_INSTRUCTION_DATA,
          ELEMENT_NUM_ATTRIBUTES,
          ATTR_VALUE,
          CHILD_NODELIST_LENGTH,
          CHILD_LOOKUP,
          ATTR_NAME_LOOKUP);

  private final Boolean enablePlaceholders;
  private final String placeholderOpeningDelimiterRegex;
  private final String placeholderClosingDelimiterRegex;
  private final DifferenceEvaluator diffEvaluator;
  private final Set<ComparisonType> exemptedComparisons;
  private final Document expectedXmlDoc;

  public EqualToXmlPattern(@JsonProperty("equalToXml") String expectedValue) {
    this(expectedValue, null, null, null, null);
  }

  public EqualToXmlPattern(
      @JsonProperty("equalToXml") String expectedValue,
      @JsonProperty("enablePlaceholders") Boolean enablePlaceholders,
      @JsonProperty("placeholderOpeningDelimiterRegex") String placeholderOpeningDelimiterRegex,
      @JsonProperty("placeholderClosingDelimiterRegex") String placeholderClosingDelimiterRegex,
      @JsonProperty("exemptedComparisons") Set<ComparisonType> exemptedComparisons) {

    super(expectedValue);
    expectedXmlDoc = Xml.read(expectedValue); // Throw an exception if we can't parse the document
    this.enablePlaceholders = enablePlaceholders;
    this.placeholderOpeningDelimiterRegex = placeholderOpeningDelimiterRegex;
    this.placeholderClosingDelimiterRegex = placeholderClosingDelimiterRegex;
    this.exemptedComparisons = exemptedComparisons;

    IgnoreUncountedDifferenceEvaluator baseDifferenceEvaluator =
        new IgnoreUncountedDifferenceEvaluator(exemptedComparisons);
    if (enablePlaceholders != null && enablePlaceholders) {
      diffEvaluator =
          DifferenceEvaluators.chain(
              baseDifferenceEvaluator,
              new PlaceholderDifferenceEvaluator(
                  placeholderOpeningDelimiterRegex, placeholderClosingDelimiterRegex));
    } else {
      diffEvaluator = baseDifferenceEvaluator;
    }
  }

  public String getEqualToXml() {
    return expectedValue;
  }

  @Override
  public String getExpected() {
    return Xml.prettyPrint(getValue());
  }

  public Boolean isEnablePlaceholders() {
    return enablePlaceholders;
  }

  public String getPlaceholderOpeningDelimiterRegex() {
    return placeholderOpeningDelimiterRegex;
  }

  public String getPlaceholderClosingDelimiterRegex() {
    return placeholderClosingDelimiterRegex;
  }

  public Set<ComparisonType> getExemptedComparisons() {
    return exemptedComparisons;
  }

  @Override
  public MatchResult match(final String value) {
    return new MatchResult() {
      @Override
      public boolean isExactMatch() {
        if (isNullOrEmpty(value)) {
          return false;
        }
        try {
          Diff diff =
              DiffBuilder.compare(Input.from(expectedXmlDoc))
                  .withTest(value)
                  .withComparisonController(ComparisonControllers.StopWhenDifferent)
                  .ignoreWhitespace()
                  .ignoreComments()
                  .withDifferenceEvaluator(diffEvaluator)
                  .withNodeMatcher(new OrderInvariantNodeMatcher())
                  .withDocumentBuilderFactory(Xml.newDocumentBuilderFactory())
                  .build();

          return !diff.hasDifferences();
        } catch (XMLUnitException e) {
          notifier()
              .info(
                  "Failed to process XML. "
                      + e.getMessage()
                      + "\nExpected:\n"
                      + expectedValue
                      + "\n\nActual:\n"
                      + value);
          return false;
        }
      }

      @Override
      public double getDistance() {
        if (isNullOrEmpty(value)) {
          return 1.0;
        }

        final AtomicInteger totalComparisons = new AtomicInteger(0);
        final AtomicInteger differences = new AtomicInteger(0);

        Diff diff;
        try {
          diff =
              DiffBuilder.compare(Input.from(expectedValue))
                  .withTest(value)
                  .ignoreWhitespace()
                  .ignoreComments()
                  .withDifferenceEvaluator(diffEvaluator)
                  .withComparisonListeners(
                      new ComparisonListener() {
                        @Override
                        public void comparisonPerformed(
                            Comparison comparison, ComparisonResult outcome) {
                          if (COUNTED_COMPARISONS.contains(comparison.getType())
                              && comparison.getControlDetails().getValue() != null) {
                            totalComparisons.incrementAndGet();
                            if (outcome == ComparisonResult.DIFFERENT) {
                              differences.incrementAndGet();
                            }
                          }
                        }
                      })
                  .withDocumentBuilderFactory(Xml.newDocumentBuilderFactory())
                  .build();
        } catch (XMLUnitException e) {
          notifier()
              .info(
                  "Failed to process XML. "
                      + e.getMessage()
                      + "\nExpected:\n"
                      + expectedValue
                      + "\n\nActual:\n"
                      + value);
          return 1.0;
        }

        notifier().info(Joiner.on("\n").join(diff.getDifferences()));

        return differences.doubleValue() / totalComparisons.doubleValue();
      }
    };
  }

  private static class IgnoreUncountedDifferenceEvaluator implements DifferenceEvaluator {

    private final Set<ComparisonType> finalCountedComparisons;

    public IgnoreUncountedDifferenceEvaluator(Set<ComparisonType> exemptedComparisons) {
      finalCountedComparisons =
          exemptedComparisons != null
              ? Sets.difference(COUNTED_COMPARISONS, exemptedComparisons)
              : COUNTED_COMPARISONS;
    }

    @Override
    public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
      if (finalCountedComparisons.contains(comparison.getType())
          && comparison.getControlDetails().getValue() != null) {
        return outcome;
      }

      return ComparisonResult.EQUAL;
    }
  }

  public EqualToXmlPattern exemptingComparisons(ComparisonType... comparisons) {
    return new EqualToXmlPattern(
        expectedValue,
        enablePlaceholders,
        placeholderOpeningDelimiterRegex,
        placeholderClosingDelimiterRegex,
        ImmutableSet.copyOf(comparisons));
  }

  private static final class OrderInvariantNodeMatcher extends DefaultNodeMatcher {
    @Override
    public Iterable<Map.Entry<Node, Node>> match(
        Iterable<Node> controlNodes, Iterable<Node> testNodes) {

      return super.match(sort(controlNodes), sort(testNodes));
    }

    private static Iterable<Node> sort(Iterable<Node> nodes) {
      return FluentIterable.from(nodes).toSortedList(COMPARATOR);
    }

    private static final Comparator<Node> COMPARATOR =
        new Comparator<Node>() {
          @Override
          public int compare(Node node1, Node node2) {
            return node1.getLocalName().compareTo(node2.getLocalName());
          }
        };
  }
}
