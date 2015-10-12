/**
 * Copyright 2009 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package org.corpus_tools.peppermodules.ridgesModules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SMedialDS;
import org.corpus_tools.salt.common.SMedialRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SAnnotationContainer;
import org.corpus_tools.salt.core.SFeature;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.graph.LabelableElement;
import org.corpus_tools.salt.graph.Relation;
import org.corpus_tools.salt.util.SaltUtil;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts all primary text tokens to spans of an newly created artifical
 * primary text which represents the timeline.
 * 
 * @author Thomas Krause
 * @version 1.0
 * 
 */
@Component(name = "Timeline2TokenComponent", factory = "PepperManipulatorComponentFactory")
public class Timeline2Token extends PepperManipulatorImpl {
	private static final Logger logger = LoggerFactory.getLogger(Timeline2Token.class);

	public Timeline2Token() {
		super();
	    setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-RidgesModules"));
		setDesc("The Timeline2Token manipulator converts all primary text tokens to spans of an newly created artifical primary text which represents the timeline. ");
		// setting name of module
		this.setName("Timeline2Token");
		setProperties(new Timeline2TokenProperties());
	}

	public class Timeline2TokenProperties extends PepperModuleProperties {
		private final static String PROP_NUMBER_ARTIFICIAL_TOKEN = "number-artificial-token";

		public Timeline2TokenProperties() {
			this.addProperty(new PepperModuleProperty<Boolean>(PROP_NUMBER_ARTIFICIAL_TOKEN, Boolean.class, "???", Boolean.TRUE, false));
		}

