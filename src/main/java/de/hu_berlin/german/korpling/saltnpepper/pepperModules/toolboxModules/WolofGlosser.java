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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

import com.neovisionaries.i18n.LanguageCode;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleNotReadyException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.impl.PepperManipulatorImpl;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.impl.PepperMapperImpl;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDataSourceSequence;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.tokenizer.AbbreviationDE;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.tokenizer.AbbreviationEN;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.tokenizer.AbbreviationFR;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.tokenizer.AbbreviationIT;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SElementId;

/**
 * 
 * @author florian
 *
 */
@Component(name = "WolofGlosserComponent", factory = "PepperManipulatorComponentFactory")
public class WolofGlosser extends PepperManipulatorImpl {
	public static final String FILE_GLOSSAR = "glossar.tab";
	public Map<String, String> glossar = new Hashtable<String, String>();
	public Map<String, String> posTags = new Hashtable<String, String>();
	/** maximal number of words for one gloss **/
	public int numOfWords = 0;

	public WolofGlosser() {
		super();
		setName("WolofGlosser");
		setSupplierContact(URI.createURI("saltnpepper@lists.hu-berlin.de"));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-ModuleBox"));
		setDesc("The WolofGlosser is a manipulator to create english glosses for a text written in wolof. ");
	}

	@Override
	public boolean isReadyToStart() throws PepperModuleNotReadyException {
		File glossarFile = new File(getResources().appendSegment(FILE_GLOSSAR).toFileString());
		if (glossarFile.exists()) {
			try (BufferedReader br = new BufferedReader(new FileReader(glossarFile))) {
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();
				while (line != null) {
					sb.append(line);
					sb.append(System.lineSeparator());
					line = br.readLine();
					if (line != null) {
						String[] parts = line.split("\t");
						int numOfBlanks = StringUtils.countMatches(parts[0], " ");
						if (numOfWords < numOfBlanks) {
							numOfWords = numOfBlanks;
						}
						if (parts.length == 3) {
							// glossar has pos and gloss entries

							posTags.put(parts[0], parts[1]);
							glossar.put(parts[0], parts[2]);
						} else if (parts.length == 2) {
							// glossar only has gloss entries

							glossar.put(parts[0], parts[1]);
						} else {
							logger.warn("Ignore glossar file entry '"+line+"' in file '" + glossarFile.getAbsolutePath() + "', because it has '" + parts.length + "' columns instead of '2' or '3'. ");
						}
					}
				}
			} catch (IOException e) {
				throw new PepperModuleException(this, "Cannot read the glossar file '" + glossarFile.getAbsolutePath() + "'. ", e);
			}
		} else {
			logger.warn("Cannot gloss texts, because the glossar file '" + glossarFile.getAbsolutePath() + "' does not exist. ");
		}
		return (true);
	}

	/**
	 * Creates a mapper of type {@link GlossarMapper}. {@inheritDoc
	 * PepperModule#createPepperMapper(SElementId)}
	 */
	@Override
	public PepperMapper createPepperMapper(SElementId sElementId) {
		GlossarMapper mapper = new GlossarMapper();
		mapper.glossar = glossar;
		mapper.posTags = posTags;
		mapper.numOfWords = numOfWords;
		return (mapper);
	}

	public static class GlossarMapper extends PepperMapperImpl {
		public Map<String, String> glossar = new Hashtable<String, String>();
		public Map<String, String> posTags = new Hashtable<String, String>();
		/** maximal number of words for one gloss **/
		public int numOfWords = 0;

		/**
		 * {@inheritDoc PepperMapper#setSDocument(SDocument)}
		 * 
		 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			if ((getSDocument().getSDocumentGraph() != null) && (getSDocument().getSDocumentGraph().getSTokens().size() > 0)) {
				for (SToken tok: getSDocument().getSDocumentGraph().getSTokens()){
					String text= getSDocument().getSDocumentGraph().getSText(tok);
					String gloss= glossar.get(text);
					
					if (gloss!= null){
						tok.createSAnnotation(null, "gloss", gloss);
					}
					String pos= posTags.get(text);
					if (pos!= null){
						tok.createSAnnotation(null, "pos_gloss", pos);
					}
				}
			}// if document contains a document graph
			return (DOCUMENT_STATUS.COMPLETED);
		}
	}
}