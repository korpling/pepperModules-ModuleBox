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

import org.corpus_tools.pepper.exceptions.PepperException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDocumentGraphObject;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Label;
import org.corpus_tools.salt.graph.impl.LabelImpl;
import org.corpus_tools.salt.util.internal.persistence.GraphMLWriter;
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
        final String type;
        final List<GMLData> data;

        protected GMLNode(String type, List<GMLData> data) {
            this.type = type;
            this.data = data;
        }
    }

    private static class GMLEdge {

        final String type;
        final String source;
        final String target;
        final List<GMLData> data;

        protected GMLEdge(String type, String source, String target, List<GMLData> data) {
            this.type = type;
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
            // find the type data element
            Optional<String> type = Optional.empty();
            ListIterator<GMLData> it = data.listIterator();
            while (it.hasNext()) {
                GMLData d = it.next();
                if ("salt::type".equals(d.key)) {
                    type = Optional.ofNullable(d.value);
                    it.remove();
                }
            }
            if (type.isPresent()) {
                GMLNode node = new GMLNode(type.get(), data);
                this.nodes.putIfAbsent(id, node);
            }
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
            Optional<String> type = Optional.empty();
            ListIterator<GMLData> it = data.listIterator();
            while (it.hasNext()) {
                GMLData d = it.next();
                if ("salt::type".equals(d.key)) {
                    type = Optional.ofNullable(d.value);
                    it.remove();
                }
            }
            if (type.isPresent()) {
                GMLEdge node = new GMLEdge(type.get(), source, target, data);
                this.edges.putIfAbsent(id, node);
            }
        }
    }

    private void mapDocument() {
        SDocument doc = SaltFactory.createSDocument();
        SDocumentGraph g = doc.createDocumentGraph();

        Map<String, SNode> id2node = new HashMap<>();
        for (Map.Entry<String, GMLNode> nodeEntry : this.nodes.entrySet()) {
            GMLNode node = nodeEntry.getValue();
            SALT_TYPE type = SALT_TYPE.valueOf(node.type);
            SNode obj;
            switch (type) {
            case SMEDIAL_DS:
                obj = SaltFactory.createSMedialDS();
                break;
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
        for (Map.Entry<String, GMLEdge> edgeEntry : this.edges.entrySet()) {
            GMLEdge edge = edgeEntry.getValue();
            SALT_TYPE type = SALT_TYPE.valueOf(edge.type);
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

        this.nodes.clear();
        this.edges.clear();

        documents.add(doc);
    }

    private Label createLabel(GMLKey key, String value) {
        Label result = new LabelImpl();
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