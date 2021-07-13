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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Iterators.find;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.common.DateTimeUnit;
import com.github.tomakehurst.wiremock.common.Json;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import org.xmlunit.diff.ComparisonType;

public class StringValuePatternJsonDeserializer extends JsonDeserializer<StringValuePattern> {

  private static final Map<String, Class<? extends StringValuePattern>> PATTERNS =
      new ImmutableMap.Builder<String, Class<? extends StringValuePattern>>()
          .put("equalTo", EqualToPattern.class)
          .put("equalToJson", EqualToJsonPattern.class)
          .put("matchesJsonPath", MatchesJsonPathPattern.class)
          .put("equalToXml", EqualToXmlPattern.class)
          .put("matchesXPath", MatchesXPathPattern.class)
          .put("contains", ContainsPattern.class)
          .put("matches", RegexPattern.class)
          .put("doesNotMatch", NegativeRegexPattern.class)
          .put("before", BeforeDateTimePattern.class)
          .put("after", AfterDateTimePattern.class)
          .put("equalToDateTime", EqualToDateTimePattern.class)
          .put("anything", AnythingPattern.class)
          .put("absent", AbsentPattern.class)
          .put("and", LogicalAnd.class)
          .put("or", LogicalOr.class)
          .build();

  @Override
  public StringValuePattern deserialize(JsonParser parser, DeserializationContext context)
      throws IOException, JsonProcessingException {
    JsonNode rootNode = parser.readValueAsTree();
    return buildStringValuePattern(rootNode);
  }

  public StringValuePattern buildStringValuePattern(JsonNode rootNode) throws JsonMappingException {
    if (isAbsent(rootNode)) {
      return AbsentPattern.ABSENT;
    }

    Class<? extends StringValuePattern> patternClass = findPatternClass(rootNode);
    if (patternClass.equals(EqualToJsonPattern.class)) {
      return deserializeEqualToJson(rootNode);
    } else if (patternClass.equals(EqualToXmlPattern.class)) {
      return deserializeEqualToXml(rootNode);
    } else if (patternClass.equals(MatchesJsonPathPattern.class)) {
      return deserialiseMatchesJsonPathPattern(rootNode);
    } else if (patternClass.equals(MatchesXPathPattern.class)) {
      return deserialiseMatchesXPathPattern(rootNode);
    } else if (patternClass.equals(EqualToPattern.class)) {
      return deserializeEqualTo(rootNode);
    } else if (AbstractDateTimePattern.class.isAssignableFrom(patternClass)) {
      final Map.Entry<String, JsonNode> mainFieldEntry = findMainFieldEntry(rootNode);
      String matcherName = mainFieldEntry.getKey();
      return deserialiseDateTimePattern(rootNode, matcherName);
    } else if (patternClass.equals(LogicalAnd.class)) {
      return deserializeAnd(rootNode);
    } else if (patternClass.equals(LogicalOr.class)) {
      return deserializeOr(rootNode);
    }

    final Map.Entry<String, JsonNode> mainFieldEntry = findMainFieldEntry(rootNode);
    if (!mainFieldEntry.getValue().isTextual()) {
      throw new JsonMappingException(
          mainFieldEntry.getKey() + " operand must be a non-null string");
    }
    String operand = mainFieldEntry.getValue().textValue();
    try {
      Constructor<? extends StringValuePattern> constructor = findConstructor(patternClass);
      return constructor.newInstance(operand);
    } catch (Exception e) {
      return throwUnchecked(e, StringValuePattern.class);
    }
  }

  private static Map.Entry<String, JsonNode> findMainFieldEntry(JsonNode rootNode) {
    return find(
        rootNode.fields(),
        new Predicate<Map.Entry<String, JsonNode>>() {
          @Override
          public boolean apply(Map.Entry<String, JsonNode> input) {
            return PATTERNS.keySet().contains(input.getKey());
          }
        });
  }

  private EqualToPattern deserializeEqualTo(JsonNode rootNode) throws JsonMappingException {
    if (!rootNode.has("equalTo")) {
      throw new JsonMappingException(rootNode.toString() + " is not a valid match operation");
    }

    JsonNode equalToNode = rootNode.findValue("equalTo");
    if (!equalToNode.isTextual()) {
      throw new JsonMappingException("equalTo operand must be a non-null string");
    }

    String operand = equalToNode.textValue();
    Boolean ignoreCase = fromNullable(rootNode.findValue("caseInsensitive"));

    return new EqualToPattern(operand, ignoreCase);
  }

