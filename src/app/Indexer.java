package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;

public class Indexer {

	IndexWriter writer;

	/**
	 * creates standard analyzer and index writer configuration which uses that analyzer
	 * set similarity for the index writer configuration
	 * creates index writer with the created configuration
	 * @param directory
	 * @param sim
	 * @throws IOException
	 */
	public Indexer(Directory directory, Similarity sim) throws IOException {
		Analyzer analyzer = new StandardAnalyzer();
		
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setSimilarity(sim);
		
		writer = new IndexWriter(directory, config);
		
	}

	/**
	 * goes through file tree and calls indexDocument for each file
	 * example from https://lucene.apache.org/core/5_5_0/core/overview-summary.html
	 * @param path
	 * @throws IOException
	 */
	public void indexDocuments(Path path) throws IOException {
		
		if (Files.isDirectory(path)) {
		
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					indexDocument(file, attrs.lastModifiedTime().toMillis());
					return FileVisitResult.CONTINUE;
				}
				
			});
			
		} else {
			
			indexDocument(path, Files.getLastModifiedTime(path).toMillis());
		
		}
		
		writer.commit();
	}

	/**
	 * indexes each document: creates fields path, modified and contents
	 * example from https://lucene.apache.org/core/5_5_0/core/overview-summary.html
	 * @param file
	 * @param lastModified
	 * @throws IOException
	 */
	public void indexDocument(Path file, long lastModified) throws IOException {
		
		InputStream stream = Files.newInputStream(file);

		Document doc = new Document();

		Field pathField = new StringField("path", file.toString(), Field.Store.YES);
		doc.add(pathField);

		doc.add(new LongField("modified", lastModified, Field.Store.NO));
		
		doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));

		if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {

			System.out.println("adding " + file);
			writer.addDocument(doc);
			
		} else {

			System.out.println("updating " + file);
			writer.updateDocument(new Term("path", file.toString()), doc);
		
		}
	}

}
