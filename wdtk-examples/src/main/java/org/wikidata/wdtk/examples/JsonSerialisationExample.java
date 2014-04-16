package org.wikidata.wdtk.examples;

/*
 * #%L
 * Wikidata Toolkit Examples
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
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
 * #L%
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.json.JsonProcessor;
import org.wikidata.wdtk.datamodel.json.JsonSerializer;
import org.wikidata.wdtk.dumpfiles.MwDumpFileProcessor;
import org.wikidata.wdtk.dumpfiles.MwDumpFileProcessorImpl;
import org.wikidata.wdtk.dumpfiles.MwRevision;
import org.wikidata.wdtk.dumpfiles.MwRevisionProcessor;
import org.wikidata.wdtk.dumpfiles.MwRevisionProcessorBroker;
import org.wikidata.wdtk.dumpfiles.StatisticsMwRevisionProcessor;
import org.wikidata.wdtk.dumpfiles.WikibaseRevisionProcessor;
import org.wikidata.wdtk.dumpfiles.WmfDumpFileManager;
import org.wikidata.wdtk.examples.DumpProcessingExample.ItemStatisticsProcessor;
import org.wikidata.wdtk.util.DirectoryManager;
import org.wikidata.wdtk.util.DirectoryManagerImpl;
import org.wikidata.wdtk.util.WebResourceFetcher;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;

public class JsonSerialisationExample {

	public static void main(String[] args) throws IOException {

		// Define where log messages go
		configureLogging();

		// Print information about this program
		printDocumentation();

		// Create object to get hold of Wikidata.org dumpfiles
		WmfDumpFileManager dumpFileManager = createDumpFileManager();

		// Create an FileOutputStream for give out the json-file.
		FileOutputStream fileStream = new FileOutputStream("WikiDataDump.json");

		// Create object for processing json from EntityDocuments
		JsonProcessor processor = new JsonProcessor(fileStream);

		// Create an object for managing the serialization process
		JsonSerializer serializer = new JsonSerializer(processor);

		// Set up processing pipeline with the json serializer
		MwDumpFileProcessor dumpFileProcessor = createDumpFileProcessor(serializer);

		// Set up the JsonSerialzer and write headers
		serializer.startSerialisation();

		// Start processing (may trigger downloads where needed)
		dumpFileManager.processAllRecentDumps(dumpFileProcessor, true);

		// Finish the Serialisation Process and close the FileStream
		serializer.finishSerialisation();
	}

	/**
	 * Creates an object that manages dumpfiles published by the Wikimedia
	 * Foundation. This object will check for available complete and incremental
	 * dump files, both online and in a local download directory. It provides
	 * direct access to the (decompressed) string content of these files.
	 * <p>
	 * The details in this method define which download directory is to be used,
	 * which Wikimedia project we are interested in (Wikidata), and that we want
	 * to allow online access (instead of using local files only).
	 * 
	 * @return dump file manager
	 * @throws IOException
	 *             if the download directory is not accessible
	 */
	private static WmfDumpFileManager createDumpFileManager()
			throws IOException {
		// The following can also be set to another directory:
		String downloadDirectory = System.getProperty("user.dir");
		DirectoryManager downloadDirectoryManager = new DirectoryManagerImpl(
				downloadDirectory);

		// The following can be set to null for offline operation:
		WebResourceFetcher webResourceFetcher = new WebResourceFetcherImpl();

		// The string "wikidatawiki" identifies Wikidata.org:
		return new WmfDumpFileManager("wikidatawiki", downloadDirectoryManager,
				webResourceFetcher);
	}

	/**
	 * Create an object that handles the complete processing of MediaWiki
	 * dumpfiles. This processing consists of the following main steps:
	 * 
	 * <pre>
	 * XML dump file -> page revisions -> item documents
	 * </pre>
	 * 
	 * The objects handling each step are of type {@link MwDumpFileProcessor},
	 * {@link MwRevisionProcessor}, and {@link EntityDocumentProcessor}. In each
	 * case, the object on the left calls the object on the right whenever new
	 * data is available. Therefore, the object on the right must be known to
	 * the object on the left, so we set up the objects in reverse order.
	 * <p>
	 * Normally, there is exactly one processor of each type. In the code below,
	 * we want to use two different objects to process revisions (one to analyse
	 * Wikidata item information and one to gather basic statistics about all
	 * revisions). To do this, we use a broker class that processes revisions to
	 * distribute them further to any number of revision processors.
	 * 
	 * @return dump file processor
	 * @throws FileNotFoundException
	 */
	private static MwDumpFileProcessor createDumpFileProcessor(
			JsonSerializer serializer) throws FileNotFoundException {

		// Our local example class ItemStatisticsProcessor counts the number of
		// labels etc. in Wikibase item documents to print out some statistics.
		// It is the last part of the processing chain.
		EntityDocumentProcessor edpItemStats = serializer
				.getEntityDocumentProcessor();

		// Revision processor for extracting entity documents from revisions:
		// (the documents are sent to our example document processor)
		MwRevisionProcessor rpItemStats = new WikibaseRevisionProcessor(
				edpItemStats);

		// Revision processor for general statistics and time keeping:
		MwRevisionProcessor rpRevisionStats = new StatisticsMwRevisionProcessor(
				"revision processing statistics", 10000);

		// Broker to distribute revisions to multiple subscribers:
		MwRevisionProcessorBroker rpBroker = new MwRevisionProcessorBroker();
		// Subscribe to the most recent revisions of type wikibase item:
		rpBroker.registerMwRevisionProcessor(rpItemStats,
				MwRevision.MODEL_WIKIBASE_ITEM, true);
		// Subscribe to all current revisions (null = no filter):
		rpBroker.registerMwRevisionProcessor(rpRevisionStats, null, true);

		// Object to parse XML dumps to send page revisions to our broker:
		return new MwDumpFileProcessorImpl(rpBroker);
	}

	/**
	 * Defines how messages should be logged. This method can be modified to
	 * restrict the logging messages that are shown on the console or to change
	 * their formatting. See the documentation of Log4J for details on how to do
	 * this.
	 */
	private static void configureLogging() {
		// Create the appender that will write log messages to the console.
		ConsoleAppender consoleAppender = new ConsoleAppender();
		// Define the pattern of log messages.
		// Insert the string "%c{1}:%L" to also show class name and line.
		String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p - %m%n";
		consoleAppender.setLayout(new PatternLayout(pattern));
		// Change to Level.ERROR for fewer messages:
		consoleAppender.setThreshold(Level.INFO);

		consoleAppender.activateOptions();
		Logger.getRootLogger().addAppender(consoleAppender);
	}

	/**
	 * Print some basic documentation about this program.
	 */
	private static void printDocumentation() {
		System.out
				.println("********************************************************************");
		System.out.println("*** Wikidata Toolkit: Dump Processing Example");
		System.out.println("*** ");
		System.out
				.println("*** This program will download and process dumps from Wikidata.");
		System.out.println("*** It will print progress json.");
		System.out
				.println("*** Downloading may take some time initially. After that, files");
		System.out
				.println("*** are stored on disk and are used until newer dumps are available.");
		System.out
				.println("*** You can delete files manually when no longer needed (see ");
		System.out
				.println("*** message below for the directory where files are found).");
		System.out
				.println("********************************************************************");
	}
}
