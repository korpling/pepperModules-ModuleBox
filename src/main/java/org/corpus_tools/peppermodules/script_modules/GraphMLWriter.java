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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Label;
import org.corpus_tools.salt.graph.LabelableElement;
import org.corpus_tools.salt.graph.Node;
import org.corpus_tools.salt.graph.Relation;
import org.eclipse.emf.common.util.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class GraphMLWriter {
	private final static Logger log = LoggerFactory.getLogger(GraphMLWriter.class);

	public final static String NS = "http://graphml.graphdrawing.org/xmlns";

	public static void convertFromSalt(OutputStream out, SDocument... docs) {
		writeDocuments(out, Arrays.asList(docs));
	}

	public static void writeDocuments(OutputStream out, List<SDocument> docs) {
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		try {
			XMLStreamWriter w = factory.createXMLStreamWriter(out);
			w.setDefaultNamespace(NS);

			w.writeStartDocument();

			w.writeStartElement(NS, "graphml");
			w.writeDefaultNamespace(NS);
			w.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			w.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
					NS + " http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd");

			IDManager ids = new IDManager();

			// we always use the "salt::type" label
			w.writeStartElement(NS, "key");

			w.writeAttribute("id", ids.getID(new AttributeSignature("salt::type", "string")));
			w.writeAttribute("attr.name", "salt::type");
			w.writeAttribute("for", "all");
			w.writeAttribute("attr.type", "string");
			w.writeEndElement();

			if (docs != null) {
				// first get all possible label names and write them as "key"
				for (SDocument doc : docs) {
					SDocumentGraph g = doc.getDocumentGraph();
					if (g != null) {
						writeKeys(w, g.getNodes(), ids);
						writeKeys(w, g.getRelations(), ids);
						writeKeys(w, g.getTextualDSs(), ids);
						writeKeys(w, g.getMedialDSs(), ids);
						if (g.getTimeline() != null) {
							List<STimeline> timeline = new LinkedList<>();
							timeline.add(g.getTimeline());
							writeKeys(w, timeline, ids);
						}
					}
				}
				// write the actual graphs
				for (SDocument d : docs) {
					writeSDocumentGraph(w, d.getDocumentGraph(), ids, true);
				}
			}
			w.writeEndDocument();
			w.close();

			out.flush();
		} catch (XMLStreamException | IOException ex) {
			log.error("Could not write GraphML", ex);
		}

	}

	private static void writeKeys(XMLStreamWriter w, Collection<? extends LabelableElement> elements, IDManager ids)
			throws XMLStreamException {
		if (elements != null && !elements.isEmpty()) {
			for (LabelableElement e : elements) {
				Collection<Label> labels = e.getLabels();
				if (labels != null && !labels.isEmpty()) {
					for (Label l : labels) {

						Object o = l.getValue();
						String type = getType(o);
						if (type != null) {
							AttributeSignature attSignature = new AttributeSignature(l.getQName(), type);
							// don't write a signature twice
							if (!ids.hasID(attSignature)) {
								String id = ids.getID(attSignature);
								w.writeStartElement(NS, "key");
								w.writeAttribute("id", id);
								w.writeAttribute("attr.name", l.getQName());
								w.writeAttribute("for", "all");
								w.writeAttribute("attr.type", type);

								w.writeEndElement();
							}
						}
					}
				}
			}
		}
	}

	private static String getType(Object o) {
		String type = null;
		if (o instanceof Boolean) {
			type = "boolean";
		} else if (o instanceof Integer) {
			type = "int";
		} else if (o instanceof Long) {
			type = "long";
		} else if (o instanceof Float) {
			type = "float";
		} else if (o instanceof Double) {
			type = "double";
		} else if (o instanceof String) {
			type = "string";
		} else if (o instanceof URI) {
			type = "string";
		}
		return type;
	}

	private static void writeLabels(XMLStreamWriter w, Collection<Label> labels, IDManager ids)
			throws XMLStreamException {
		if (labels != null && !labels.isEmpty()) {
			for (Label l : labels) {
				AttributeSignature key = new AttributeSignature(l.getQName(), getType(l.getValue()));
				w.writeStartElement(NS, "data");
				w.writeAttribute("key", ids.getID(key));
				w.writeCharacters("" + l.getValue());
				w.writeEndElement();
			}
		}
	}

	/**
	 * Writes a data element describing the type of the object.
	 * 
	 * @param w
	 * @param o
	 * @throws XMLStreamException
	 */
	private static void writeType(XMLStreamWriter w, Object o, IDManager ids) throws XMLStreamException {
		Set<SALT_TYPE> saltTypes = SALT_TYPE.class2SaltType(o.getClass());

		if (!saltTypes.isEmpty()) {
			// find the most specific type
			SALT_TYPE mostSpecificType = null;
			for (SALT_TYPE type : saltTypes) {
				if (mostSpecificType == null) {
					mostSpecificType = type;
				} else {
					final Class<?> A = mostSpecificType.getJavaType();
					final Class<?> B = type.getJavaType();

					// Check if this type (B) is more specific as the current
					// most specific type (A).
					// Thus "B superclass of A" is ok, but not "A superclass of
					// B".
					if (A.isAssignableFrom(B)) {
						mostSpecificType = type;
					}
				}
			}

			if (mostSpecificType != null) {
				AttributeSignature attSign = new AttributeSignature("salt::type", "string");
				w.writeStartElement(NS, "data");
				w.writeAttribute("key", ids.getID(attSign));
				w.writeCharacters(mostSpecificType.name());
				w.writeEndElement();
			}
		}
	}

	private static void writeNode(XMLStreamWriter w, Node c, IDManager ids) throws XMLStreamException {
		w.writeStartElement(NS, "node");
		w.writeAttribute("id", ids.getID(c));

		writeType(w, c, ids);
		writeLabels(w, c.getLabels(), ids);
		w.writeEndElement();
	}

	private static void writeEdge(XMLStreamWriter w, Relation r, IDManager ids) throws XMLStreamException {
		w.writeStartElement(NS, "edge");
		w.writeAttribute("id", ids.getID(r));
		w.writeAttribute("source", ids.getID(r.getSource()));
		w.writeAttribute("target", ids.getID(r.getTarget()));

		writeType(w, r, ids);
		writeLabels(w, r.getLabels(), ids);

		w.writeEndElement();
	}

	private static void writeSDocumentGraph(XMLStreamWriter w, SDocumentGraph g, IDManager ids,
			boolean includeDocLabels) throws XMLStreamException {
		if (g == null) {
			return;

		}
		List<SNode> nodes = g.getNodes();
		List<SRelation<SNode, SNode>> relations = g.getRelations();
		// graphs without nodes are not allowed
		if (nodes != null && !nodes.isEmpty()) {
			w.writeStartElement(NS, "graph");
			w.writeAttribute("id", ids.getID(g));
			w.writeAttribute("edgedefault", "directed");

			if (includeDocLabels && g.getDocument() != null) {
				writeLabels(w, g.getDocument().getLabels(), ids);
			}

			for (SNode n : nodes) {
				writeNode(w, n, ids);
			}

			if (relations != null) {
				for (SRelation e : relations) {
					writeEdge(w, e, ids);
				}
			}

			w.writeEndElement();
		}
	}

	private static class AttributeSignature {
		final String qname;
		final String type;

		public AttributeSignature(String qname, String type) {
			this.qname = qname;
			this.type = type;
		}

		@Override
		public int hashCode() {
			return Objects.hash(qname, type);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof AttributeSignature) {
				AttributeSignature other = (AttributeSignature) obj;
				return Objects.equals(this.qname, other.qname) && Objects.equals(this.type, other.type);
			} else {
				return false;
			}
		}
	}

	private static class IDManager {
		private final AtomicLong counter = new AtomicLong(0);

		private final Map<Object, String> existing = new HashMap<>();

		public boolean hasID(Object e) {
			String id = existing.get(e);
			return id != null;
		}

		public String getID(Object e) {

			String id = existing.get(e);
			if (id == null) {
				id = "_" + counter.getAndIncrement();
				existing.put(e, id);
			}
			return id;
		}

	}
}