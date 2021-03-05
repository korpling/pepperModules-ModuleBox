package org.corpus_tools.peppermodules.hierarchyModules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "HierarchizerComponent", factory = "PepperManipulatorComponentFactory")
public class Hierarchizer extends PepperManipulatorImpl{
	private static final Logger logger = LoggerFactory.getLogger(Hierarchizer.class);
	public Hierarchizer() {
		super();
		this.setName("Hierarchizer");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setDesc("This module converts multiple non-hierarchical span-annotations into a hierarchy of structures.");
		this.setProperties(new HierarchizerProperties());
	}
	
	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {		
		return new HierarchyMapper();
	}

	public class HierarchyMapper extends PepperMapperImpl implements PepperMapper {
		
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			if (getDocument() == null) {
				throw new PepperModuleDataException(this, "Document is null.");
			}
			if (getDocument().getDocumentGraph() == null) {
				throw new PepperModuleDataException(this, "Document graph is null.");
			}
			/* read properties */
			HierarchizerProperties props = (HierarchizerProperties) getProperties(); 
			String structName = props.getStructAnnoName();
			String edgeType = props.getEdgeType();
			SLayer layer = SaltFactory.createSLayer();
			String layerName = props.getLayerName();
			if (layerName != null) {
				layer.setName(props.getLayerName());
				layer.setGraph(getDocument().getDocumentGraph());
			}
			Map<String, String> defaultValues = props.getDefaultValues();
			List<String> hierarchy = props.getHierarchyNames();
			Set<String> namesAsEdges;
			Map<SStructuredNode, SAnnotation> struct2EdgeAnno;
			{
				namesAsEdges = props.getEdgeNames();
				struct2EdgeAnno = new HashMap<>();
			}
			/* basic functionality: build structures */
			List<List<SSpan>> hierarchySpans = new ArrayList<List<SSpan>>();
			for (String name : hierarchy) {
				hierarchySpans.add(new ArrayList<>());
				for (SSpan span : getDocument().getDocumentGraph().getSpans()) {				
					if (span.containsLabel(name)) {
						hierarchySpans.get(hierarchySpans.size() - 1).add(span);
					}
				}
			}
			Map<SToken, SStructure> nextMap = new HashMap<>();
			Set<SStructure> createdStructs = new HashSet<>();
			List<SAnnotation> structAnnos = new ArrayList<>();
			for (int i = hierarchySpans.size() - 1; i >= 0; i--) {
				String catName = hierarchy.get(i);
				Map<SToken, SStructure> tok2Struct = nextMap;
				nextMap = new HashMap<>();
				if (hierarchySpans.get(i).isEmpty()) {
					logger.info("No spans for hierarchy level " + catName + " in document " + getDocument().getName() + ".");
				}
				for (SSpan span : hierarchySpans.get(i)) {
					List<SToken> tokens = getDocument().getDocumentGraph().getSortedTokenByText( getDocument().getDocumentGraph().getOverlappedTokens(span) );
					if (tokens.isEmpty()) {
						logger.warn("Span annotated for hierarchy level " + catName + " does not cover any token and is skipped.");
						continue;
					}
					Set<SStructuredNode> children = new LinkedHashSet<>();
					for (SToken tok : tokens) {
						SStructuredNode child = tok2Struct.get(tok);
						children.add(child == null? tok : child);
						if (layerName != null) { // add tokens to layer, too
							tok.addLayer(layer);
						}
					}
					if (children.isEmpty()) {
						// first level
						children.addAll(tokens);
					}
					if (!namesAsEdges.contains(catName)) {
						SStructure struct = getDocument().getDocumentGraph().createStructure(new ArrayList<>(children));
						createdStructs.add(struct);
						structAnnos.add( struct.createAnnotation(null, structName, defaultValues.containsKey(catName)? defaultValues.get(catName) : span.getAnnotation(catName).getValue()) );
						struct.getOutRelations().stream().filter((SRelation r) -> r instanceof SDominanceRelation).forEach((SRelation r) -> r.setType(edgeType));
						for (SToken tok : tokens) {
							nextMap.put(tok, struct);						
						}
						if (span.getAnnotations().size() == 1 && props.deleteSpanAnnotations()) {
							getDocument().getDocumentGraph().removeNode(span);
						}
						if (layerName != null) {
							struct.addLayer(layer);
							struct.getOutRelations().stream().forEach(layer::addRelation);
						}
						if (struct2EdgeAnno != null && struct2EdgeAnno.containsKey(struct)) {
							SAnnotation anno = struct2EdgeAnno.get(struct);
							for (SRelation r : struct.getOutRelations().stream().filter((SRelation r) -> r instanceof SDominanceRelation).collect(Collectors.toList())) {
								r.createAnnotation(anno.getNamespace(), anno.getName(), anno.getValue());
							}
						}
					} else {
						nextMap = tok2Struct;
						for (SStructuredNode child : children) {
							SAnnotation anno = SaltFactory.createSAnnotation();
							anno.setName(catName);
							anno.setValue(defaultValues.containsKey(catName)? defaultValues.get(catName) : span.getAnnotation(catName).getValue());
							struct2EdgeAnno.put(child, anno);
						}
					}
				}
			}
			/* clean out empty structs */
			for (SStructure struct : createdStructs) {
				if (getDocument().getDocumentGraph().getOverlappedTokens(struct).isEmpty()) {
					getDocument().getDocumentGraph().removeNode(struct);
				}
			}
			/* build and annotate text root if required */
			if (props.treeifyForest()) {
				buildTextRoot(layer, edgeType, structName, props.getRootValue());
			}
			/* assign secondary relations */			
			if (props.hasPointers()) {
				buildSecondaryRelations(layer, structAnnos, props.getPointerMarker(), props.getPointerType());
			};
			return (DOCUMENT_STATUS.COMPLETED);
		}
		
