package org.corpus_tools.peppermodules.hierarchyModules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.corpus_tools.peppermodules.hierarchyModules.Hierarchizer.HierarchyMapper;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SDominanceRelation;
import org.corpus_tools.salt.common.SStructuredNode;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.util.SaltUtil;
import org.junit.Before;
import org.junit.Test;

public class HierarchizerTest {
	private Hierarchizer fixture = null;
	private SDocument sourceDocument = null;
	private SDocument targetDocument = null;
	private static final String[] BASE_TOKENS = {"This", "is", "a", "test", ".", "Very", "original", "."};
	private static final String[] POS = {"PRON", "VERB", "DET", "NOUN", "PUNCT", "ADV", "ADJ", "PUNCT"};
	private static final int[][] CATS = {{0}, {1, 2, 3}, {4}, {5, 6}, {7}};
	private static final String[] CAT_VALUES = {"SUBJ", "PRED", "PUNCT", "PRED", "PUNCT"};
	private static final int[][] SENTENCES = {{0, 1, 2}, {3, 4}};
	private static final String SENTENCE_VALUE = "ROOT";
	private static final String SENTENCE_NAME = "sentence";
	private static final String POS_NAME = "pos";
	private static final String CAT_NAME = "cat";
	private static final String STRUCT_ANNO_NAME = CAT_NAME;
	
	@Before
	public void setFixture() {
		fixture = new Hierarchizer();
		HierarchizerProperties props = new HierarchizerProperties();
		props.setPropertyValue(props.PROP_HIERARCHY_NAMES, StringUtils.join(new String[]{SENTENCE_NAME, CAT_NAME, POS_NAME}, ","));
		props.setPropertyValue(props.PROP_STRUCT_ANNO_NAME, STRUCT_ANNO_NAME);
		fixture.setProperties(props);
	}
	
	public Hierarchizer getFixture() {
		return this.fixture;
	}
	
	@Test
	public void testHierarchizer() {
		SDocument doc = SaltFactory.createSDocument();
		doc.setName("testDocument");
		SDocument targetDoc = SaltFactory.createSDocument();
		SDocumentGraph graph = doc.createDocumentGraph();
		SDocumentGraph targetGraph = targetDoc.createDocumentGraph();
		List<SToken> tokens = graph.createTextualDS(StringUtils.join(BASE_TOKENS, " ")).tokenize();
		List<SToken> targetTokens = targetGraph.createTextualDS(StringUtils.join(BASE_TOKENS, " ")).tokenize();
		String edgeType = ((HierarchizerProperties) getFixture().getProperties()).getEdgeType();
		for (int[] s : SENTENCES) {
			List<SToken> sentenceTokens = new ArrayList<>();
			List<SStructuredNode> outerStructs = new ArrayList<>();
			for (int k : s) {
				graph.createSpan(tokens.subList(CATS[k][0], CATS[k][CATS[k].length - 1] + 1)).createAnnotation(null, CAT_NAME, CAT_VALUES[k]);
				List<SStructuredNode> structures = new ArrayList<>();				
				for (int j : CATS[k]) {		
					graph.createSpan(tokens.get(j)).createAnnotation(null, POS_NAME, POS[j]);
					sentenceTokens.add(tokens.get(j));
					structures.add(targetGraph.createStructure(targetTokens.get(j)));
					structures.get(structures.size() - 1).createAnnotation(null, STRUCT_ANNO_NAME, POS[j]);
				}
				setTypeForStructs(structures, edgeType);
				outerStructs.add(targetGraph.createStructure(structures));
				outerStructs.get(outerStructs.size() - 1).createAnnotation(null, STRUCT_ANNO_NAME, CAT_VALUES[k]);
			}
			setTypeForStructs(outerStructs, edgeType);
			graph.createSpan(sentenceTokens).createAnnotation(null, SENTENCE_NAME, SENTENCE_VALUE);
			targetGraph.createStructure(outerStructs).createAnnotation(null, STRUCT_ANNO_NAME, SENTENCE_VALUE);
		}
		sourceDocument = doc;
		targetDocument = targetDoc;
		HierarchyMapper mapper = (HierarchyMapper) getFixture().createPepperMapper(sourceDocument.getIdentifier());
		mapper.setProperties(getFixture().getProperties());
		mapper.setDocument(sourceDocument);
		mapper.mapSDocument();
		SDocumentGraph fixGraph = mapper.getDocument().getDocumentGraph();
		assertEquals(fixGraph.findDiffs(targetDocument.getDocumentGraph()).size(), 0);
	}
	
	private void setTypeForStructs(List<SStructuredNode> structures, String edgeType) {
		for (SStructuredNode structNode : structures) {
			structNode.getOutRelations().stream().filter((SRelation r) -> r instanceof SDominanceRelation).forEach((SRelation r) -> r.setType(edgeType));
		}
	}
}
