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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;

import com.github.tomakehurst.wiremock.common.Errors;
import com.github.tomakehurst.wiremock.common.SilentErrorHandler;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathFactory;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Xml {

  private Xml() {
    // Hide constructor
  }

  public static void optimizeFactoriesLoading() {
    try {
      String transformerFactoryImpl = TransformerFactory.newInstance().getClass().getName();
      String xPathFactoryImpl = XPathFactory.newInstance().getClass().getName();

      setProperty(TransformerFactory.class.getName(), transformerFactoryImpl);
      setProperty(
          XPathFactory.DEFAULT_PROPERTY_NAME + ":" + XPathFactory.DEFAULT_OBJECT_MODEL_URI,
          xPathFactoryImpl);

      XMLUnit.setTransformerFactory(transformerFactoryImpl);
      XMLUnit.setXPathFactory(xPathFactoryImpl);
    } catch (Throwable ignored) {
      // Since this is just an optimisation, if an exception is thrown we do nothing and carry on
    }
  }

  private static String setProperty(final String name, final String value) {
    return AccessController.doPrivileged(
        new PrivilegedAction<String>() {
          @Override
          public String run() {
            return System.setProperty(name, value);
          }
        });
  }

  public static String prettyPrint(String xml) {
    try {
      return prettyPrint(read(xml));
    } catch (Exception e) {
      return throwUnchecked(e, String.class);
    }
  }

  public static String prettyPrint(Document doc) {
    try {
      TransformerFactory transformerFactory = createTransformerFactory();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(INDENT, "yes");
      transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
      StreamResult result = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, result);
      return result.getWriter().toString();
    } catch (Exception e) {
      return throwUnchecked(e, String.class);
    }
  }

  private static TransformerFactory createTransformerFactory() {
    try {
      TransformerFactory transformerFactory =
          (TransformerFactory)
              Class.forName("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl")
                  .newInstance();
      transformerFactory.setAttribute("indent-number", 2);
      return transformerFactory;
    } catch (Exception e) {
      return TransformerFactory.newInstance();
    }
  }

  public static Document read(String xml) {
    try {
      DocumentBuilderFactory dbf = newDocumentBuilderFactory();
      DocumentBuilder db = dbf.newDocumentBuilder();
      InputSource is = new InputSource(new StringReader(xml));
      return db.parse(is);
    } catch (SAXException e) {
      throw XmlException.fromSaxException(e);
    } catch (Exception e) {
      return throwUnchecked(e, Document.class);
    }
  }

  public static String toStringValue(Node node) {
    switch (node.getNodeType()) {
      case Node.TEXT_NODE:
      case Node.ATTRIBUTE_NODE:
        return node.getTextContent();
      case Node.ELEMENT_NODE:
        return render(node);
      default:
        return node.toString();
    }
  }

  private static String render(Node node) {
    try {
      StringWriter sw = new StringWriter();
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(INDENT, "yes");
      transformer.transform(new DOMSource(node), new StreamResult(sw));
      return sw.toString();
    } catch (TransformerException e) {
      return throwUnchecked(e, String.class);
    }
  }

  public static XmlDocument parse(String xml) {
    try {
      InputSource source = new InputSource(new StringReader(xml));
      return new XmlDocument(getDocumentBuilder().parse(source));
    } catch (SAXException | IOException e) {
      throw new XmlException(Errors.single(50, e.getMessage()));
    }
  }

  private static DocumentBuilder getDocumentBuilder() {
    try {
      return newDocumentBuilderFactory().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      return throwUnchecked(e, DocumentBuilder.class);
    }
  }

  public static DocumentBuilderFactory newDocumentBuilderFactory() {
    return new SkipResolvingEntitiesDocumentBuilderFactory();
  }

  private static class SkipResolvingEntitiesDocumentBuilderFactory extends DocumentBuilderFactory {

    private static final ThreadLocal<DocumentBuilderFactory> DBF_CACHE =
        new ThreadLocal<DocumentBuilderFactory>() {
          @Override
          protected DocumentBuilderFactory initialValue() {
            try {
              DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
              dbf.setFeature("http://xml.org/sax/features/validation", false);
              dbf.setFeature(
                  "http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
              dbf.setFeature(
                  "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
              return dbf;
            } catch (ParserConfigurationException e) {
              return throwUnchecked(e, DocumentBuilderFactory.class);
            }
          }
        };
    private static final ThreadLocal<DocumentBuilder> DB_CACHE =
        new ThreadLocal<DocumentBuilder>() {
          @Override
          protected DocumentBuilder initialValue() {
            try {
              DocumentBuilder documentBuilder = DBF_CACHE.get().newDocumentBuilder();
              documentBuilder.setErrorHandler(new SilentErrorHandler());
              return documentBuilder;
            } catch (ParserConfigurationException e) {
              return throwUnchecked(e, DocumentBuilder.class);
            }
          }

          @Override
          public DocumentBuilder get() {
            DocumentBuilder documentBuilder = super.get();
            documentBuilder.setEntityResolver(new ResolveToEmptyString());
            documentBuilder.setErrorHandler(null);
            return documentBuilder;
          }
        };

    @Override
    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
      return DB_CACHE.get();
    }

    private static class ResolveToEmptyString implements EntityResolver {
      @Override
      public InputSource resolveEntity(String publicId, String systemId)
          throws SAXException, IOException {
        return new InputSource(new StringReader(""));
      }
    }

    @Override
    public void setAttribute(String name, Object value) {
      DBF_CACHE.get().setAttribute(name, value);
    }

    @Override
    public Object getAttribute(String name) {
      return DBF_CACHE.get().getAttribute(name);
    }

    @Override
    public void setFeature(String name, boolean value) throws ParserConfigurationException {
      DBF_CACHE.get().setFeature(name, value);
    }

    @Override
    public boolean getFeature(String name) throws ParserConfigurationException {
      return DBF_CACHE.get().getFeature(name);
    }
  }
}
