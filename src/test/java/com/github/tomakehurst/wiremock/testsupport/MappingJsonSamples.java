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
package com.github.tomakehurst.wiremock.testsupport;

import static com.github.tomakehurst.wiremock.common.Encoding.encodeBase64;

public class MappingJsonSamples {

  public static final String BASIC_MAPPING_REQUEST_WITH_RESPONSE_HEADER =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"GET\",						\n"
          + "		\"url\": \"/a/registered/resource\"			\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 401,							\n"
          + "		\"headers\": {								\n"
          + "			\"Content-Type\": \"text/plain\"		\n"
          + "		},											\n"
          + "		\"body\": \"Not allowed!\"					\n"
          + "	}												\n"
          + "}													";

  public static final String STATUS_ONLY_MAPPING_REQUEST =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"PUT\",						\n"
          + "		\"url\": \"/status/only\"					\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 204								\n"
          + "	}												\n"
          + "}													";

  public static final String STATUS_ONLY_GET_MAPPING_TEMPLATE =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"GET\",						\n"
          + "		\"urlPattern\": \"%s\"						\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 200								\n"
          + "	}												\n"
          + "}													";

  public static final String WITH_RESPONSE_BODY =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"GET\",						\n"
          + "		\"url\": \"/with/body\"						\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 200,							\n"
          + "		\"body\": \"Some content\"					\n"
          + "	}												\n"
          + "}													";

  public static final String SPEC_WITH_RESPONSE_BODY =
      "{                                                  \n"
          + "   \"request\": {                                  \n"
          + "      \"url\": \"/with/body\",                     \n"
          + "       \"method\": \"GET\"                         \n"
          + "   },                                              \n"
          + "   \"response\": {                                 \n"
          + "       \"body\": \"Some content\",                 \n"
          + "       \"status\": 200                             \n"
          + "   }                                               \n"
          + "}                                                  ";

  public static final String BASIC_GET =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"GET\",						\n"
          + "		\"url\": \"/basic/mapping/resource\"		\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 304 							\n"
          + "	}												\n"
          + "}													";

  public static final String BASIC_POST = BASIC_GET.replace("GET", "POST");
  public static final String BASIC_PUT = BASIC_GET.replace("GET", "PUT");
  public static final String BASIC_DELETE = BASIC_GET.replace("GET", "DELETE");
  public static final String BASIC_PATCH = BASIC_GET.replace("GET", "PATCH");
  public static final String BASIC_HEAD = BASIC_GET.replace("GET", "HEAD");
  public static final String BASIC_OPTIONS = BASIC_GET.replace("GET", "OPTIONS");
  public static final String BASIC_TRACE = BASIC_GET.replace("GET", "TRACE");
  public static final String BASIC_ANY_METHOD = BASIC_GET.replace("GET", "ANY");

  public static final String MAPPING_REQUEST_WITH_EXACT_HEADERS =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"GET\",						\n"
          + "		\"url\": \"/header/dependent\",				\n"
          + "		\"headers\": {								\n"
          + "			\"Accept\": {							\n"
          + "				\"equalTo\": \"text/xml\"			\n"
          + "			},										\n"
          + "			\"If-None-Match\": {					\n"
          + "				\"equalTo\": \"abcd1234\"			\n"
          + "			}										\n"
          + "		}											\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 304,							\n"
          + "		\"headers\": {								\n"
          + "			\"Content-Type\": \"text/xml\"			\n"
          + "		}											\n"
          + "	}												\n"
          + "}													";

  public static final String MAPPING_REQUEST_WITH_REGEX_HEADERS =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"GET\",						\n"
          + "		\"url\": \"/header/match/dependent\",		\n"
          + "		\"headers\": {								\n"
          + "			\"Accept\": {							\n"
          + "				\"matches\": \"(.*)xml(.*)\"		\n"
          + "			},										\n"
          + "			\"If-None-Match\": {					\n"
          + "				\"matches\": \"([a-z0-9]*)\"		\n"
          + "			}										\n"
          + "		}											\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 304,							\n"
          + "		\"headers\": {								\n"
          + "			\"Content-Type\": \"text/xml\"			\n"
          + "		}											\n"
          + "	}												\n"
          + "}													";

  public static final String MAPPING_REQUEST_WITH_NEGATIVE_REGEX_HEADERS =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"GET\",						\n"
          + "		\"url\": \"/header/match/dependent\",		\n"
          + "		\"headers\": {								\n"
          + "			\"Accept\": {							\n"
          + "				\"doesNotMatch\": \"(.*)xml(.*)\"	\n"
          + "			}										\n"
          + "		}											\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 200,							\n"
          + "		\"headers\": {								\n"
          + "			\"Content-Type\": \"text/xml\"			\n"
          + "		}											\n"
          + "	}												\n"
          + "}													";

