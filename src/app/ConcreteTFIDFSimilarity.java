package app;

import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 * tf-idf similarity implementation
 * @author draganmisic
 *
 */
public class ConcreteTFIDFSimilarity extends ClassicSimilarity{
	
	@Override
	public float idf(long docFreq, long numDocs){
		return (float) Math.log10((double)numDocs/(double)docFreq);
	}
	
	@Override
	public float tf(float freq){
		return (float) (1 + Math.log10((double)freq));
	}
}