  private EqualToJsonPattern deserializeEqualToJson(JsonNode rootNode) throws JsonMappingException {
    if (!rootNode.has("equalToJson")) {
      throw new JsonMappingException(rootNode.toString() + " is not a valid match operation");
    }

    JsonNode operand = rootNode.findValue("equalToJson");

    Boolean ignoreArrayOrder = fromNullable(rootNode.findValue("ignoreArrayOrder"));
    Boolean ignoreExtraElements = fromNullable(rootNode.findValue("ignoreExtraElements"));

    // Allow either a JSON value or a string containing JSON
    if (operand.isTextual()) {
      return new EqualToJsonPattern(operand.textValue(), ignoreArrayOrder, ignoreExtraElements);
    } else {
      return new EqualToJsonPattern(operand, ignoreArrayOrder, ignoreExtraElements);
    }
  }

  private EqualToXmlPattern deserializeEqualToXml(JsonNode rootNode) throws JsonMappingException {
    if (!rootNode.has("equalToXml")) {
      throw new JsonMappingException(rootNode.toString() + " is not a valid match operation");
    }

    JsonNode operand = rootNode.findValue("equalToXml");

    Boolean enablePlaceholders = fromNullable(rootNode.findValue("enablePlaceholders"));
    String placeholderOpeningDelimiterRegex =
        fromNullableTextNode(rootNode.findValue("placeholderOpeningDelimiterRegex"));
    String placeholderClosingDelimiterRegex =
        fromNullableTextNode(rootNode.findValue("placeholderClosingDelimiterRegex"));
    Set<ComparisonType> exemptedComparisons =
        comparisonTypeSetFromArray(rootNode.findValue("exemptedComparisons"));

    return new EqualToXmlPattern(
        operand.textValue(),
        enablePlaceholders,
        placeholderOpeningDelimiterRegex,
        placeholderClosingDelimiterRegex,
        exemptedComparisons);
  }

  private MatchesJsonPathPattern deserialiseMatchesJsonPathPattern(JsonNode rootNode)
      throws JsonMappingException {
    if (!rootNode.has("matchesJsonPath")) {
      throw new JsonMappingException(rootNode.toString() + " is not a valid match operation");
    }

    JsonNode outerPatternNode = rootNode.findValue("matchesJsonPath");
    if (outerPatternNode.isTextual()) {
      return new MatchesJsonPathPattern(outerPatternNode.textValue());
    }

    if (!outerPatternNode.has("expression")) {
      throw new JsonMappingException("expression is required in the advanced matchesJsonPath form");
    }

    String expression = outerPatternNode.findValue("expression").textValue();
    StringValuePattern valuePattern = buildStringValuePattern(outerPatternNode);

    return new MatchesJsonPathPattern(expression, valuePattern);
  }

  private MatchesXPathPattern deserialiseMatchesXPathPattern(JsonNode rootNode)
      throws JsonMappingException {
    if (!rootNode.has("matchesXPath")) {
      throw new JsonMappingException(rootNode.toString() + " is not a valid match operation");
    }

    JsonNode namespacesNode = rootNode.findValue("xPathNamespaces");

    Map<String, String> namespaces =
        namespacesNode != null
            ? toNamespaceMap(namespacesNode)
            : Collections.<String, String>emptyMap();

    JsonNode outerPatternNode = rootNode.findValue("matchesXPath");
    if (outerPatternNode.isTextual()) {
      return new MatchesXPathPattern(outerPatternNode.textValue(), namespaces);
    }

    if (!outerPatternNode.has("expression")) {
      throw new JsonMappingException("expression is required in the advanced matchesXPath form");
    }

    String expression = outerPatternNode.findValue("expression").textValue();
    StringValuePattern valuePattern = buildStringValuePattern(outerPatternNode);

    return new MatchesXPathPattern(expression, namespaces, valuePattern);
  }

  private StringValuePattern deserialiseDateTimePattern(JsonNode rootNode, String matcherName)
      throws JsonMappingException {
    JsonNode dateTimeNode = rootNode.findValue(matcherName);
    JsonNode formatNode = rootNode.findValue("actualFormat");
    JsonNode truncateExpectedNode = rootNode.findValue("truncateExpected");
    JsonNode truncateActualNode = rootNode.findValue("truncateActual");
    JsonNode expectedOffsetAmountNode = rootNode.findValue("expectedOffset");
    JsonNode expectedOffsetUnitNode = rootNode.findValue("expectedOffsetUnit");

    switch (matcherName) {
      case "before":
        return new BeforeDateTimePattern(
            dateTimeNode.textValue(),
            formatNode != null ? formatNode.textValue() : null,
            truncateExpectedNode != null ? truncateExpectedNode.textValue() : null,
            truncateActualNode != null ? truncateActualNode.textValue() : null,
            expectedOffsetAmountNode != null ? expectedOffsetAmountNode.intValue() : null,
            expectedOffsetUnitNode != null
                ? DateTimeUnit.valueOf(expectedOffsetUnitNode.textValue().toUpperCase())
                : null);
      case "after":
        return new AfterDateTimePattern(
            dateTimeNode.textValue(),
            formatNode != null ? formatNode.textValue() : null,
            truncateExpectedNode != null ? truncateExpectedNode.textValue() : null,
            truncateActualNode != null ? truncateActualNode.textValue() : null,
            expectedOffsetAmountNode != null ? expectedOffsetAmountNode.intValue() : null,
            expectedOffsetUnitNode != null
                ? DateTimeUnit.valueOf(expectedOffsetUnitNode.textValue().toUpperCase())
                : null);
      case "equalToDateTime":
        return new EqualToDateTimePattern(
            dateTimeNode.textValue(),
            formatNode != null ? formatNode.textValue() : null,
            truncateExpectedNode != null ? truncateExpectedNode.textValue() : null,
            truncateActualNode != null ? truncateActualNode.textValue() : null,
            expectedOffsetAmountNode != null ? expectedOffsetAmountNode.intValue() : null,
            expectedOffsetUnitNode != null
                ? DateTimeUnit.valueOf(expectedOffsetUnitNode.textValue().toUpperCase())
                : null);
    }

    throw new JsonMappingException(rootNode.toString() + " is not a valid match operation");
  }

