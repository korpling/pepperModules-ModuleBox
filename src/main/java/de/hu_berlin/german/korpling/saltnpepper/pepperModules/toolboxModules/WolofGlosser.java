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
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.graph.Identifier;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

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
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
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
							logger.warn("Ignore glossar file entry '" + line + "' in file '" + glossarFile.getAbsolutePath() + "', because it has '" + parts.length + "' columns instead of '2' or '3'. ");
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
	 * PepperModule#createPepperMapper(Identifier)}
	 */
	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
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
		 * {@inheritDoc PepperMapper#setDocument(SDocument)}
		 * 
		 * OVERRIDE THIS METHOD FOR CUSTOMIZED MAPPING.
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			if ((getDocument().getDocumentGraph() != null) && (getDocument().getDocumentGraph().getTokens().size() > 0)) {
				for (SToken tok : getDocument().getDocumentGraph().getTokens()) {
					String text = getDocument().getDocumentGraph().getText(tok);
					String gloss = glossar.get(text);

					if (gloss != null) {
						tok.createAnnotation(null, "gloss", gloss);
					}
					String pos = posTags.get(text);
					if (pos != null) {
						tok.createAnnotation(null, "pos_gloss", pos);
					}
				}
			}// if document contains a document graph
			return (DOCUMENT_STATUS.COMPLETED);
		}
	}
}