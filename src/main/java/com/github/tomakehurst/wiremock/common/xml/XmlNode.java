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

import com.github.tomakehurst.wiremock.common.ListOrSingle;
import com.google.common.collect.ImmutableMap;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.XMLReader;

public class XmlNode {

  protected static final InheritableThreadLocal<XPath> XPATH_CACHE =
      new InheritableThreadLocal<XPath>() {
        @Override
        protected XPath initialValue() {
          final XPathFactory xPathfactory = XPathFactory.newInstance();
          return xPathfactory.newXPath();
        }
      };

  protected static final InheritableThreadLocal<Transformer> TRANSFORMER_CACHE =
      new InheritableThreadLocal<Transformer>() {
        @Override
        protected Transformer initialValue() {
          TransformerFactory transformerFactory;
          try {
            transformerFactory =
                (TransformerFactory)
                    Class.forName(
                            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl")
                        .newInstance();
            transformerFactory.setAttribute("indent-number", 2);
          } catch (Exception e) {
            transformerFactory = TransformerFactory.newInstance();
          }

          try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(INDENT, "yes");
            transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
            return transformer;
          } catch (TransformerConfigurationException e) {
            return throwUnchecked(e, Transformer.class);
          }
        }
      };

  private static final Class<XMLReader> DOM2SAX_XMLREADER_CLASS = getDom2SaxAvailability();

  private static Class<XMLReader> getDom2SaxAvailability() {
    try {
      return (Class<XMLReader>)
          Class.forName("com.sun.org.apache.xalan.internal.xsltc.trax.DOM2SAX");
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private final Node domNode;
  private final Map<String, String> attributes;

  public XmlNode(Node domNode) {
    this.domNode = domNode;
    attributes =
        domNode.hasAttributes()
            ? convertAttributeMap(domNode.getAttributes())
            : Collections.<String, String>emptyMap();
  }

  private static Map<String, String> convertAttributeMap(NamedNodeMap namedNodeMap) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (int i = 0; i < namedNodeMap.getLength(); i++) {
      Node node = namedNodeMap.item(i);
      builder.put(node.getNodeName(), node.getNodeValue());
    }

    return builder.build();
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  protected static ListOrSingle<XmlNode> toListOrSingle(NodeList nodeList) {
    ListOrSingle<XmlNode> nodes = new ListOrSingle<>();
    for (int i = 0; i < nodeList.getLength(); i++) {
      nodes.add(new XmlNode(nodeList.item(i)));
    }

    return nodes;
  }

  public String getName() {
    return domNode.getNodeName();
  }

  public String getText() {
    return domNode.getTextContent();
  }

  @Override
  public String toString() {
    switch (domNode.getNodeType()) {
      case Node.TEXT_NODE:
      case Node.ATTRIBUTE_NODE:
        return domNode.getTextContent();
      case Node.DOCUMENT_NODE:
      case Node.ELEMENT_NODE:
        return render();
      default:
        return domNode.toString();
    }
  }

  private String render() {
    try {
      Transformer transformer = TRANSFORMER_CACHE.get();
      StreamResult result = new StreamResult(new StringWriter());
      Source source = getSourceForTransform(domNode);
      transformer.transform(source, result);
      return result.getWriter().toString();
    } catch (Exception e) {
      return throwUnchecked(e, String.class);
    }
  }

  // This nasty little hack attempts to ensure no exception is thrown when attempting to print an
  // XML node with
  // unbound namespace prefixes (which can happen when you've selected an element via XPath whose
  // namespaces are declared in a parent element).
  // For some reason Transformer is happy to do this with a SAX source, but not a DOM source.
  private static Source getSourceForTransform(Node node) {
    if (DOM2SAX_XMLREADER_CLASS != null) {
      try {
        Constructor<XMLReader> constructor = DOM2SAX_XMLREADER_CLASS.getConstructor(Node.class);
        XMLReader dom2SAX = constructor.newInstance(node);
        SAXSource saxSource = new SAXSource();
        saxSource.setXMLReader(dom2SAX);
        return saxSource;
      } catch (NoSuchMethodException
          | InstantiationException
          | IllegalAccessException
          | InvocationTargetException e) {
        return new DOMSource(node);
      }
    }

    return new DOMSource(node);
  }
}
