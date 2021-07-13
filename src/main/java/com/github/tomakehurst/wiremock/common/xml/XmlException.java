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
package com.github.tomakehurst.wiremock.common.xml;

import com.github.tomakehurst.wiremock.common.Errors;
import com.github.tomakehurst.wiremock.common.InvalidInputException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlException extends InvalidInputException {

  protected XmlException(Errors errors) {
    super(errors);
  }

  protected XmlException(Throwable cause, Errors errors) {
    super(cause, errors);
  }

  public static XmlException fromSaxException(SAXException e) {
    if (e instanceof SAXParseException) {
      SAXParseException spe = (SAXParseException) e;
      String detail =
          String.format(
              "%s; line %d; column %d",
              spe.getMessage(), spe.getLineNumber(), spe.getColumnNumber());
      return new XmlException(Errors.singleWithDetail(50, e.getMessage(), detail));
    }

    return new XmlException(e, Errors.single(50, e.getMessage()));
  }
}
