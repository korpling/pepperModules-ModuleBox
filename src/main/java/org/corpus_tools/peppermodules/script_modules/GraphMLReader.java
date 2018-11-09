/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.corpus_tools.peppermodules.script_modules;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.exceptions.PepperException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDocumentGraphObject;
import org.corpus_tools.salt.common.SMedialDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.core.impl.SFeatureImpl;
import org.corpus_tools.salt.graph.Label;
import org.corpus_tools.salt.graph.impl.LabelImpl;
import org.corpus_tools.salt.util.SaltUtil;
import org.corpus_tools.salt.util.internal.persistence.GraphMLWriter;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphMLReader {

    private final static Logger log = LoggerFactory.getLogger(GraphMLWriter.class);

    public final static String NS = "http://graphml.graphdrawing.org/xmlns";

    private final static XMLInputFactory xmlFactory = XMLInputFactory.newInstance();

    private final Map<String, GMLKey> keys = new LinkedHashMap<>();
    private final Map<String, GMLNode> nodes = new LinkedHashMap<>();
    private final Map<String, GMLEdge> edges = new LinkedHashMap<>();
    private final XMLStreamReader xml;
    private final List<SDocument> documents = new LinkedList<>();

    protected GraphMLReader(XMLStreamReader xml) {
        this.xml = xml;

    }

    private static enum GMLKeyFor {
        all, node, edge
    }

    private static class GMLKey {

        final GMLKeyFor forObj;
        final String qname;
        final String type;

        protected GMLKey(GMLKeyFor forObj, String qname, String type) {
            this.forObj = forObj;
            this.qname = qname;
            this.type = type;
        }
    }

    private static class GMLData {
        final String key;
        final String value;

        protected GMLData(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class GMLNode {
        final List<GMLData> data;

        protected GMLNode(List<GMLData> data) {
            this.data = data;
        }
    }

    private static class GMLEdge {

        final String source;
        final String target;
        final List<GMLData> data;

        protected GMLEdge(String source, String target, List<GMLData> data) {
            this.source = source;
            this.target = target;
            this.data = data;
        }
    }

    private void map() throws XMLStreamException {
        // read all elements and store the result in intermediate maps
        Optional<String> currentGraphID = Optional.empty();

        while (xml.hasNext()) {
            switch (xml.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                QName name = xml.getName();
                if (NS.equals(name.getNamespaceURI())) {
                    if ("key".equals(xml.getLocalName())) {
                        addKey();
                    } else if ("graph".equals(xml.getLocalName())) {

                        if (currentGraphID.isPresent()) {
                            // map the previous document
                            mapDocument();
                        }

                        currentGraphID = Optional.ofNullable(xml.getAttributeValue(null, "id"));
                        if (!"directed".equals(xml.getAttributeValue(null, "edgedefault"))) {
                            log.warn("GraphML edges are not directed for graph {} (but will be interpreted as such)",
                                    currentGraphID.toString());
                        }

                    } else if ("node".equals(xml.getLocalName())) {
                        mapNode();
                    } else if ("edge".equals(xml.getLocalName())) {
                        mapEdge();
                    }
                }
                break;
            }

            xml.next();
        }

        if (currentGraphID.isPresent()) {
            // map the last document
            mapDocument();
        }
    }

    private List<GMLData> parseData(String parentName) throws XMLStreamException {

        List<GMLData> result = new LinkedList<>();

        while (xml.hasNext()) {
            switch (xml.getEventType()) {
            case XMLStreamConstants.END_ELEMENT:
                if (parentName.equals(xml.getLocalName())) {
                    return result;
                }
                break;
            case XMLStreamConstants.START_ELEMENT:
                if ("data".equals(xml.getLocalName())) {
                    String key = xml.getAttributeValue(null, "key");
                    String value = xml.getElementText();
                    result.add(new GMLData(key, value));
                }
                break;
            }
            xml.next();
        }
        return result;
    }

    private void mapNode() throws XMLStreamException {
        String id = xml.getAttributeValue(null, "id");
        if (id != null) {
            // get all possible "data" sub-elements
            List<GMLData> data = parseData("node");
            GMLNode node = new GMLNode(data);
            this.nodes.putIfAbsent(id, node);

        }
    }

    private void mapEdge() throws XMLStreamException {
        String id = xml.getAttributeValue(null, "id");
        String source = xml.getAttributeValue(null, "source");
        String target = xml.getAttributeValue(null, "target");
        if (id != null && source != null && target != null) {
            // get all possible "data" sub-elements
            List<GMLData> data = parseData("edge");

            // find the type data element
            GMLEdge node = new GMLEdge(source, target, data);
            this.edges.putIfAbsent(id, node);
        }
    }

    private Optional<String> getType(List<GMLData> data) {
        // iterate over all items and the the "salt::type" entry
        for (GMLData d : data) {
            GMLKey k = keys.get(d.key);
            if (k != null && "salt::type".equals(k.qname)) {
                return Optional.ofNullable(d.value);
            }
        }
        return Optional.empty();
    }

    private void mapDocument() {
        SDocument doc = SaltFactory.createSDocument();
        SDocumentGraph g = doc.createDocumentGraph();

        Map<String, SNode> id2node = new HashMap<>();
        for (Map.Entry<String, GMLNode> nodeEntry : this.nodes.entrySet()) {
            GMLNode node = nodeEntry.getValue();
            Optional<String> typeRaw = getType(node.data);
            if (typeRaw.isPresent()) {
                SALT_TYPE type = SALT_TYPE.valueOf(typeRaw.get());
                SNode obj;
                switch (type) {
                case SMEDIAL_DS:
                    obj = SaltFactory.createSMedialDS();
                    break;
                case STIMELINE:
                    obj = SaltFactory.createSTimeline();
                case STEXTUAL_DS:
                    obj = SaltFactory.createSTextualDS();
                    break;
                case SSPAN:
                    obj = SaltFactory.createSSpan();
                    break;
                case SSTRUCTURE:
                    obj = SaltFactory.createSStructure();
                    break;
                case STOKEN:
                    obj = SaltFactory.createSToken();
                    break;
                default:
                    obj = null;
                }
                if (obj == null) {
                    log.warn("Can't create Salt object from type {}", type.toString());
                } else {
                    for (GMLData data : node.data) {
                        // get the corresponding key entry and parse the value according to the type of
                        // the key
                        GMLKey key = this.keys.get(data.key);
                        if (key != null && (key.forObj == GMLKeyFor.all || key.forObj == GMLKeyFor.node)) {
                            if ("salt::SNAME".equals(key.qname)) {
                                obj.setName(data.value);
                            } else if ("salt::id".equals(key.qname)) {
                                obj.setId(data.value);
                            } else if("salt::type".equals(key.qname)) {
                                // ignore
                            } else if ("salt::SDATA".equals(key.qname) && obj instanceof STimeline) {
                                // directly set the timeline end
                                ((STimeline) obj).increasePointOfTime(Integer.parseInt(data.value));
                            } else if("salt::SAUDIO_REFERENCE".equals(key.qname) && obj instanceof SMedialDS) {
                                ((SMedialDS) obj).setMediaReference(URI.createURI(data.value));
                            } else {
                                Label lbl = createLabel(key, data.value);
                                obj.addLabel(lbl);
                            }
                        }
                    }
                    g.addNode(obj);
                    id2node.put(nodeEntry.getKey(), obj);
                }
            }
        }
        for (Map.Entry<String, GMLEdge> edgeEntry : this.edges.entrySet()) {
            GMLEdge edge = edgeEntry.getValue();
            Optional<String> typeRaw = getType(edge.data);
            if(typeRaw.isPresent()) {
                SALT_TYPE type = SALT_TYPE.valueOf(typeRaw.get());
                SRelation obj;
                switch (type) {
                case SDOMINANCE_RELATION:
                    obj = SaltFactory.createSDominanceRelation();
                    break;
                case SMEDIAL_RELATION:
                    obj = SaltFactory.createSMedialRelation();
                    break;
                case SORDER_RELATION:
                    obj = SaltFactory.createSOrderRelation();
                    break;
                case SPOINTING_RELATION:
                    obj = SaltFactory.createSPointingRelation();
                    break;
                case SSPANNING_RELATION:
                    obj = SaltFactory.createSSpanningRelation();
                    break;
                case STEXTUAL_RELATION:
                    obj = SaltFactory.createSTextualRelation();
                    break;
                case STIMELINE_RELATION:
                    obj = SaltFactory.createSTimelineRelation();
                    break;
                default:
                    obj = null;
                }
                if (obj == null) {
                    log.warn("Can't create Salt object from type {}", type.toString());
                } else {
                    SNode source = id2node.get(edge.source);
                    SNode target = id2node.get(edge.target);
                    if (source != null && target != null) {
                        obj.setSource(source);
                        obj.setTarget(target);

                        for (GMLData data : edge.data) {
                            // get the corresponding key entry and parse the value according to the type of
                            // the key
                            GMLKey key = this.keys.get(data.key);
                            if (key != null && (key.forObj == GMLKeyFor.all || key.forObj == GMLKeyFor.edge)) {
                                if ("salt::SNAME".equals(key.qname)) {
                                    obj.setName(data.value);
                                } else if ("salt::id".equals(key.qname)) {
                                    obj.setId(data.value);
                                } else if("salt::type".equals(key.qname)) {
                                    // ignore
                                } else {
                                    Label lbl = createLabel(key, data.value);
                                    obj.addLabel(lbl);
                                }
                            }
                        }
                        g.addRelation(obj);
                    }
                }
            }
        }

        this.nodes.clear();
        this.edges.clear();

        documents.add(doc);
    }

    private Label createLabel(GMLKey key, String value) {

        Pair<String, String> splittedQName = SaltUtil.splitQName(key.qname);
        Label result;
        if("salt".equals(splittedQName.getLeft())) {
            // salt labels are in general features
            result = SaltFactory.createSFeature();
        } else {
            result = SaltFactory.createSAnnotation();
        }
        Object labelVal = null;
        switch (key.type.toLowerCase()) {
        case "boolean":
            labelVal = Boolean.parseBoolean(value);
            break;
        case "int":
            labelVal = Integer.parseInt(value);
            break;
        case "long":
            labelVal = Long.parseLong(value);
            break;
        case "float":
            labelVal = Float.parseFloat(value);
            break;
        case "double":
            labelVal = Double.parseDouble(value);
            break;
        case "string":
            labelVal = value;
            break;

        }

        result.setQName(key.qname);
        result.setValue(labelVal);
        return result;
    }

    private void addKey() {
        String id = xml.getAttributeValue(null, "id");
        String attrName = xml.getAttributeValue(null, "attr.name");
        String attFor = xml.getAttributeValue(null, "for");
        String attrType = xml.getAttributeValue(null, "attr.type");

        if (id != null && attrName != null && attrType != null) {
            GMLKeyFor keyType = GMLKeyFor.all;
            if ("node".equals(attFor)) {
                keyType = GMLKeyFor.node;
            } else if ("edge".equals(attFor)) {
                keyType = GMLKeyFor.edge;
            }

            GMLKey key = new GMLKey(keyType, attrName, attrType);
            keys.putIfAbsent(id, key);

        }
    }

    public static List<SDocument> convertToSalt(InputStream in) {

        try {
            // create some helper collections to store the parsed XML content

            // create the XML reader
            XMLStreamReader xml = xmlFactory.createXMLStreamReader(in);

            GraphMLReader reader = new GraphMLReader(xml);
            reader.map();

            return reader.documents;

        } catch (XMLStreamException ex) {
            throw new PepperException("GraphML reading excpetion", ex);
        }
    }

}