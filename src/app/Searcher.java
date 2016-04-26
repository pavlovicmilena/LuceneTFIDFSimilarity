package app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.codecs.TermStats;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.store.Directory;

public class Searcher {

	DirectoryReader reader;
	IndexSearcher indexSearcher;
	IndexReader indexReader;
	ConcreteTFIDFSimilarity sim;
	Query query;

	public Searcher(){}
	
	/**
	 * creates directory reader and index searcher
	 * sets similarity to index searcher so that the searcher uses vector space model for searching
	 * important note: the same similarity set here should also be set to index writer
	 * @param directory
	 * @param sim
	 * @throws IOException
	 */
	public void startSearcher(Directory directory, ConcreteTFIDFSimilarity sim) throws IOException{
		reader = DirectoryReader.open(directory);
		indexSearcher = new IndexSearcher(reader);
		indexSearcher.setSimilarity(sim);
		this.sim = sim;
		indexReader = indexSearcher.getIndexReader();
	}
	
	/**
	 * closes directory reader
	 * @throws IOException
	 */
	public void stopSearcher() throws IOException{
		reader.close();
	}
	
	/**
	 * creates a phrase query based on input argument
	 * queries documents
	 * uses similarity set in constructor
	 * @param querystring
	 * @throws IOException
	 */
	public void phraseSearch(String querystring) throws IOException{
		
		System.out.println("Starting Phrase Search...");

		String[] searchWords = querystring.split(" ");
		ArrayList<Term> terms = new ArrayList<Term>();
		
		PhraseQuery.Builder builder = new PhraseQuery.Builder();
		for(int i = 0; i < searchWords.length; i++){
			Term t = new Term("contents", searchWords[i]);
			builder.add(t);
			terms.add(t);
		}
		
		query = builder.build();
		
		// documents containing the phrase
		ScoreDoc[] docs = indexSearcher.search(query, 3).scoreDocs;
		
		// hash map which will store scores of all documents with document identifiers as keys
		HashMap<Integer, Float> score = new HashMap<Integer, Float>();
		
		// initialize values to zero
		for(int i = 0; i < indexReader.maxDoc(); i++)
			score.put(i, (float) 0);

		// calculates score for each document as a sum of scores of for the document for each term in the phrase
		for(int i = 0; i < terms.size(); i++){
			
			System.out.println("\tTerm: " + terms.get(i).text());
			
			CollectionStatistics collectionStats = new CollectionStatistics("contents", indexReader.maxDoc(), indexReader.getDocCount("contents"), indexReader.getSumTotalTermFreq("contents"), indexReader.getSumDocFreq("contents"));
			TermStatistics termStats = new TermStatistics(terms.get(i).bytes(), indexReader.docFreq(terms.get(i)), indexReader.totalTermFreq(terms.get(i)));
			
			// creates explanation object - how the idf was calculated
			Explanation explanation = sim.idfExplain(collectionStats, termStats);
			
			System.out.println("\t\tidf explanation: " + explanation.toString());
			
			float idf = sim.idf(indexReader.docFreq(terms.get(i)), indexReader.maxDoc());
			PostingsEnum docEnum = MultiFields.getTermDocsEnum(indexReader, "contents", terms.get(i).bytes());
			
			System.out.println("\t\tFrequency in each document:");
			while(docEnum.nextDoc() < indexReader.numDocs()){
				float tf = sim.tf(docEnum.freq());
				System.out.println("\t\t\tDocument id: " + docEnum.docID() + " tf: " + tf);
				score.put(docEnum.docID(), score.get(docEnum.docID()) + tf*idf);
			}
			
			System.out.println();
		}
		
		// prints scores and explanations for documents which contain the phrase
		System.out.println("\tScores for best-ranked documents:");
				
		for(ScoreDoc s : docs)
			System.out.println("\t\tDocument id: " + s.doc + " score: " + score.get(s.doc) + "\n\n explanation:\n" + indexSearcher.explain(query, s.doc).toString());
		
		System.out.println("Exiting Phrase Search...");
	}
	
	/**
	 * searches for term which is created from querystring
	 * uses TermQuery and tf-idf similarity
	 * @param querystring
	 * @throws IOException
	 */
	public void termSearch(String querystring) throws IOException{
		
		System.out.println("Starting Term Search...");		
		
		// creates term from the querystring which is tied to the contents field
		Term t = new Term("contents", querystring);
		query = new TermQuery(t);
		
		// calculates inverse document frequency for the term t
		float idf = sim.idf(indexReader.docFreq(t), indexReader.maxDoc());
		
		CollectionStatistics collectionStats = new CollectionStatistics("contents", indexReader.maxDoc(), indexReader.getDocCount("contents"), indexReader.getSumTotalTermFreq("contents"), indexReader.getSumDocFreq("contents"));
		TermStatistics termStats = new TermStatistics(t.bytes(), indexReader.docFreq(t), indexReader.totalTermFreq(t));
		
		Explanation explanation = sim.idfExplain(collectionStats, termStats);
		
		System.out.println("\tidf explanation: " + explanation.toString());
		
		// finds documents which contain the term in the field contents
		PostingsEnum docEnum = MultiFields.getTermDocsEnum(indexReader, "contents", t.bytes());
		
		System.out.println("\tDocuments where the term was found:");
		
		// for each document which is result of the query:
		while(docEnum.nextDoc() < indexReader.numDocs()){
			// calculates term frequency
			float tf = sim.tf(docEnum.freq());
			
			// calculates vector space model score for the document
			double score = tf * idf;
			System.out.println("\t\tDocument id: " + docEnum.docID() + " tf: " + tf + " score:" + score);
		}
		
		System.out.println("Exiting Term Search...");
		
	}
	
	public void search(String querystring) throws IOException{
		
		if(querystring.contains(" "))
			phraseSearch(querystring);
		else
			termSearch(querystring);
	}
	
}
