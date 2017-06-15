/*
 * Copyright 2017 Humboldt-Universität zu Berlin, INRIA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.corpus_tools.peppermodules.modulebox;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.graph.Identifier;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Thomas Krause <thomaskrause@posteo.de>
 */
@Component(name = "AnnotationToSpanComponent", factory = "PepperManipulatorComponentFactory")
public class AnnotationToSpan extends PepperManipulatorImpl {
  
  private static final Logger logger = LoggerFactory.getLogger("AnnotationToSpan");

  public AnnotationToSpan()
  {
    super();
    setName("AnnotationToSpan");
    setVersion("1.0.0");
  }
  
  @Override
  public PepperMapper createPepperMapper(Identifier sElementId) {
    AnnotationToSpanMapper mapper = new AnnotationToSpanMapper();
    
    return mapper;
  }
  
  
  
  public class AnnotationToSpanMapper extends PepperMapperImpl implements PepperMapper {

    @Override
    public DOCUMENT_STATUS mapSDocument() {
      
      SDocumentGraph g = getDocument().getDocumentGraph();
      
      List<SSpan> spanList = g.getSpans();
      List<SToken> tokenList = g.getTokens();
      
      final int numOfNodes = tokenList.size() + spanList.size();
      
      int i=0;
      for(SSpan n : new LinkedList<>(spanList)) {
        Set<SAnnotation> annos = n.getAnnotations();
        if(annos != null && annos.size() > 1) {
          
          List<SToken> overlappedToken = g.getOverlappedTokens(n);          
          List<SAnnotation> annoList = new LinkedList<>(annos);
          
          for(SAnnotation a : annoList) {
            SSpan newSpan = g.createSpan(overlappedToken);
            newSpan.addAnnotation(a);
          }
        }
        i++;
        setProgress((double) i / (double) numOfNodes );
      }
      for(SToken t : new LinkedList<>(tokenList)) {
        Set<SAnnotation> annos = t.getAnnotations();
        if(annos != null && annos.size() > 1) {
          
          List<SAnnotation> annoList = new LinkedList<>(annos);
          
          for(SAnnotation a : annoList) {
            SSpan newSpan = g.createSpan(t);
            newSpan.addAnnotation(a);
          }
        }
        i++;
        setProgress((double) i / (double) numOfNodes );
      }
      
      return DOCUMENT_STATUS.COMPLETED;
    }
    
  }
  
}
