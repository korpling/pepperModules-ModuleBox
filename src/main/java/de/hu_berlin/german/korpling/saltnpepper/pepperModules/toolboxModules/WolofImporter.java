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

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperImporterImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.tokenizer.Tokenizer;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

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
		this.setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		this.setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-ModuleBox"));
		this.addSupportedFormat("xml", "1.0", null);
		this.getDocumentEndings().add("xml");
	}

	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
		PepperMapper mapper = new PepperMapperImpl() {
			@Override
			public DOCUMENT_STATUS mapSDocument() {
				DocumentStructureReader contentHandler = new DocumentStructureReader();
				contentHandler.structure = getDocument().getDocumentGraph();
				this.readXMLResource(contentHandler, getResourceURI());

				return DOCUMENT_STATUS.COMPLETED;
			}

		};
		if (sElementId.getIdentifiableElement() != null && sElementId.getIdentifiableElement() instanceof SDocument) {
			URI resource = getIdentifier2ResourceTable().get(sElementId);
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
				currentTokList = new ArrayList<SToken>();
			} else if (TAG_ARTICLE.equals(qName)) {
				for (int i = 0; i < attributes.getLength(); i++) {
					SAnnotation anno = SaltFactory.createSAnnotation();
					anno.setName(attributes.getQName(i));
					anno.setValue(attributes.getValue(i));
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

		List<SToken> currentTokList = new ArrayList<SToken>();
		List<SToken> articleTokList = new ArrayList<SToken>();

		Set<SAnnotation> annoList = new HashSet<>();
		HashMap<String, String> annoListForSegmentElem = new HashMap<String, String>();

		List<String> annosToAssociateWithWholeSegmentList = new ArrayList<String>();

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {

			if (TAG_WOL.equals(qName)) {
				if (primaryText == null) {
					// initialize primaryText

					primaryText = SaltFactory.createSTextualDS();
					primaryText.setText("");
					structure.addNode(primaryText);
				}
				String text = currentText.toString();

				Tokenizer tokenizer = new Tokenizer();
				List<String> tokenList = tokenizer.tokenizeToString(currentText.toString(), null);

				int offset = primaryText.getText().length();
				primaryText.setText(primaryText.getText() + text);

				for (String tok : tokenList) {
					int currentPos = text.indexOf(tok);
					int start = offset + currentPos;
					int end = start + tok.length();
					offset += tok.length() + currentPos;
					text = text.substring(currentPos + tok.length());

					SToken currTok = structure.createToken(primaryText, start, end);

					// remember all SToken
					currentTokList.add(currTok);
				}

				for (SToken curTok : currentTokList) {
					articleTokList.add(curTok);
				}
			} else if (TAG_SENTENCE.equals(qName)) {
				currentTokList.clear();
			} else if (TAG_EN.equals(qName)) {
				SSpan span = structure.createSpan(currentTokList);
				span.createAnnotation(null, "en", currentText.toString());
				span.createAnnotation(null, "sentence", "sentence");
			} else if (TAG_ARTICLE.equals(qName)) {
				SSpan article = structure.createSpan(articleTokList);
				for (SAnnotation anno : annoList) {
					article.addAnnotation(anno);
				}
				annoList.clear();
				articleTokList.clear();
			}
			currentText = new StringBuilder();
		}
	}

}
