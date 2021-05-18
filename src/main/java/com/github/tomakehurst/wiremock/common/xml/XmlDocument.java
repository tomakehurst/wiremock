package com.github.tomakehurst.wiremock.common.xml;

import com.github.tomakehurst.wiremock.common.ListOrSingle;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xmlunit.util.Convert;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import java.util.HashMap;
import java.util.Map;

import static javax.xml.xpath.XPathConstants.NODESET;

public class XmlDocument extends XmlNode {

    private final Document document;

    public XmlDocument(Document document) {
        super(document);
        this.document = document;
    }

    public ListOrSingle<XmlNode> findNodes(String xPathExpression) {
        return findNodes(xPathExpression, null);
    }

    public ListOrSingle<XmlNode> findNodes(String xPathExpression, Map<String, String> namespaces) {
        try {
            final XPath xPath = XPATH_CACHE.get();
            xPath.reset();

            NodeList nodeSet;
            if (namespaces != null) {
                Map<String, String> fullNamespaces = addStandardNamespaces(namespaces);
                NamespaceContext namespaceContext = Convert.toNamespaceContext(fullNamespaces);
                xPath.setNamespaceContext(namespaceContext);
                nodeSet = (NodeList) xPath.evaluate(xPathExpression, Convert.toInputSource(new DOMSource(document)), NODESET);
            } else {
                nodeSet = (NodeList) xPath.evaluate(xPathExpression, document, NODESET);
            }

            return toListOrSingle(nodeSet);
        } catch (XPathExpressionException e) {
            throw XPathException.fromXPathException(e);
        }
    }

    private static Map<String, String> addStandardNamespaces(Map<String, String> namespaces) {
        Map<String, String> result = new HashMap<String, String>();
        for (String prefix: namespaces.keySet()) {
            String uri = namespaces.get(prefix);
            // according to the Javadocs only the constants defined in
            // XMLConstants are allowed as prefixes for the following
            // two URIs
            if (!XMLConstants.XML_NS_URI.equals(uri)
                    && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(uri)) {
                result.put(prefix, uri);
            }
        }
        result.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
        result.put(XMLConstants.XMLNS_ATTRIBUTE,
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI);

        return result;
    }

}