		public boolean getNumberArtificialTokens() {
			boolean numberArtificialTokens = "true".equalsIgnoreCase(getProperty(PROP_NUMBER_ARTIFICIAL_TOKEN).getValue().toString());
			return (numberArtificialTokens);
		}
	}

	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
		PepperMapper mapper = new Timeline2TokenMapper();
		return (mapper);
	}

	private class Timeline2TokenMapper extends PepperMapperImpl {

		@Override
		public DOCUMENT_STATUS mapSDocument() {
			SDocument doc = getDocument();
			SDocumentGraph graph = doc.getDocumentGraph();

			Set<SToken> artificialTokenSet = new HashSet<SToken>();
			ArrayList<SToken> artificialTokens = new ArrayList<SToken>();

			// create a new primary text from the timeline, with artificial
			// token
			STextualDS timelineText = SaltFactory.createSTextualDS();
			graph.addNode(timelineText);

			StringBuilder sbText = new StringBuilder();

			int i = 0;
			if (graph.getTimeline() == null) {
				throw new PepperModuleException(this, "No timeline was found for document '"+SaltUtil.getGlobalId(doc.getIdentifier())+"'. ");
			}
			if (graph.getTimeline().getEnd() == null) {
				throw new PepperModuleException(this, "The timeline for document '"+SaltUtil.getGlobalId(doc.getIdentifier())+"' does not contain any points of time. ");
			}
			for (int t= 0; t<= graph.getTimeline().getEnd(); t++){
				// create artificial token
				STextualRelation textRel = SaltFactory.createSTextualRelation();

				textRel.setStart(sbText.length());
				if (((Timeline2TokenProperties) getProperties()).getNumberArtificialTokens()) {
					sbText.append(i);
				}
				textRel.setEnd(sbText.length());
				sbText.append(" ");

				SToken tok = SaltFactory.createSToken();
				textRel.setSource(tok);
				textRel.setTarget(timelineText);

				graph.addNode(tok);
				graph.addRelation(textRel);

				artificialTokens.add(tok);
				artificialTokenSet.add(tok);

				i++;
			}

			timelineText.setText(sbText.toString());

			setProgress(0.1);

			// transform token of all primary texts to spans of the new
			// artificial one
			List<STimelineRelation> timeRels = new ArrayList<STimelineRelation>(graph.getTimelineRelations());
			ListIterator<STimelineRelation> it = timeRels.listIterator();
			while (it.hasNext()) {
				int currentPosition = it.nextIndex();

				double ratio = (double) currentPosition / (double) timeRels.size();
				setProgress(0.1 + (ratio * 0.7));

				STimelineRelation timeRel = it.next();

				SToken tok = timeRel.getSource();
				if (!artificialTokenSet.contains(tok)) {

					SSpan span = SaltFactory.createSSpan();
					graph.addNode(span);

					List<SToken> coveredArtificialToken = artificialTokens.subList(timeRel.getStart(), timeRel.getEnd());

					for (SToken newTok : coveredArtificialToken) {
						SSpanningRelation spanRel = SaltFactory.createSSpanningRelation();
						spanRel.setSource(span);
						spanRel.setTarget(newTok);

						graph.addRelation(spanRel);
					}

					copySpan(graph, tok, span);
					copyTokenAttributes(graph, coveredArtificialToken, tok, span);

					// remove token
					graph.removeNode(tok);
				}
			}

			setProgress(0.8);
			List<STextualDS> oldTexts = new LinkedList<STextualDS>(graph.getTextualDSs());
			oldTexts.remove(timelineText);

			for (STextualDS text : oldTexts) {
				graph.removeNode(text);
			}

			setProgress(1.0);
			return (DOCUMENT_STATUS.COMPLETED);
		}

		private void copySpan(SDocumentGraph graph, SToken tok, SSpan span) {
			// get the TextualDS which belongs too the token
			STextualRelation textRel = null;
			for (Relation e : graph.getOutRelations(tok.getId())) {
				if (e instanceof STextualRelation) {
					textRel = (STextualRelation) e;
					break;
				}
			}

			if (textRel != null) {
				STextualDS text = textRel.getTarget();

				// span needs to be converted to an annotation
				SAnnotation spanAnno = SaltFactory.createSAnnotation();
				spanAnno.setNamespace("annis");
				spanAnno.setName(text.getName());
				spanAnno.setValue(text.getText().substring(textRel.getStart(), textRel.getEnd()));

				span.addAnnotation(spanAnno);
			}
		}

		private void copyTokenAttributes(SDocumentGraph graph, List<SToken> coveredArtificialTokens, SToken tok, SSpan span) {

			copyAnnotations(tok, span);

			// translocate edges
			List<Relation> inRelations = new ArrayList<Relation>(graph.getInRelations(tok.getId()));

			for (Relation e : inRelations) {
				if (e instanceof SRelation) {
					if (e instanceof SSpanningRelation) {
						SSpanningRelation spanRel = (SSpanningRelation) e;
						copySpanRelation(graph, spanRel, coveredArtificialTokens);
						graph.removeRelation((SRelation)spanRel);
					} else {
						((SRelation) e).setTarget(span);
					}
				}
			}

			List<Relation> outRelations = new ArrayList<Relation>(graph.getOutRelations(tok.getId()));
			for (Relation e : outRelations) {
				if (e instanceof SRelation) {
					if (e instanceof STextualRelation) {
						STextualRelation textRel = (STextualRelation) e;
						copyTextualRelation(graph, textRel, coveredArtificialTokens);
						graph.removeRelation((SRelation)textRel);
					} else {
						((SRelation) e).setSource(span);
					}
				}
			}
		}

		private void copyTextualRelation(SDocumentGraph graph, STextualRelation origRel, List<SToken> newSources) {
			for (SToken tok : newSources) {
				SRelation rel = SaltFactory.createSTextualRelation();

				rel.setSource(tok);
				rel.setTarget(origRel.getTarget());
				copyAnnotations(origRel, rel);

				graph.addRelation(rel);
			}
		}

		private void copySpanRelation(SDocumentGraph graph, SSpanningRelation origRel, List<SToken> newTargets) {
			for (SToken tok : newTargets) {
				SRelation rel = SaltFactory.createSSpanningRelation();

				rel.setSource(origRel.getSource());
				rel.setTarget(tok);
				copyAnnotations(origRel, rel);

				graph.addRelation(rel);
			}
		}

		private void copyAnnotations(LabelableElement orig, LabelableElement copy) {
			// copy annotations and features
			if (orig instanceof SAnnotationContainer && copy instanceof SAnnotationContainer) {
				for (SAnnotation anno : ((SAnnotationContainer) orig).getAnnotations()) {
					((SAnnotationContainer) copy).addAnnotation(anno);
				}
			}
			if (orig instanceof SAnnotationContainer && copy instanceof SAnnotationContainer) {
				for (SFeature feature : ((SAnnotationContainer) orig).getFeatures()) {
					if (!"salt".equals(feature.getNamespace())) {
						((SAnnotationContainer) copy).addFeature(feature);
					}
				}
			}
		}
	}
}
