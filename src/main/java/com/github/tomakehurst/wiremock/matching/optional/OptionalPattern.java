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
package com.github.tomakehurst.wiremock.matching.optional;

import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

public abstract class OptionalPattern extends StringValuePattern {

    protected StringValuePattern pattern;

    public OptionalPattern(final StringValuePattern pattern) {
        super(pattern.getValue());
        this.pattern = pattern;
    }

    @Override
    public MatchResult match(final String value) {
        final MatchResult patternMatchResult = pattern.match(value);
        final MatchResult absentMatchResult = ABSENT.match(value);

        return MatchResult.of(patternMatchResult.isExactMatch() || absentMatchResult.isExactMatch());
    }
}
