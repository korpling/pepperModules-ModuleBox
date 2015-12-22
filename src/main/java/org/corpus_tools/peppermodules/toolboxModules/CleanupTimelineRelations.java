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

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.corpus_tools.peppermodules.ridgesModules.*;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SOrderRelation;
import org.corpus_tools.salt.common.STimeline;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This class will cleanup {@link STimelineRelation} from a {@link STimeline}.
 * If there is more than one outgoing {@link STimelineRelation} from a
 * {@link SToken} it will merge the relations into one relation.
 *
 * @author Thomas Krause
 *
 */
@Component(name = "CleanupTimelineRelations", factory = "PepperManipulatorComponentFactory")
public class CleanupTimelineRelations extends PepperManipulatorImpl {

  private static final Logger logger = LoggerFactory.getLogger("CleanupTimelineRelations");

  public CleanupTimelineRelations() {
    super();
    this.setName("CleanupTimelineRelations");
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
    CleanupTimelineRelationsMapper mapper = new CleanupTimelineRelationsMapper();
    return (mapper);
  }

  public class CleanupTimelineRelationsMapper extends PepperMapperImpl implements PepperMapper {

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

      if (timeline != null && timeline.getStart() != null && timeline.getEnd() != null) {

        // collect all point of times that are in use and by which token
        TreeMultimap<Integer, SToken> usedPoT = TreeMultimap.create(Ordering.natural(),
                new Ordering<SToken>() {
          @Override
          public int compare(SToken left, SToken right) {
            return ComparisonChain.start().compare(left.getId(), right.getId()).result();
          }
        });
        for (STimelineRelation rel : graph.getTimelineRelations()) {
          usedPoT.put(rel.getStart(), rel.getSource());
          usedPoT.put(rel.getEnd(), rel.getSource());
        }

        // iterate over all points of time and remove them if they are not in use
        int removedPoT = 0;
        for (int i = timeline.getStart(); i <= timeline.getEnd(); i++) {
          if (!usedPoT.containsKey(i)) {
            removePointOfTime(i-removedPoT, usedPoT);
            removedPoT++;
          }
          
          setProgress((double) i / (double) timeline.getEnd() );
        }
        timeline.setData(timeline.getEnd()-removedPoT);

      } // end if there is a timeline

      return (DOCUMENT_STATUS.COMPLETED);
    }
    
    private int checkPoTRange(STimelineRelation timeRel, TreeMultimap<Integer, SToken> tokenByPoT) {
      
      int removedPoTs = 0;
      
      for(int pot=timeRel.getStart()+1; pot < timeRel.getEnd(); pot++) {
        if(!tokenByPoT.containsKey(pot)) {
      
          // this PoT is unused and should be removed
          tokenByPoT.remove(timeRel.getEnd(), timeRel.getSource());
          timeRel.setEnd(timeRel.getEnd()-1);
          
          SortedSet<Integer> subsequentPoTs = new TreeSet<>(tokenByPoT.keySet().tailSet(pot, false));
          
          removedPoTs++;
        }
      }
      
      return removedPoTs;
    }
    
    
    private void setPoTOffset(Set<SToken> token, int offset, TreeMultimap<Integer, SToken> tokenByPoT) {
      
        for (SToken tok : token) {
          for (SRelation rel : tok.getOutRelations()) {
            if (rel instanceof STimelineRelation) {
              STimelineRelation timeRel = (STimelineRelation) rel;
              
              // one PoT less means we have to adjust the indexes
              timeRel.setStart(timeRel.getStart() + offset);
              timeRel.setEnd(timeRel.getEnd() + offset);
              
              tokenByPoT.put(timeRel.getStart(), timeRel.getSource());
              tokenByPoT.put(timeRel.getEnd(), timeRel.getSource());
            }
          }
        } // end for each token
        
    }

    private void removePointOfTime(int potRemove, TreeMultimap<Integer, SToken> tokenByPoT) {
      // find all token that are behind the position we want to delete 
      // (it's not inclusive since we already know that the position to delete has no connected token)
      SortedSet<Integer> subsequentPoTs = new TreeSet<>(tokenByPoT.keySet().tailSet(potRemove, false));
      for (int pot : subsequentPoTs) {
        
        Set<SToken> tokenForCurrentPoT = new LinkedHashSet<>(tokenByPoT.get(pot));
        tokenByPoT.removeAll(pot);
        
        for (SToken tok : tokenForCurrentPoT) {
          for (SRelation rel : tok.getOutRelations()) {
            if (rel instanceof STimelineRelation) {
              STimelineRelation timeRel = (STimelineRelation) rel;
              
              // one PoT less means we have to adjust the indexes
              timeRel.setStart(timeRel.getStart() - 1);
              timeRel.setEnd(timeRel.getEnd() - 1);
              
              tokenByPoT.put(timeRel.getStart(), timeRel.getSource());
              tokenByPoT.put(timeRel.getEnd(), timeRel.getSource());
            }
          }
        } // end for each token
        
        // also update the PoT map
        
        
      } // end for each PoT after the PoT to remove
    }
  }
}
