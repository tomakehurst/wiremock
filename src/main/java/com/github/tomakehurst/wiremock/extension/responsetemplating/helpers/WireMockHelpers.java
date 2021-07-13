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

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.util.Date;

/**
 * This enum is implemented similar to the StringHelpers of handlebars. It is basically a library of
 * all available wiremock helpers
 */
public enum WireMockHelpers implements Helper<Object> {
  xPath {
    private final HandlebarsXPathHelper helper = new HandlebarsXPathHelper();

    @Override
    public Object apply(final Object context, final Options options) throws IOException {
      return this.helper.apply(String.valueOf(context), options);
    }
  },
  soapXPath {
    private final HandlebarsSoapHelper helper = new HandlebarsSoapHelper();

    @Override
    public Object apply(final Object context, final Options options) throws IOException {
      return this.helper.apply(String.valueOf(context), options);
    }
  },
  jsonPath {
    private final HandlebarsJsonPathHelper helper = new HandlebarsJsonPathHelper();

    @Override
    public Object apply(final Object context, final Options options) throws IOException {
      return this.helper.apply(context, options);
    }
  },
  randomValue {
    private final HandlebarsRandomValuesHelper helper = new HandlebarsRandomValuesHelper();

    @Override
    public Object apply(final Object context, final Options options) throws IOException {
      return this.helper.apply(null, options);
    }
  },
  hostname {
    private final HostnameHelper helper = new HostnameHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return this.helper.apply(context, options);
    }
  },
  date {
    private final HandlebarsCurrentDateHelper helper = new HandlebarsCurrentDateHelper();

    @Override
    public Object apply(final Object context, final Options options) throws IOException {
      Date dateContext = context instanceof Date ? (Date) context : null;
      return this.helper.apply(dateContext, options);
    }
  },
  now {
    private final HandlebarsCurrentDateHelper helper = new HandlebarsCurrentDateHelper();

    @Override
    public Object apply(final Object context, final Options options) throws IOException {
      return this.helper.apply(null, options);
    }
  },
  parseDate {
    private final ParseDateHelper helper = new ParseDateHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context.toString(), options);
    }
  },
  truncateDate {
    private final TruncateDateTimeHelper helper = new TruncateDateTimeHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  trim {
    private final StringTrimHelper helper = new StringTrimHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  base64 {
    private final Base64Helper helper = new Base64Helper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  urlEncode {
    private final UrlEncodingHelper helper = new UrlEncodingHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  formData {
    private final FormDataHelper helper = new FormDataHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  regexExtract {
    private final RegexExtractHelper helper = new RegexExtractHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  size {
    private final SizeHelper helper = new SizeHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  pickRandom {
    private final PickRandomHelper helper = new PickRandomHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  randomInt {
    private final RandomIntHelper helper = new RandomIntHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(null, options);
    }
  },

  randomDecimal {
    private final RandomDecimalHelper helper = new RandomDecimalHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(null, options);
    }
  },

  range {
    private final RangeHelper helper = new RangeHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  array {
    private final ArrayHelper helper = new ArrayHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  parseJson {
    private final ParseJsonHelper helper = new ParseJsonHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  matches {
    private final MatchesRegexHelper helper = new MatchesRegexHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  contains {
    private final ContainsHelper helper = new ContainsHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  },

  math {
    private final MathsHelper helper = new MathsHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
      return helper.apply(context, options);
    }
  }
}