		private void buildTextRoot(SLayer layer, String edgeType, String structAnnoName, String rootValue) {
			List<SStructuredNode> structRoots = getDocument().getDocumentGraph().getRoots().stream()
					.filter((SNode n) -> n instanceof SStructure)
					.map((SNode n) -> (SStructuredNode) n)
					.collect(Collectors.toList());
			SStructure root = getDocument().getDocumentGraph().createStructure(structRoots);
			root.addLayer(layer);
			root.createAnnotation(null, structAnnoName, rootValue);
			root.getOutRelations().stream().filter((SRelation r) -> r instanceof SDominanceRelation).forEach((SRelation r) -> r.setType(edgeType));
		}
		
		private void buildSecondaryRelations(SLayer layer, List<SAnnotation> structAnnos, String marker, String pointerType) {
			List<SAnnotation> markedAnnotations = structAnnos.stream().filter((SAnnotation a) -> a.getValue_STEXT().contains(marker)).collect(Collectors.toList());
			Map<String, List<SStructuredNode>> id2Nodes = getMatchingNodes(markedAnnotations, marker);
			for (List<SStructuredNode> markedNodes : id2Nodes.values()) {
				for (int i=1; i < markedNodes.size(); i++) {
					SRelation r = getDocument().getDocumentGraph().createRelation(markedNodes.get(i - 1), markedNodes.get(i), SALT_TYPE.SDOMINANCE_RELATION, null);
					r.setType(pointerType);
					r.addLayer(layer);
				}
			}
		}
		
		private Map<String, List<SStructuredNode>> getMatchingNodes(List<SAnnotation> markedAnnotations, String marker) {
			Map<String, List<SStructuredNode>> id2Nodes = new HashMap<>();
			for (SAnnotation anno : markedAnnotations) {
				String value = anno.getValue_STEXT();
				int index = value.lastIndexOf(marker);
				String id = value.substring(index);
				id = id.contains(" ")? id.substring(0, id.indexOf(" ")) : id;
				if (!id2Nodes.containsKey(id)) {
					id2Nodes.put(id, new ArrayList<>());
				}
				id2Nodes.get(id).add((SStructuredNode) anno.getContainer());
				unmarkAnnotation(anno, id);
			}
			return id2Nodes;
		}
		
		private void unmarkAnnotation(SAnnotation anno, String marker) {
			if (anno == null) {
				return;
			}
			String value = anno.getValue_STEXT();
			anno.setValue(value.replace(marker, "").trim());
		}
	}	
}
