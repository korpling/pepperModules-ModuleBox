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
package org.corpus_tools.peppermodules.ridgesModules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.corpus_tools.peppermodules.ridgesModules.OrderRelationAdder.SOrderRelationMapper;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.util.Difference;
import org.junit.Before;
import org.junit.Test;

public class SOrderRelationAdderMapperTest {

	private SOrderRelationMapper fixture = null;

	public SOrderRelationMapper getFixture() {
		return fixture;
	}

	public void setFixture(SOrderRelationMapper fixture) {
		this.fixture = fixture;
	}

	@Before
	public void setUp() throws Exception {
		OrderRelationAdder adder = new OrderRelationAdder();
		setFixture(adder.new SOrderRelationMapper());
		getFixture().setProperties(new SOrderRelationAdderProperties());
	}

	/**
	 * Creates only one segmentation layer seg1
	 */
	@Test
	public void test_OneSegmentation() {
		SDocument sDoc = SaltFactory.createSDocument();
		getFixture().setDocument(sDoc);
		sDoc.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS primData = sDoc.getDocumentGraph().createTextualDS("A sample text");
		SToken t1 = sDoc.getDocumentGraph().createToken(primData, 0, 1);
		SToken t2 = sDoc.getDocumentGraph().createToken(primData, 2, 8);
		SToken t3 = sDoc.getDocumentGraph().createToken(primData, 9, 13);

		SSpan s1 = sDoc.getDocumentGraph().createSpan(t1);
		s1.createAnnotation(null, "seg1", "");
		SSpan s2 = sDoc.getDocumentGraph().createSpan(t2);
		s2.createAnnotation(null, "seg1", "");
		SSpan s3 = sDoc.getDocumentGraph().createSpan(t3);
		s3.createAnnotation(null, "seg1", "");

		getFixture().getProperties().setPropertyValue(SOrderRelationAdderProperties.PROP_SEGMENTATION_LAYERS, "seg1");

		getFixture().mapSDocument();

		assertNotNull(sDoc.getDocumentGraph().getOrderRelations());
		assertEquals(2, sDoc.getDocumentGraph().getOrderRelations().size());

		assertEquals(1, sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(0) instanceof SOrderRelation);
		assertTrue(((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(0)).getType().equals("seg1"));

		assertEquals(1, sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).get(0) instanceof SOrderRelation);
		assertTrue(((SRelation) sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).get(0)).getType().equals("seg1"));
	}

	/**
	 * Creates two segmentation layer seg1 and seg2 on the same span
	 */
	@Test
	public void test_TwoSegmentation() {
		SDocument sDoc = SaltFactory.createSDocument();
		getFixture().setDocument(sDoc);
		sDoc.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS primData = sDoc.getDocumentGraph().createTextualDS("A sample text");
		SToken t1 = sDoc.getDocumentGraph().createToken(primData, 0, 1);
		SToken t2 = sDoc.getDocumentGraph().createToken(primData, 2, 8);
		SToken t3 = sDoc.getDocumentGraph().createToken(primData, 9, 13);

		SSpan s1 = sDoc.getDocumentGraph().createSpan(t1);
		s1.createAnnotation(null, "seg1", "");
		s1.createAnnotation(null, "seg2", "");
		SSpan s2 = sDoc.getDocumentGraph().createSpan(t2);
		s2.createAnnotation(null, "seg1", "");
		s2.createAnnotation(null, "seg2", "");
		SSpan s3 = sDoc.getDocumentGraph().createSpan(t3);
		s3.createAnnotation(null, "seg1", "");
		s3.createAnnotation(null, "seg2", "");

		getFixture().getProperties().setPropertyValue(SOrderRelationAdderProperties.PROP_SEGMENTATION_LAYERS, "seg1, seg2");

		getFixture().mapSDocument();

		assertNotNull(sDoc.getDocumentGraph().getOrderRelations());
		assertEquals(4, sDoc.getDocumentGraph().getOrderRelations().size());

		assertEquals(2, sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(0) instanceof SOrderRelation);
		SRelation rel1 = ((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(0));
		SRelation rel2 = ((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(1));
		if (rel1.getType().equals("seg1")) {
			assertTrue(rel2.getType().equals("seg2"));
		} else if (rel1.getType().equals("seg2")) {
			assertTrue(rel2.getType().equals("seg1"));
		} else {
			fail();
		}

		assertEquals(2, sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).get(0) instanceof SOrderRelation);
		SRelation rel3 = ((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(0));
		SRelation rel4 = ((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(1));
		if (rel3.getType().equals("seg1")) {
			assertTrue(rel4.getType().equals("seg2"));
		} else if (rel3.getType().equals("seg2")) {
			assertTrue(rel4.getType().equals("seg1"));
		} else {
			fail();
		}
	}

	/**
	 * Creates two segmentation layer seg1 and seg2 on different spans.
	 */
	@Test
	public void test_TwoSegmentationsTwoSpans() {
		SDocument sDoc = SaltFactory.createSDocument();
		getFixture().setDocument(sDoc);
		sDoc.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS primData = sDoc.getDocumentGraph().createTextualDS("A sample text");
		SToken t1 = sDoc.getDocumentGraph().createToken(primData, 0, 1);
		SToken t2 = sDoc.getDocumentGraph().createToken(primData, 2, 8);
		SToken t3 = sDoc.getDocumentGraph().createToken(primData, 9, 13);

		SSpan s1 = sDoc.getDocumentGraph().createSpan(t1);
		s1.createAnnotation(null, "seg1", "");
		SSpan s2 = sDoc.getDocumentGraph().createSpan(t2);
		s2.createAnnotation(null, "seg1", "");
		SSpan s3 = sDoc.getDocumentGraph().createSpan(t3);
		s3.createAnnotation(null, "seg1", "");

		SSpan s4 = sDoc.getDocumentGraph().createSpan(t1);
		s4.createAnnotation(null, "seg2", "");
		SSpan s5 = sDoc.getDocumentGraph().createSpan(t2);
		s5.createAnnotation(null, "seg2", "");
		SSpan s6 = sDoc.getDocumentGraph().createSpan(t3);
		s6.createAnnotation(null, "seg2", "");

		getFixture().getProperties().setPropertyValue(SOrderRelationAdderProperties.PROP_SEGMENTATION_LAYERS, "seg1, seg2");

		getFixture().mapSDocument();

		assertNotNull(sDoc.getDocumentGraph().getOrderRelations());
		assertEquals(4, sDoc.getDocumentGraph().getOrderRelations().size());

		assertEquals(1, sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(0) instanceof SOrderRelation);
		assertTrue(((SRelation) sDoc.getDocumentGraph().getRelations(s1.getId(), s2.getId()).get(0)).getType().equals("seg1"));

		assertEquals(1, sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).get(0) instanceof SOrderRelation);
		assertTrue(((SRelation) sDoc.getDocumentGraph().getRelations(s2.getId(), s3.getId()).get(0)).getType().equals("seg1"));

		assertEquals(1, sDoc.getDocumentGraph().getRelations(s4.getId(), s5.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s4.getId(), s5.getId()).get(0) instanceof SOrderRelation);
		assertTrue(((SRelation) sDoc.getDocumentGraph().getRelations(s4.getId(), s5.getId()).get(0)).getType().equals("seg2"));

		assertEquals(1, sDoc.getDocumentGraph().getRelations(s5.getId(), s6.getId()).size());
		assertTrue((SRelation) sDoc.getDocumentGraph().getRelations(s5.getId(), s6.getId()).get(0) instanceof SOrderRelation);
		assertTrue(((SRelation) sDoc.getDocumentGraph().getRelations(s5.getId(), s6.getId()).get(0)).getType().equals("seg2"));
	}

	/**
	 * Tests when no annotation names are given for segmentation, create order
	 * relations for tokens
	 */
	@Test
	public void test_OnlyTokens() {
		SDocument sDoc = SaltFactory.createSDocument();
		getFixture().setDocument(sDoc);
		sDoc.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS primData1 = sDoc.getDocumentGraph().createTextualDS("A sample text");
		sDoc.getDocumentGraph().createToken(primData1, 0, 1);
		sDoc.getDocumentGraph().createToken(primData1, 2, 8);
		sDoc.getDocumentGraph().createToken(primData1, 9, 13);

		STextualDS primData2 = sDoc.getDocumentGraph().createTextualDS("Another one");
		sDoc.getDocumentGraph().createToken(primData2, 0, 7);
		sDoc.getDocumentGraph().createToken(primData2, 8, 11);

		STextualDS primData3 = sDoc.getDocumentGraph().createTextualDS("And number three");
		sDoc.getDocumentGraph().createToken(primData3, 0, 3);
		sDoc.getDocumentGraph().createToken(primData3, 4, 10);
		sDoc.getDocumentGraph().createToken(primData3, 11, 16);

		// create template
		SDocument template = SaltFactory.createSDocument();
		getFixture().setDocument(template);
		template.setDocumentGraph(SaltFactory.createSDocumentGraph());
		STextualDS text1 = template.getDocumentGraph().createTextualDS("A sample text");
		STextualDS text2 = template.getDocumentGraph().createTextualDS("Another one");
		STextualDS text3 = template.getDocumentGraph().createTextualDS("And number three");

		SToken t1 = template.getDocumentGraph().createToken(text1, 0, 1);
		SToken t2 = template.getDocumentGraph().createToken(text1, 2, 8);
		template.getDocumentGraph().createRelation(t1, t2, SALT_TYPE.SORDER_RELATION, null).setType("text1");
		;
		SToken t3 = template.getDocumentGraph().createToken(text1, 9, 13);
		template.getDocumentGraph().createRelation(t2, t3, SALT_TYPE.SORDER_RELATION, null).setType("text1");
		;

		SToken t4 = template.getDocumentGraph().createToken(text2, 0, 7);
		SToken t5 = template.getDocumentGraph().createToken(text2, 8, 11);
		template.getDocumentGraph().createRelation(t4, t5, SALT_TYPE.SORDER_RELATION, null).setType("text2");

		SToken t6 = template.getDocumentGraph().createToken(text3, 0, 3);
		SToken t7 = template.getDocumentGraph().createToken(text3, 4, 10);
		template.getDocumentGraph().createRelation(t6, t7, SALT_TYPE.SORDER_RELATION, null).setType("text3");
		SToken t8 = template.getDocumentGraph().createToken(text3, 11, 16);
		template.getDocumentGraph().createRelation(t7, t8, SALT_TYPE.SORDER_RELATION, null).setType("text3");

		getFixture().mapSDocument();

		Set<Difference> diffs = template.getDocumentGraph().findDiffs(sDoc.getDocumentGraph());
		assertEquals(0, diffs.size());
	}
}
