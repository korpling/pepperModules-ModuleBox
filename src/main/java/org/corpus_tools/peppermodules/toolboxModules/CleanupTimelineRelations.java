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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.LinkedList;
import org.corpus_tools.peppermodules.ridgesModules.*;
import java.util.List;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.graph.Label;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * This class will cleanup {@link STimelineRelation} from a {@link STimeline}. 
 * If there is more than one outgoing {@link STimelineRelation} from
 * a {@link SToken} it will merge the relations into one relation.
 * 
 * @author Thomas Krause
 * 
 */
@Component(name = "CleanupTimelineRelations", factory = "PepperManipulatorComponentFactory")
public class CleanupTimelineRelations extends PepperManipulatorImpl {
	private static final Logger logger = LoggerFactory.getLogger("CleanupTimelineRelations");

	public CleanupTimelineRelations() {
		super();
		this.setName("RemoveUnusedTimelineItems");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-ModuleBox"));
		setDesc("This class will cleanup timeline relations from a timeline.  "
      + "If there is more than one outgoing timeline relation from a token it will merge the relations into one relation.");
	}
  
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
        
        ListMultimap<SToken, STimelineRelation> tok2rel = LinkedListMultimap.create();
        for(STimelineRelation rel : graph.getTimelineRelations()) {
          tok2rel.put(rel.getSource(), rel);
        }
        
        for(SToken tok : graph.getTokens()) {
          List<STimelineRelation> timeRels = tok2rel.get(tok);
          if(timeRels != null && timeRels.size() > 1) {
            
            STimelineRelation mergedRel = SaltFactory.createSTimelineRelation();
            mergedRel.setSource(tok);
            mergedRel.setTarget(timeline);
            
            // collect the minmal starting and the maximal end point
            int minStart = Integer.MAX_VALUE;
            int maxEnd = Integer.MIN_VALUE;
            for(STimelineRelation oldRel : timeRels) {
              minStart = Math.min(minStart, oldRel.getStart());
              maxEnd = Math.max(maxEnd, oldRel.getEnd());
              
              
              // move all labels
              List<Label> labelsOfOldRel = new LinkedList<>(oldRel.getLabels());
              for(Label oldLabel : labelsOfOldRel) {
                if(mergedRel.getLabel(oldLabel.getQName()) == null) {
                  mergedRel.addLabel(oldLabel);
                } else {
                  oldRel.removeLabel(oldLabel.getQName());
                }
              }
              
              // remove the original timeline relation
              graph.removeRelation(oldRel);
              
            }
            // add the merged timeline relation
            mergedRel.setStart(minStart);
            mergedRel.setEnd(maxEnd);
            graph.addRelation(mergedRel);
          }
        }
        
        
      } // end if there is a timeline

			return (DOCUMENT_STATUS.COMPLETED);
		}
	}
}
