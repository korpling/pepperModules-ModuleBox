package org.corpus_tools.peppermodules.hierarchyModules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SStructure;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.SToken;
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
		
		private List<String> hierarchy = null;
		
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			if (getDocument() == null) {
				throw new PepperModuleDataException(this, "Document is null.");
			}
			if (getDocument().getDocumentGraph() == null) {
				throw new PepperModuleDataException(this, "Document graph is null.");
			}
			HierarchizerProperties props = (HierarchizerProperties) getProperties(); 
			String structName = props.getStructAnnoName();
			String edgeType = props.getEdgeType();
			Map<String, String> defaultValues = props.getDefaultValues();
			hierarchy = props.getHierarchyNames();
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
			for (int i = hierarchySpans.size() - 1; i >= 0; i--) {
				String catName = hierarchy.get(i);
				Map<SToken, SStructure> tok2Struct = nextMap;
				nextMap = new HashMap<>();
				for (SSpan span : hierarchySpans.get(i)) {
					List<SToken> tokens = getDocument().getDocumentGraph().getOverlappedTokens(span);
					if (tokens.isEmpty()) {
						logger.warn("Span annotated for hierarchy level " + catName + " does not cover any token and is skipped.");
						continue;
					}
					Set<SStructuredNode> children = new HashSet<>();
					for (SToken tok : tokens) {
						SStructuredNode child = tok2Struct.get(tok);
						if (child != null) {
							children.add(child);
						}
					}
					if (children.isEmpty()) {
						// first level
						children.addAll(tokens);
					} 
					SStructure struct = getDocument().getDocumentGraph().createStructure(new ArrayList<>(children));					
					struct.createAnnotation(null, structName, defaultValues.containsKey(catName)? defaultValues.get(catName) : span.getAnnotation(catName).getValue());
					struct.getOutRelations().stream().filter((SRelation r) -> r instanceof SDominanceRelation).forEach((SRelation r) -> r.setType(edgeType));
					for (SToken tok : tokens) {
						nextMap.put(tok, struct);						
					}
					if (span.getAnnotations().size() == 1 && props.deleteSpanAnnotations()) {
						getDocument().getDocumentGraph().removeNode(span);
					}
				}
			}
			return (DOCUMENT_STATUS.COMPLETED);
		}
	}
}
