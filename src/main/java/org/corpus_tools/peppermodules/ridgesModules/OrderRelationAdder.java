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
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STextualRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * The OrderRelationAdder connects tokens or spans via an order relation with
 * each other. This manipulator can be customized to connect spans having a
 * specific annotation or to connect all tokens.
 * 
 * @author Thomas Krause
 * @version 1.0
 * 
 */
@Component(name = "OrderRelationAdderComponent", factory = "PepperManipulatorComponentFactory")
public class OrderRelationAdder extends PepperManipulatorImpl {
	private static final Logger logger = LoggerFactory.getLogger("OrderRelationAdder");

	public OrderRelationAdder() {
		super();
		this.setName("OrderRelationAdder");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-RidgesModules"));
		setDesc("The OrderRelationAdder connects tokens or spans via an order relation with each other. This manipulator can be customized to connect spans having a specific annotation or to connect all tokens. ");
		this.setProperties(new SOrderRelationAdderProperties());
	}

	/**
	 * {@inheritDoc PepperModule#createPepperMapper(Identifier)}
	 */
	/**
	 * Creates a mapper of type {@link PAULA2SaltMapper}. {@inheritDoc
	 * PepperModule#createPepperMapper(Identifier)}
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
		SOrderRelationMapper mapper = new SOrderRelationMapper();
		return (mapper);
	}

	public class SOrderRelationMapper extends PepperMapperImpl implements PepperMapper {
		/**
		 * This method runs in two loops, first all Spans are collected, whose
		 * annotations match the one of
		 * {@link SOrderRelationAdderProperties#getSegmentations()}. For all
		 * collected spans, an {@link SOrderRelation} is created.
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			SDocumentGraph graph = getDocument().getDocumentGraph();
			// number of SOrderRelations which have been created
			// int numOfCreatedSegmentations= 0;
			Map<String, Integer> numOfCreatedSegmentations = new Hashtable<>();

			if ((((SOrderRelationAdderProperties) this.getProperties()).getSegmentations() == null) || (((SOrderRelationAdderProperties) this.getProperties()).getSegmentations().size() == 0)) {
				// when no annotation names are given for segmentation, create
				// order relations for tokens
				logger.debug("[{}] No annotation names are given to be used as segmentations, therefore order relations are added between tokens. ", getName());
				// create a storage structure to find tokens to be linked by
				// sorder relation
				Map<STextualDS, List<Pair<Integer, SToken>>> maps = new Hashtable<>();
				for (STextualRelation rel : getDocument().getDocumentGraph().getTextualRelations()) {
					STextualDS text = rel.getTarget();
					if (text != null) {
						List<Pair<Integer, SToken>> list = maps.get(text);
						if (list == null) {
							list = new ArrayList<Pair<Integer, SToken>>();
							maps.put(text, list);
						}
						list.add(new ImmutablePair<>(rel.getStart(), rel.getSource()));
					}
				}
				int numberOfAddedRels = 0;
				// sorting list by the start value of the tokens and creating
				// order relations
				for (Map.Entry<STextualDS, List<Pair<Integer, SToken>>> entry : maps.entrySet()) {

					List<Pair<Integer, SToken>> list = entry.getValue();
					STextualDS textDS = entry.getKey();
					String segName = textDS.getName();

					Comparator<Pair<Integer, SToken>> comparator = new Comparator<Pair<Integer, SToken>>() {
						@Override
						public int compare(Pair<Integer, SToken> p1, Pair<Integer, SToken> p2) {
							return p1.getLeft() - p2.getLeft();
						}
					};
					Collections.sort(list, comparator);

					// adding SOrderRelation objects
					SToken lastToken = null;
					for (Pair<Integer, SToken> pair : list) {
						if (lastToken != null) {
							SOrderRelation orderRel = SaltFactory.createSOrderRelation();
							orderRel.setSource(lastToken);
							orderRel.setTarget(pair.getRight());

							if (segName != null) {
								orderRel.setType(segName);
							}
							getDocument().getDocumentGraph().addRelation(orderRel);
							numberOfAddedRels++;
						}
						lastToken = pair.getRight();
					}
				}
				if (numberOfAddedRels == 0) {
					logger.warn("[{}] Added no SOrderRelations for tokens. ", getName());
				} else {
					logger.debug("[{}] Added '" + numberOfAddedRels + "' SOrderRelations to document '{}'. ", getName(), getDocument().getId());
				}

			} else {
				// create order realtions for span having an annotation being
				// contained in list given by custom property

				if (((SOrderRelationAdderProperties) this.getProperties()).getSegmentations() != null) {
					int i = 0;
					// stores all spans corresponding to a segmentation
					Map<String, TreeMap<Integer, SSpan>> segmentationSpans = new Hashtable<String, TreeMap<Integer, SSpan>>();
					String seg = null;
					List<SALT_TYPE> allowed = new ArrayList<SALT_TYPE>();
					allowed.add(SALT_TYPE.STEXT_OVERLAPPING_RELATION);

					for (SSpan span : graph.getSpans()) {
						for (SAnnotation anno : span.getAnnotations()) {
							if (((SOrderRelationAdderProperties) getProperties()).getSegmentations().contains(anno.getName())) {
								seg = anno.getName();
								List<DataSourceSequence> overlapped = graph.getOverlappedDataSourceSequence(span, allowed);
								if (overlapped != null) {
									for (DataSourceSequence o : overlapped) {
										if (o.getDataSource() instanceof STextualDS) {
											TreeMap<Integer, SSpan> orderedSpans = segmentationSpans.get(seg);
											if (orderedSpans == null) {
												orderedSpans = new TreeMap<Integer, SSpan>();
												segmentationSpans.put(seg, orderedSpans);
											}
											orderedSpans.put((Integer)o.getStart(), span);
											break;
										}
									}
								}
							}
						}
					}
					for (String key : segmentationSpans.keySet()) {
						TreeMap<Integer, SSpan> orderedSpans = segmentationSpans.get(key);
						SSpan lastSpan = null;
						if (orderedSpans.values().size() == 0) {
							logger.warn("[{}] Cannot add any SOrderRelation, because no span was found having one of the following annotations '{}'.", getName(), ((SOrderRelationAdderProperties) this.getProperties()).getSegmentations());
						} else {
							for (SSpan span : orderedSpans.values()) {
								if (lastSpan != null) {
									SOrderRelation orderRel = SaltFactory.createSOrderRelation();
									orderRel.setSource(lastSpan);
									orderRel.setTarget(span);
									orderRel.setType(key);
									graph.addRelation(orderRel);
									Integer number = 1;
									if (numOfCreatedSegmentations.containsKey(key)) {
										number = numOfCreatedSegmentations.get(key);
										number++;
									}
									numOfCreatedSegmentations.put(key, number);
								}
								lastSpan = span;
							}
						}
						i++;
						setProgress(new Double(i / getDocument().getDocumentGraph().getSpans().size()));

					}
				}
				if (numOfCreatedSegmentations.size() == 0) {
					logger.warn("[{}] Added no SOrderRelations for segmentations {}. ", getName(), ((SOrderRelationAdderProperties) getProperties()).getSegmentations());
				} else {
					if (logger.isDebugEnabled()) {
						StringBuilder str = new StringBuilder();
						str.append("[" + getName() + "] report creation of segmentations: \n");
						for (String key : numOfCreatedSegmentations.keySet()) {
							str.append("\t - Added ");
							str.append(numOfCreatedSegmentations.get(key));
							str.append(" SOrderRelations (segmentations) for '");
							str.append(key);
							str.append("'.\n");
						}
						logger.debug(str.toString());
					}
				}
			}

			return (DOCUMENT_STATUS.COMPLETED);
		}
	}
}