  public static final String WITH_REQUEST_HEADERS =
      "{ 													\n"
          + "	\"request\": {									\n"
          + "		\"method\": \"PUT\",						\n"
          + "		\"url\": \"/header/matches/dependent\",		\n"
          + "		\"headers\": {								\n"
          + "			\"Content-Type\": {						\n"
          + "				\"equalTo\": \"text/xml\"			\n"
          + "			},										\n"
          + "			\"Cache-Control\": {					\n"
          + "				\"contains\": \"private\"			\n"
          + "			},										\n"
          + "			\"If-None-Match\": {					\n"
          + "				\"matches\": \"([a-z0-9]*)\"		\n"
          + "			},										\n"
          + "			\"Accept\": {							\n"
          + "				\"doesNotMatch\": \"(.*)xml(.*)\"	\n"
          + "			}										\n"
          + "		}											\n"
          + "	},												\n"
          + "	\"response\": {									\n"
          + "		\"status\": 201								\n"
          + "	}												\n"
          + "}													";

  public static final String WITH_BODY_PATTERNS =
      "{ 														\n"
          + "	\"request\": {										\n"
          + "		\"method\": \"PUT\",							\n"
          + "		\"url\": \"/body/patterns/dependent\",			\n"
          + "		\"bodyPatterns\": [								\n"
          + "			{ \"equalTo\": \"the number is 1234\" },	\n"
          + "			{ \"contains\": \"number\" },				\n"
          + "			{ \"matches\": \".*[0-9]{4}\" },			\n"
          + "			{ \"doesNotMatch\": \".*5678.*\"}			\n"
          + "		]												\n"
          + "	},													\n"
          + "	\"response\": {										\n"
          + "		\"status\": 201									\n"
          + "	}													\n"
          + "}														";

  public static final byte[] BINARY_COMPRESSED_CONTENT =
      new byte[] {
        31, -117, 8, 8, 72, -53, -8, 79, 0, 3, 103, 122, 105, 112, 100, 97, 116, 97, 45, 111, 117,
        116, 0, -77, 41, 74, 45, 46, -56, -49, 43, 78, -75, -53, 72, -51, -55, -55, -73, -47, -121,
        -13, 1, 9, 69, -3, 52, 26, 0, 0, 0
      };
  public static final String BINARY_COMPRESSED_JSON_STRING =
      encodeBase64(BINARY_COMPRESSED_CONTENT);
  public static final String BINARY_COMPRESSED_CONTENT_AS_STRING = "<response>hello</response>";

  public static final String MAPPING_REQUEST_FOR_BYTE_BODY =
      "{ 													                            \n"
          + "	\"request\": {									                            \n"
          + "		\"method\": \"GET\",						                            \n"
          + "		\"url\": \"/byte/resource/from/file\"			                        \n"
          + "	},												                            \n"
          + "	\"response\": {									                            \n"
          + "		\"status\": 200,							                            \n"
          + "		\"base64Body\": \""
          + encodeBase64(new byte[] {65, 66, 67})
          + "\"		\n"
          + "	}												                            \n"
          + "}													                            ";

  public static final String MAPPING_REQUEST_FOR_BINARY_BYTE_BODY =
      "{ 													                    \n"
          + "	\"request\": {									                    \n"
          + "		\"method\": \"GET\",						                    \n"
          + "		\"url\": \"/bytecompressed/resource/from/file\"			        \n"
          + "	},												                    \n"
          + "	\"response\": {									                    \n"
          + "		\"status\": 200,							                    \n"
          + "		\"base64Body\": \""
          + BINARY_COMPRESSED_JSON_STRING
          + "\"	    \n"
          + "	}												                    \n"
          + "}													                    ";

  public static final String MAPPING_REQUEST_FOR_NON_UTF8 =
      "{                                                                                                                      \n"
          + "   \"request\": {                                                                                      \n"
          + "           \"method\": \"GET\",                                                                \n"
          + "           \"url\": \"/test/nonutf8/\"                         \n"
          + "   },                                                                                                                  \n"
          + "   \"response\": {                                                                                     \n"
          + "           \"status\": 200,                                                                            \n"
          + "           \"headers\": {                                                    \n"
          + "               \"Content-type\": \"text/plain; charset=GB2312\"      \n"
          + "           },                                                    \n"
          + "           \"body\": \"国家标准\"                       \n"
          + "   }                                                                                          \n"
          + "}\n";
}
