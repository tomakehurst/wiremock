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
package com.github.tomakehurst.wiremock.mapping;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import com.github.tomakehurst.wiremock.verification.VerificationResult;

public final class Json {
	
	/**
	 * All members of this class are static, the ctor
	 * should not be available
	 */
	private Json() {
		// never create an instance
	}
	
	public static RequestResponseMapping buildMappingFrom(String mappingSpecJson) {
		return read(mappingSpecJson, RequestResponseMapping.class);
	}
	
	public static String buildJsonStringFor(RequestResponseMapping mapping) {
		return write(mapping);
	}
	
	public static VerificationResult buildVerificationResultFrom(String json) {
		return read(json, VerificationResult.class);
	}
	
	public static RequestPattern buildRequestPatternFrom(String json) {
		return read(json, RequestPattern.class);
	}
	
	public static <T> T read(String json, Class<T> clazz) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
			return mapper.readValue(json, clazz);
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to bind JSON to object. Reason: " + ioe.getMessage() + "  JSON:" + json, ioe);
		}
	}
	
	public static <T> String write(T object) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(object);
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to generate JSON from object. Reason: " + ioe.getMessage(), ioe);
		}
	}
}
