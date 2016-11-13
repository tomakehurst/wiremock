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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.matching.optional.*;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.google.common.collect.Iterables.tryFind;
import static java.util.Arrays.asList;

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
                    .put("anything", AnythingPattern.class)
                    .put("matchesOrAbsent", OptionalRegexPattern.class)
                    .put("containsOrAbsent", OptionalContainsPattern.class)
                    .put("doesNotMatchOrAbsent", OptionalNegativeRegexPattern.class)
                    .put("equalsToOrAbsent", OptionalEqualToPattern.class)
                    .put("matchesOrAbsentXPath", OptionalMatchesXPathPattern.class)
                    .put("matchesOrAbsentJsonPath", OptionalMatchesJsonPathPattern.class)
                    .build();

    @Override
    public StringValuePattern deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        JsonNode rootNode = parser.readValueAsTree();

        if (isAbsent(rootNode)) {
            return StringValuePattern.ABSENT;
        }

        Class<? extends StringValuePattern> patternClass = findPatternClass(rootNode);
        if (patternClass.equals(EqualToJsonPattern.class)) {
            return deserializeEqualToJson(rootNode);
        } else if (patternClass.equals(MatchesXPathPattern.class) || patternClass.equals(OptionalMatchesXPathPattern.class)) {
            return deserialiseMatchesXPathPattern(rootNode);
        }

        Constructor<? extends StringValuePattern> constructor = findConstructor(patternClass);

        Map.Entry<String, JsonNode> entry = rootNode.fields().next();
        String operand = entry.getValue().textValue();
        try {
            return constructor.newInstance(operand);
        } catch (Exception e) {
            return throwUnchecked(e, StringValuePattern.class);
        }
    }

    private EqualToJsonPattern deserializeEqualToJson(JsonNode rootNode) throws JsonMappingException {
        if (!rootNode.has("equalToJson")) {
            throw new JsonMappingException(rootNode.toString() + " is not a valid comparison");
        }

        String operand = rootNode.findValue("equalToJson").textValue();
        boolean ignoreArrayOrder = fromNullable(rootNode.findValue("ignoreArrayOrder"));
        boolean ignoreExtraElements = fromNullable(rootNode.findValue("ignoreExtraElements"));

        return new EqualToJsonPattern(operand, ignoreArrayOrder, ignoreExtraElements);
    }

    private StringValuePattern deserialiseMatchesXPathPattern(JsonNode rootNode) throws JsonMappingException {
        rootHasRequiredNode(rootNode);
        final Map<String, String> namespaces = deserializeNamespaces(rootNode);

        if (hasXPathPattern(rootNode)) {
            String operand = rootNode.findValue("matchesXPath").textValue();
            return new MatchesXPathPattern(operand, namespaces);
        } else if (hasOptionalXPathPattern(rootNode)) {
            String operand = rootNode.findValue("matchesOrAbsentXPath").textValue();
            return new OptionalMatchesXPathPattern(operand, namespaces);
        } else {
            throw new IllegalStateException("Illegal state expection appeared when deserialize XPathPattern");
        }
    }

    private boolean rootHasRequiredNode(final JsonNode rootNode) throws JsonMappingException {
        if (hasXPathPattern(rootNode) || hasOptionalXPathPattern(rootNode)) {
            return true;
        }

        throw new JsonMappingException(rootNode.toString() + " is not a valid comparison");
    }

    private boolean hasOptionalXPathPattern(final JsonNode rootNode) throws JsonMappingException {
        return rootNode.has("matchesOrAbsentXPath");
    }

    private boolean hasXPathPattern(final JsonNode rootNode) throws JsonMappingException {
        return rootNode.has("matchesXPath");
    }

    private Map<String, String> deserializeNamespaces(final JsonNode rootNode) {
        final JsonNode namespacesNode = rootNode.findValue("xPathNamespaces");

        return namespacesNode != null ?
                toNamespaceMap(namespacesNode) :
                Collections.<String, String>emptyMap();
    }

    private static Map<String, String> toNamespaceMap(JsonNode namespacesNode) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (Iterator<Map.Entry<String, JsonNode>> fields = namespacesNode.fields(); fields.hasNext(); ) {
            Map.Entry<String, JsonNode> field = fields.next();
            builder.put(field.getKey(), field.getValue().textValue());
        }

        return builder.build();
    }

    private static boolean fromNullable(JsonNode node) {
        return node != null && node.asBoolean();
    }

    @SuppressWarnings("unchecked")
    private static Constructor<? extends StringValuePattern> findConstructor(Class<? extends StringValuePattern> clazz) {
        Optional<Constructor<?>> optionalConstructor =
                tryFind(asList(clazz.getDeclaredConstructors()), new Predicate<Constructor<?>>() {
                    @Override
                    public boolean apply(Constructor<?> input) {
                        return input.getParameterTypes().length == 1 &&
                                input.getGenericParameterTypes()[0].equals(String.class);
                    }
                });

        if (!optionalConstructor.isPresent()) {
            throw new IllegalStateException("Constructor for " + clazz.getSimpleName() + " must have a single string argument constructor");
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

    private static Class<? extends StringValuePattern> findPatternClass(JsonNode rootNode) throws JsonMappingException {
        for (Map.Entry<String, JsonNode> node : ImmutableList.copyOf(rootNode.fields())) {
            Class<? extends StringValuePattern> patternClass = PATTERNS.get(node.getKey());
            if (patternClass != null) {
                return patternClass;
            }
        }

        throw new JsonMappingException(rootNode.toString() + " is not a valid comparison");
    }
}
