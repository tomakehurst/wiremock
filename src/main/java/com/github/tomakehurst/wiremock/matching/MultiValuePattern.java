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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.MultiValue;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import java.util.Comparator;
import java.util.List;

import static java.util.Collections.min;

public class MultiValuePattern implements NamedValueMatcher<MultiValue> {

    private final StringValuePattern valuePattern;

    public MultiValuePattern(StringValuePattern valuePattern) {
        this.valuePattern = valuePattern;
    }

    @JsonCreator
    public static MultiValuePattern of(StringValuePattern valuePattern) {
        return new MultiValuePattern(valuePattern);
    }

    public static MultiValuePattern absent() {
        return new MultiValuePattern(WireMock.absent());
    }

    @Override
    public MatchResult match(MultiValue multiValue) {
        if (valuePattern.nullSafeIsAbsent()) {
            return MatchResult.of(!multiValue.isPresent());
        }

        if (valuePattern.isPresent() && multiValue.isPresent()) {
            return getBestMatch(valuePattern, multiValue.values());
        }

        if (valuePattern.isOptional() && !multiValue.isPresent()) {
            return MatchResult.of(!multiValue.isPresent());
        }

        return MatchResult.of(valuePattern.isPresent() == multiValue.isPresent());
    }

    @JsonValue
    public StringValuePattern getValuePattern() {
        return valuePattern;
    }

    @Override
    public String getName() {
        return valuePattern.getName();
    }

    @Override
    public String getExpected() {
        return valuePattern.expectedValue;
    }

    private static MatchResult getBestMatch(final StringValuePattern valuePattern, List<String> values) {
        List<MatchResult> allResults = Lists.transform(values, new Function<String, MatchResult>() {
            public MatchResult apply(String input) {
                return valuePattern.match(input);
            }
        });

        return min(allResults, new Comparator<MatchResult>() {
            public int compare(MatchResult o1, MatchResult o2) {
                return new Double(o1.getDistance()).compareTo(o2.getDistance());
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiValuePattern that = (MultiValuePattern) o;
        return Objects.equal(valuePattern, that.valuePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(valuePattern);
    }
}
