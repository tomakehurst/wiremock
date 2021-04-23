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
package com.github.tomakehurst.wiremock.common;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

import java.util.Random;

public class VeryShortRandomIdGenerator implements IdGenerator {
    
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public RequestResponseId generate(Request _request, Response _response, byte[] _bodyBytes) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(randomChar());
        }
        
        return new RequestResponseId(sb.toString());
    }
    
    private static char randomChar() {
        final Random random = new Random();
        final int index = random.nextInt(CHARS.length());
        return CHARS.charAt(index);
    }

}
