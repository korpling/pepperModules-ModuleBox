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
package org.corpus_tools.peppermodules.toolboxModules;

import org.corpus_tools.peppermodules.ridgesModules.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
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
 * This class will remove all unused timeline point of times (PoT) from the timeline.
 * A PoT is unused if no {@link STimelineRelation} is using this PoT.
 * 
 * @author Thomas Krause
 * 
 */
@Component(name = "RemoveUnusedTimelineItems", factory = "PepperManipulatorComponentFactory")
public class RemoveUnusedTimelineItems extends PepperManipulatorImpl {
	private static final Logger logger = LoggerFactory.getLogger("RemoveUnusedTimelineItems");

	public RemoveUnusedTimelineItems() {
		super();
		this.setName("RemoveUnusedTimelineItems");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-ModuleBox"));
		setDesc("This class will remove all unused timeline point of times (PoT) from the timeline. A PoT is unused if no timeline relation is using this PoT.");
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
		RemoveUnusedTimelineItemsMapper mapper = new RemoveUnusedTimelineItemsMapper();
		return (mapper);
	}

	public class RemoveUnusedTimelineItemsMapper extends PepperMapperImpl implements PepperMapper {
		/**
		 * This method runs in two loops, first all Spans are collected, whose
		 * annotations match the one of
		 * {@link SOrderRelationAdderProperties#getSegmentations()}. For all
		 * collected spans, an {@link SOrderRelation} is created.
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			SDocumentGraph graph = getDocument().getDocumentGraph();
			
      
      STimeline timeline = graph.getTimeline();
      
      if(timeline != null) {
      
        // collect all PoTs that are used in some timeline relation
        HashSet<Integer> usedPot = new HashSet<>();
        for(STimelineRelation rel : graph.getTimelineRelations()) {
          if(rel.getStart() != null) {
            usedPot.add(rel.getStart());
          }
          if(rel.getEnd() != null) {
            usedPot.add(rel.getEnd());
          }
        }
        
        
      } // end if there is a timeline

			return (DOCUMENT_STATUS.COMPLETED);
		}
	}
}
