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
package org.corpus_tools.peppermodules.script_modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.common.io.CharStreams;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.util.internal.persistence.SaltXML10Handler;
import org.corpus_tools.salt.util.internal.persistence.SaltXML10Writer;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * 
 * @author Thomas Krause
 * @version 1.0
 * 
 */
@Component(name = "ScriptManipulatorComponent", factory = "PepperManipulatorComponentFactory")
public class ScriptManipulator extends PepperManipulatorImpl {
	private static final Logger logger = LoggerFactory.getLogger(ScriptManipulator.class);

	public ScriptManipulator() {
		super();
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-ModuleBox"));
		setDesc("Executes a defined external script that manipulates the Salt document");
		// setting name of module
		this.setName("ScriptManipulator");
		setProperties(new ScriptManipulatorProperties());
	}

	public class ScriptManipulatorProperties extends PepperModuleProperties {
		private static final long serialVersionUID = 4531683409905022856L;
		private final static String PROP_PATH = "path";
		private final static String PROP_ARGS = "args";
		private final static String PROP_FORMAT = "format";

		public ScriptManipulatorProperties() {
			this.addProperty(new PepperModuleProperty<>(PROP_PATH, String.class,
					"The path to the script file to execute. If this is a relative path, it must be relative to the "
							+ "workflow file.",
					null, false));
			this.addProperty(new PepperModuleProperty<>(PROP_ARGS, String.class,
					"Additional arguments given to the script file.", "", false));
			this.addProperty(new PepperModuleProperty<>(PROP_FORMAT, String.class,
					"The format used to write and read from the script. Can be either \"graphml\" or \"saltxml\"",
					"graphml", false));
		}

		public String getPath() {
			return (String) getProperty(PROP_PATH).getValue();
		}

		public String getArgs() {
			return (String) getProperty(PROP_ARGS).getValue();
		}

		public String getFormat() {
			return (String) getProperty(PROP_FORMAT).getValue();
		}

	}

	protected ScriptManipulatorProperties getProps() {
		return (ScriptManipulatorProperties) this.getProperties();
	}

	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
		PepperMapper mapper = new ScriptMapper();
		return (mapper);
	}

	private class ScriptMapper extends PepperMapperImpl {

		private final XMLOutputFactory XML_OUT_FACTORY = XMLOutputFactory.newInstance();
		private final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

		@Override
		public DOCUMENT_STATUS mapSDocument() {
			final SDocument doc = getDocument();

			// create a process with the requested parameter
			File baseDir = new File(getModuleController().getJob().getBaseDir().toFileString());
			try {
				String path = getProps().getPath();
				if(path == null) {
					// nothing to do
					return DOCUMENT_STATUS.COMPLETED;
				}

				CommandLine cmdLine = new CommandLine(path);
				if (getProps().getArgs() != null) {
					cmdLine = cmdLine.addArguments(getProps().getArgs());
				}
				Map<String, String> env = EnvironmentUtils.getProcEnvironment();
				env.put("PEPPER_DOCUMENT_NAME", doc.getName());
				env.put("PEPPER_FORMAT", getProps().getFormat().toLowerCase());

				DefaultExecutor executor = new DefaultExecutor();
				executor.setWorkingDirectory(baseDir);

				// record the output streams
				final PipedOutputStream resultStream = new PipedOutputStream();
				final PipedOutputStream errStream = new PipedOutputStream();
				final PipedInputStream inStream = new PipedInputStream();
				executor.setStreamHandler(new PumpStreamHandler(resultStream, errStream, inStream));
				// create an error handler
				DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

				// execute asynchronously
				executor.execute(cmdLine, env, resultHandler);

				// create the representation in the requested format and write it to the
				// standard input of the process
				Runnable writer = () -> {
					try {
						PipedOutputStream stdin = new PipedOutputStream(inStream);
						switch (getProps().getFormat().toLowerCase()) {
						case "graphml":
							GraphMLWriter.convertFromSalt(stdin, doc);
							break;
						case "saltxml":
							XMLStreamWriter xmlWriter;

							xmlWriter = XML_OUT_FACTORY.createXMLStreamWriter(stdin);

							new SaltXML10Writer().writeObjects(xmlWriter, doc.getDocumentGraph());
							xmlWriter.flush();
							xmlWriter.close();

							break;
						default:
						}
					} catch (XMLStreamException | IOException ex) {
						throw new PepperModuleException(
								"Could not write to the manipulator script " + getProps().getPath(), ex);
					}
				};
				Thread writerThread = new Thread(writer);
				writerThread.start();

				Runnable reader = () -> {
					try {
						PipedInputStream stdout = new PipedInputStream(resultStream);

						switch (getProps().getFormat().toLowerCase()) {
						case "graphml":
							List<SDocument> docs = GraphMLReader.convertToSalt(stdout);
							if (!docs.isEmpty()) {
								doc.setDocumentGraph(docs.get(0).getDocumentGraph());
							}
							break;
						case "saltxml":
							SaltXML10Handler saltHandler = new SaltXML10Handler();
							SAXParser xmlParser = SAX_PARSER_FACTORY.newSAXParser();
							xmlParser.parse(stdout, saltHandler);
							getDocument().setDocumentGraph((SDocumentGraph) saltHandler.getSaltObject());
							break;
						default:
							logger.error("Invalid script-exchange format {} configured in properties.",
									getProps().getFormat());
						}
					} catch (IOException | ParserConfigurationException | SAXException ex) {
						throw new PepperModuleException(
								"Could not read from the manipulator script " + getProps().getPath(), ex);
					}
				};
				Thread readerThread = new Thread(reader);
				readerThread.start();

				resultHandler.waitFor();
				if (resultHandler.getExitValue() != 0) {
					// get stderr
					PipedInputStream stderr = new PipedInputStream(errStream);
					String errorMsg = CharStreams.toString(new InputStreamReader(stderr, StandardCharsets.UTF_8));

					throw new PepperModuleException("Manipulator script " + getProps().getPath()
							+ " returned error code " + resultHandler.getExitValue() + ":\n" + errorMsg);
				}

				readerThread.join();

			} catch (IOException | InterruptedException ex) {
				throw new PepperModuleException(
						"Could not execute the manipulator script " + getProps().getPath(), ex);
			}

			setProgress(1.0);
			return (DOCUMENT_STATUS.COMPLETED);
		}
	}
}
