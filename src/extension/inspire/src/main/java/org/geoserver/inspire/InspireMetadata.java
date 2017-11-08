/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_NAMESPACE;

import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;

import org.geoserver.ExtendedCapabilitiesProvider.Translator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.helpers.NamespaceSupport;

public enum InspireMetadata {
    CREATE_EXTENDED_CAPABILITIES("inspire.createExtendedCapabilities"),
    LANGUAGE("inspire.language"), 
    SERVICE_METADATA_URL("inspire.metadataURL"), 
    SERVICE_METADATA_TYPE("inspire.metadataURLType"), 
    SERVICE_METADATA_HARDCODED_TEXT("inspire.metadataHardcodedTextService"),
    SPATIAL_DATASET_IDENTIFIER_TYPE("inspire.spatialDatasetIdentifier");

    public String key;

    private InspireMetadata(String key) {
        this.key = key;
    }
    
    // Get the root DOM Element of the specified XML string.
    @SuppressWarnings("rawtypes")
    private static Element parseDomElement(NamespaceSupport namespaces, String xmlrawText) {
        String xml = "<rootTempDomNode ";
        
        // namespace declarations
        if (namespaces != null) {
            for (Enumeration declaredPrefixes = namespaces.getDeclaredPrefixes(); declaredPrefixes.hasMoreElements();) {
                String prefix = (String) declaredPrefixes.nextElement();
                String uri = namespaces.getURI(prefix);
                xml += "xmlns:";
                xml += prefix;
                xml += "='";
                xml += uri;
                xml += "' ";
            }
        }
        xml += ">";
        xml += xmlrawText;
        xml += "</rootTempDomNode>";
        
        SAXBuilder builder = new SAXBuilder(false);
        try {
            Document doc = builder.build(new StringReader(xml));
            return doc.getRootElement();
        } catch (JDOMException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
    
    // Write the specified DOM Element to a Capabilites translator instance.
    private static Boolean writeDomElement(Translator translator, Element element) {
        if (element!=null) {
            Boolean written = false;
            
            String name = element.getName();
            String prefix = element.getNamespacePrefix();
            if (prefix != null && prefix.length() > 0) name = prefix + ":" + name;
            String text = element.getText();
            written = true;
            
            translator.start(name);
            if (text != null && text.length() > 0) translator.chars(text);
            for (Object child : element.getChildren()) written |= writeDomElement(translator, (Element)child);
            translator.end(name);
            
            return written;
        }
        return false;
    }
    // Write the specified XML string to a Capabilites translator instance.
    public static Boolean writeDomElement(Translator translator, NamespaceSupport namespaces, String xmlrawText) {
        Element element = parseDomElement(namespaces, xmlrawText);
        if (element!=null) {
            Boolean written = false;
            for (Object child : element.getChildren()) written |= writeDomElement(translator, (Element)child);
            return written;
        }
        return false;
    }
    // Write the specified XML string to a Capabilites translator instance.
    public static Boolean writeDomElement(Translator translator, String xmlrawText) {
        NamespaceSupport namespaces = new NamespaceSupport();
        namespaces.declarePrefix("inspire_common", COMMON_NAMESPACE);
        namespaces.declarePrefix("inspire_vs", VS_NAMESPACE);
        namespaces.declarePrefix("inspire_dls", DLS_NAMESPACE);
        
        return writeDomElement(translator, namespaces, xmlrawText);
    }
}