  private LogicalAnd deserializeAnd(JsonNode node) throws JsonMappingException {
    JsonNode operandsNode = node.get("and");
    if (!operandsNode.isArray()) {
      throw new JsonMappingException("and field must be an array of matchers");
    }

    JsonParser parser = Json.getObjectMapper().treeAsTokens(node.get("and"));

    try {
      List<StringValuePattern> operands =
          parser.readValueAs(new TypeReference<List<StringValuePattern>>() {});
      return new LogicalAnd(operands);
    } catch (IOException e) {
      return throwUnchecked(e, LogicalAnd.class);
    }
  }

  private LogicalOr deserializeOr(JsonNode node) throws JsonMappingException {
    JsonNode operandsNode = node.get("or");
    if (!operandsNode.isArray()) {
      throw new JsonMappingException("and field must be an array of matchers");
    }

    JsonParser parser = Json.getObjectMapper().treeAsTokens(node.get("or"));

    try {
      List<StringValuePattern> operands =
          parser.readValueAs(new TypeReference<List<StringValuePattern>>() {});
      return new LogicalOr(operands);
    } catch (IOException e) {
      return throwUnchecked(e, LogicalOr.class);
    }
  }

  private static Map<String, String> toNamespaceMap(JsonNode namespacesNode) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (Iterator<Map.Entry<String, JsonNode>> fields = namespacesNode.fields();
        fields.hasNext(); ) {
      Map.Entry<String, JsonNode> field = fields.next();
      builder.put(field.getKey(), field.getValue().textValue());
    }

    return builder.build();
  }

  private static Boolean fromNullable(JsonNode node) {
    return node == null ? null : node.asBoolean();
  }

  private static String fromNullableTextNode(JsonNode node) {
    return node == null ? null : node.asText();
  }

  private static Set<ComparisonType> comparisonTypeSetFromArray(JsonNode node) {
    if (node == null || !node.isArray()) {
      return null;
    }

    ImmutableSet.Builder<ComparisonType> builder = ImmutableSet.builder();
    for (JsonNode itemNode : node) {
      builder.add(ComparisonType.valueOf(itemNode.textValue()));
    }

    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private static Constructor<? extends StringValuePattern> findConstructor(
      Class<? extends StringValuePattern> clazz) {
    Optional<Constructor<?>> optionalConstructor =
        tryFind(
            asList(clazz.getDeclaredConstructors()),
            new Predicate<Constructor<?>>() {
              @Override
              public boolean apply(Constructor<?> input) {
                return input.getParameterTypes().length == 1
                    && input.getGenericParameterTypes()[0].equals(String.class);
              }
            });

    if (!optionalConstructor.isPresent()) {
      throw new IllegalStateException(
          "Constructor for "
              + clazz.getSimpleName()
              + " must have a single string argument constructor");
    }

    return (Constructor<? extends StringValuePattern>) optionalConstructor.get();
  }

  private static boolean isAbsent(JsonNode rootNode) {
    for (Map.Entry<String, JsonNode> node : ImmutableList.copyOf(rootNode.fields())) {
      if (node.getKey().equals("absent")) {
        return true;
      }
    }

    return false;
  }

  private static Class<? extends StringValuePattern> findPatternClass(JsonNode rootNode)
      throws JsonMappingException {
    for (Map.Entry<String, JsonNode> node : ImmutableList.copyOf(rootNode.fields())) {
      Class<? extends StringValuePattern> patternClass = PATTERNS.get(node.getKey());
      if (patternClass != null) {
        return patternClass;
      }
    }

    throw new JsonMappingException(rootNode.toString() + " is not a valid match operation");
  }
}
