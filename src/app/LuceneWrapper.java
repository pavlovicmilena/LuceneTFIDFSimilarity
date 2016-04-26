package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class LuceneWrapper {

	Directory directory;
	ConcreteTFIDFSimilarity sim;
	Indexer indexer;
	Searcher searcher;
	
	/**
	 * creates RAMDirectory and ConcreteTFIDFSimilarity
	 * creates indexer and indexes documents
	 * creates searcher and starts it
	 * @throws IOException
	 */
	public LuceneWrapper() throws IOException{
		directory = new RAMDirectory();
		
		sim = new ConcreteTFIDFSimilarity();
		
		Indexer indexer = new Indexer(directory, sim);
		indexer.indexDocuments(Paths.get("./files"));

		searcher = new Searcher();
		searcher.startSearcher(directory, sim);
	}
	
	/**
	 * main search method: reads a query, performs a search and prints the result
	 * @throws IOException
	 */
	public void search() throws IOException{
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter query:");
        String querystring = br.readLine();
        
        // searches until q (quit) is entered
		do{
	        querystring = querystring.toLowerCase();
			searcher.search(querystring);
        	System.out.println("Enter query:");
	        querystring = br.readLine();
			
        }while(!querystring.equals("q"));
		
		System.out.println("Exiting...");
		
		searcher.stopSearcher();
		directory.close();
	}
	
}
