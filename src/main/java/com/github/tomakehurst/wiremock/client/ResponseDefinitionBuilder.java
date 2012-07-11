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
package com.github.tomakehurst.wiremock.client;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.mapping.ResponseDefinition;

import java.nio.charset.Charset;

import static com.google.common.base.Charsets.UTF_8;

public class ResponseDefinitionBuilder {

	private int status;
	private byte[] bodyContent;
    private boolean isBinaryBody = false;
    private String bodyFileName;
	private HttpHeaders headers;
	private Integer fixedDelayMilliseconds;
	private String proxyBaseUrl;
	private Fault fault;
	
	public ResponseDefinitionBuilder withStatus(int status) {
		this.status = status;
		return this;
	}
	
	public ResponseDefinitionBuilder withHeader(String key, String value) {
		if (headers == null) {
			headers = new HttpHeaders();
		}
		
		headers.put(key, value);
		return this;
	}
	
	public ResponseDefinitionBuilder withBodyFile(String fileName) {
		this.bodyFileName = fileName;
		return this;
	}
	
	public ResponseDefinitionBuilder withBody(String body) {
		this.bodyContent = body.getBytes(Charset.forName(UTF_8.name()));
        isBinaryBody = false;
		return this;
	}

    public ResponseDefinitionBuilder withBody(byte[] body) {
        this.bodyContent = body;
        isBinaryBody = true;
        return this;
    }

    public ResponseDefinitionBuilder withFixedDelay(Integer milliseconds) {
        this.fixedDelayMilliseconds = milliseconds;
        return this;
    }
	
	public ResponseDefinitionBuilder proxiedFrom(String proxyBaseUrl) {
		this.proxyBaseUrl = proxyBaseUrl;
		return this;
	}
	
	public ResponseDefinitionBuilder withFault(Fault fault) {
		this.fault = fault;
		return this;
	}
	
	public ResponseDefinition build() {
        ResponseDefinition response;

        if(isBinaryBody) {
	        response = new ResponseDefinition(status, bodyContent);
        } else {
            if(bodyContent==null) {
                response = new ResponseDefinition(status, (String)null);
            } else {
                response = new ResponseDefinition(status, new String(bodyContent,Charset.forName(UTF_8.name())));
            }
        }
		response.setHeaders(headers);
		response.setBodyFileName(bodyFileName);
		response.setFixedDelayMilliseconds(fixedDelayMilliseconds);
		response.setProxyBaseUrl(proxyBaseUrl);
		response.setFault(fault);
		return response;
	}
}
