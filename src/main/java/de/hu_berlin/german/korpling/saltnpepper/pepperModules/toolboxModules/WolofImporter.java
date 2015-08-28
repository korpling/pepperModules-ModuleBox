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
package de.hu_berlin.german.korpling.saltnpepper.pepperModules.toolboxModules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperImporter;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.impl.PepperImporterImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.impl.PepperMapperImpl;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SSpan;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.tokenizer.Tokenizer;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

@Component(name = "WolofImporterComponent", factory = "PepperImporterComponentFactory")
public class WolofImporter extends PepperImporterImpl implements PepperImporter {
	// this is a logger, for recording messages during program process, like
	// debug messages
	public static final Logger logger = LoggerFactory.getLogger(WolofImporter.class);

	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * A constructor for your module. Set the coordinates, with which your
	 * module shall be registered. The coordinates (modules name, version and
	 * supported formats) are a kind of a fingerprint, which should make your
	 * module unique.
	 */
	public WolofImporter() {
		super();
		this.setName("WolofImporter");
		this.setVersion("0.0.1");
		this.setDesc("This importer transforms data of an unknown format to salt. ");
		this.setSupplierContact(URI.createURI("saltnpepper@lists.hu-berlin.de"));
		this.setSupplierHomepage(URI.createURI("saltnpepper@lists.hu-berlin.de"));
		this.addSupportedFormat("xml", "1.0", null);
		this.getSDocumentEndings().add("xml");
	}

	@Override
	public PepperMapper createPepperMapper(SElementId sElementId) {
		PepperMapper mapper = new PepperMapperImpl() {
			@Override
			public DOCUMENT_STATUS mapSDocument() {
				DocumentStructureReader contentHandler = new DocumentStructureReader();
				contentHandler.structure = getSDocument().getSDocumentGraph();
				this.readXMLResource(contentHandler, getResourceURI());

				return DOCUMENT_STATUS.COMPLETED;
			}

		};
		if (sElementId.getIdentifiableElement() != null && sElementId.getIdentifiableElement() instanceof SDocument) {
			URI resource = getSElementId2ResourceTable().get(sElementId);
			mapper.setResourceURI(resource);
		}
		return mapper;
	}

	public class DocumentStructureReader extends DefaultHandler2 {
		public static final String TAG_ARTICLE = "article";
		public static final String TAG_SENTENCE = "s";
		public static final String TAG_WOL = "wol";
		public static final String TAG_EN = "en";

		public SDocumentGraph structure = null;
		private StringBuilder currentText = new StringBuilder();

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			currentText = new StringBuilder();
			if (TAG_WOL.equals(qName)) {
				// reset currentTokList for each new primary text
				currentTokList = new BasicEList<SToken>();
			} else if (TAG_ARTICLE.equals(qName)) {
				for (int i = 0; i < attributes.getLength(); i++) {
					SAnnotation anno = SaltFactory.eINSTANCE.createSAnnotation();
					anno.setSName(attributes.getQName(i));
					anno.setSValue(attributes.getValue(i));
					annoList.add(anno);
				}
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			for (int i = start; i < start + length; i++) {
				currentText.append(ch[i]);
			}
		}

		STextualDS primaryText = null;

		EList<SToken> currentTokList = new BasicEList<SToken>();
		EList<SToken> articleTokList = new BasicEList<SToken>();
		
		Set<SAnnotation> annoList = new HashSet<>();
		HashMap<String, String> annoListForSegmentElem = new HashMap<String, String>();

		List<String> annosToAssociateWithWholeSegmentList = new ArrayList<String>();

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {

			if (TAG_WOL.equals(qName)) {
				if (primaryText == null) {
					// initialize primaryText

					primaryText = SaltFactory.eINSTANCE.createSTextualDS();
					primaryText.setSText("");
					structure.addNode(primaryText);
				}
				String text = currentText.toString();

				Tokenizer tokenizer = new Tokenizer();
				List<String> tokenList = tokenizer.tokenizeToString(currentText.toString(), null);

				int offset = primaryText.getSText().length();
				primaryText.setSText(primaryText.getSText() + text);

				for (String tok : tokenList) {
					int currentPos = text.indexOf(tok);
					int start = offset + currentPos;
					int end = start + tok.length();
					offset += tok.length() + currentPos;
					text = text.substring(currentPos + tok.length());

					SToken currTok = structure.createSToken(primaryText, start, end);

					// remember all SToken
					currentTokList.add(currTok);
				}

				for (SToken curTok : currentTokList) {
					articleTokList.add(curTok);
				}
			}else if (TAG_SENTENCE.equals(qName)){
				currentTokList.clear();
			}else if (TAG_EN.equals(qName)){
				SSpan span= structure.createSSpan(currentTokList);
				span.createSAnnotation(null, "en", currentText.toString());
				span.createSAnnotation(null, "sentence", "sentence");
			}else if (TAG_ARTICLE.equals(qName)){
				SSpan article= structure.createSSpan(articleTokList);
				for (SAnnotation anno: annoList){
					article.addSAnnotation(anno);
				}
				annoList.clear();
				articleTokList.clear();
			}
			currentText = new StringBuilder();
		} 
	}

}
