package indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.List;

import tokenizer.DocumentIndex;


public class SPIMI {
		

	private int blockSize;
	private int memorySize;
	private int blockNumber;
	private Iterator<DocumentIndex> documentIndexStream;
	private Map<String,List<Integer>> dictionary;
	
	
	private List<Integer> blockPostingsList;
	
	
	public SPIMI(int blockSize, int memorySize){
		this.blockSize = blockSize;
		this.memorySize = memorySize;
	}
	
	
	public Iterator<DocumentIndex> getDocumentIndexStream() {
		return documentIndexStream;
	}


	public void setDocumentIndexStream(Iterator<DocumentIndex> documentIndexStream) {
		this.documentIndexStream = documentIndexStream;
	}


	public int getBlockNumber() {
		return blockNumber;
	}


	public void setBlockNumber(int blockNumber) {
		this.blockNumber = blockNumber;
	}
	
	
	//here create a new postings list and add the docID to it.
	public List<Integer> addToDictionary(Map<String,List<Integer>> dictionary,String term){
		List<Integer> postingsList = new ArrayList<Integer>();
		dictionary.put(term,postingsList);
		return postingsList;
	}
	
	public List<Integer> getPostingsList(Map<String,List<Integer>> dictionary, String term){
		return dictionary.get(term);
	}

	
	//for max memory in spimi invert
	public void setMemorySize(int memSize){
		this.memorySize = memSize;
	}
	
	public int getMemorySize(){
		return this.memorySize;
	}
	
	//for max block memory in spimi invert
	public void setBlockSize(int blockSize){
		this.blockSize = blockSize;
	}
	
	public int getBlockSize(){
		return this.blockSize;
	}
	
	public Map<String, List<Integer>> getDictionary() {
		return dictionary;
	}

	public void setDictionary(Map<String, List<Integer>> dictionary) {
		this.dictionary = dictionary;
	}

	/**
	 * Spimi invert algorithm implementation
	 */
	public void SPIMIInvert(){
		
		//Setting the initial memory
		int initialMemory = (int) java.lang.Runtime.getRuntime().freeMemory();
		int usedMemory = 0;
		
		Map<String, List<Integer>> dictionary = new LinkedHashMap<String, List<Integer>>();

		//Checking with the memory limitation and with the document stream.
		while(usedMemory<this.memorySize && this.documentIndexStream.hasNext()){

			//Setting the current memory for the memory limitation
			int currentMemory = (int) java.lang.Runtime.getRuntime().freeMemory();
			usedMemory = initialMemory - currentMemory;
			
			//Getting the terms and docID of each document
			DocumentIndex docIndex = this.documentIndexStream.next();
				String[] terms = docIndex.getTerms();
				String docID = docIndex.getDocID();
	
				//Looping through all the terms
				for (int i = 0; i < terms.length; i++) {
					
					// note that our postings list is a list of integers.
					//our postings list variable
					List<Integer> postingsList;

					//Getting the current term
					String term = terms[i];
					
					//If we did not add the term, we create a new postings list and link it to our variable,else we just add it to the entry.
					if (dictionary.get(term) == null) {
						postingsList = this.addToDictionary(dictionary, term); 
					} else {
						postingsList = dictionary.get(term);
					}
					
					//instead of doubling the size when full, we use arrayList's implemented size increasing alogrithm
					//adding the posting to the list.
					postingsList.add(Integer.parseInt(docID));
				}
	
	
				
		}	
		
		//We now sort and write the block to disk.
		sortAndWriteBlockToFile(dictionary);

	}

	//merge all the blocks using a linear scam
	public void mergeAllBlocks(){
	
		//creating a new dictionary
		this.dictionary = new LinkedHashMap<String,List<Integer>>();
		
		//Looping through all our written blocks
		for(int i = 1;i<this.blockNumber;i++){
			
			//Getting the current block
			Map<String,List<Integer>> blockDictionary = this.readBlockAndConvertToDictionary("block"+i+".txt");
			
			//for debug
			//System.out.println("block dictionary size : " + blockDictionary.size());

			//Creating the new mergedblocks (dictionary) variable
			Map<String,List<Integer>> mergedBlocks = new LinkedHashMap<String,List<Integer>>();
			
			//Merging the terms of the two hashmaps together so we can do the linear scan.
			List<String> mergedSortedTerms = mergeBlocks(this.dictionary,blockDictionary);
			
			//Linear scan. O(n)
			for(String term : mergedSortedTerms){
				
				//If we have both terms, we merge the two postings list.
				if(this.dictionary.get(term)!=null && blockDictionary.get(term)!=null){
					//merge the two posting lists and add the term to the new merged list
					 mergedBlocks.put(term,mergeOrdered(this.dictionary.get(term),blockDictionary.get(term)));
				}else if(this.dictionary.get(term)!=null){
					//Merge the new term.
					mergedBlocks.put(term,this.dictionary.get(term));
				}else{
					//Merge the new term.
					mergedBlocks.put(term,blockDictionary.get(term));
				}

			}
			
			//Dictionary conists of the merged blocks.
			this.dictionary = mergedBlocks;

		}

		//WRite the dictionary to file.
		this.writeDictionary(this.dictionary);

		//For compiling dictionary compression techniques table
		count(this.dictionary);
		//countstopwordsremoval(this.dictionary,30);
		//countstopwordsremoval(this.dictionary,150);

	}
	
	public static void count(Map<String,List<Integer>> mergedDict){
		int tokens = 0;
		int nonpositionalpostings = 0;
			Set<String> keys = mergedDict.keySet();
		
		for(String term : keys){
				tokens += mergedDict.get(term).size();
				
				
				Set<Integer> nonpospostingslist = new LinkedHashSet<Integer>(mergedDict.get(term)); // to compile table at the end
				nonpositionalpostings += nonpospostingslist.size();
		}
		
	
		
		System.out.println("Number of terms : " + mergedDict.size());
		System.out.println("Number of non pos postings : " + nonpositionalpostings);
		System.out.println("Number of tokens : " + tokens);
	}
	
	public static void countstopwordsremoval(Map<String,List<Integer>> mergedDict,int numberOfStopWords){
		int tokens = 0;
		int nonpositionalpostings = 0;
		int sizeOfDict = 0;
		
		Set<String> keys = mergedDict.keySet();
		
		for(String term : keys){
			if(!isAStopWord(numberOfStopWords,term)){
				sizeOfDict++;
				tokens += mergedDict.get(term).size();
				
				
				Set<Integer> nonpospostingslist = new LinkedHashSet<Integer>(mergedDict.get(term)); // to compile table at the end
				nonpositionalpostings += nonpospostingslist.size();
			}
		}

		System.out.println("Number of terms with " + numberOfStopWords +" stopwords : " + sizeOfDict);
		System.out.println("Number of non pos postings with " + numberOfStopWords +" stopwords : " + nonpositionalpostings);
		System.out.println("Number of tokens with " + numberOfStopWords +" stopwords : " + tokens);

		
	}
	
	public static boolean isAStopWord(int numberOfStopWords,String term){
		String[] stopwords = { "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any",
				"are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both",
				"but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing",
				"don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
				"have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
				"him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is",
				"isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no",
				"nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
				"out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so",
				"some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then",
				"there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those",
				"through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're",
				"we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while",
				"who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll",
				"you're", "you've", "your", "yours", "yourself", "yourselves" };
		
		if(numberOfStopWords>=stopwords.length) numberOfStopWords = stopwords.length;
		for(int i = 0; i<=numberOfStopWords;i++){
			if(term.equals(stopwords[i])){
				return true;
			}
		}
		
		return false;
	}

	/**
	 * to get the merged terms together so we can achieve our linear scan to merge the blocks
	 * @param map1
	 * @param map2
	 * @return the merged list of terms.
	 */
	public static List<String> mergeBlocks(Map<String,List<Integer>> map1,Map<String,List<Integer>> map2){
		//when putting the set, merge the postings
		Set<String> keys1 = map1.keySet();
		Set<String> keys2 = map2.keySet();		
		
		 // Get list iterators and fetch first value from each, if available
	    Iterator<String> iter1 = keys1.iterator();
	    Iterator<String> iter2 = keys2.iterator();

		 // Get list iterators and fetch first value from each, if available
	    List<String> merged = new ArrayList<String>();

	    String value1 = (iter1.hasNext() ? iter1.next() : null);
	    String value2 = (iter2.hasNext() ? iter2.next() : null);
	    // Loop while values remain in either list
	    while (value1 != null || value2 != null) {

	    if(value2 == null){
	    	merged.add(value1);
            value1 = (iter1.hasNext() ? iter1.next() : null);
	    }else if(value1 == null){
	    	merged.add(value2);
            value2 = (iter2.hasNext() ? iter2.next() : null);
	    }
	    else if(value1.equals(value2)){
            merged.add(value1);
            value1 = (iter1.hasNext() ? iter1.next() : null);
            value2 = (iter2.hasNext() ? iter2.next() : null);

	    } else if (value1 != null && value1.compareTo(value2) <= 0) {

	            // Add list1 value to result and fetch next value, if available
	            merged.add(value1);
	            value1 = (iter1.hasNext() ? iter1.next() : null);

	        } else {

	            // Add list2 value to result and fetch next value, if available
	            merged.add(value2);
	            value2 = (iter2.hasNext() ? iter2.next() : null);

	        }
	    }

	    // Return merged result
	    return merged;

				
	}
	
	/**
	 * merging the two lists with a linear scan.
	 * @param list0
	 * @param list1
	 * @return the merged list.
	 */
	public static List<Integer> mergeOrdered( List<Integer> list0,  List<Integer> list1) {
	    List<Integer> result = new ArrayList<Integer>();
	    
	    
	    while (list0.size() > 0 && list1.size() > 0) {
	    if (list0.get(0).compareTo(list1.get(0)) < 0) {
	            result.add(list0.get(0));
	            list0.remove(0);
	        }
	        else {
	            result.add(list1.get(0));
	            list1.remove(0);
	        }
	    }

	    if (list0.size() > 0) {
	        result.addAll(list0);
	    }
	    else if (list1.size() > 0) {
	        result.addAll(list1);
	    }

	    return result;
	}
	
	/**
	 * Writing dict at the end of merging. no sorting here
	 * @param dictionary
	 */
	private void writeDictionary(Map<String,List<Integer>> dictionary){
	this.blockNumber++;
		
		Path file = Paths.get("dictionary.txt");
		
		dictionary.remove("");

		List<String> keys = new ArrayList<String>(dictionary.keySet());

		List<String> lines = new ArrayList<String>();
		for(String key : keys){
			
			String index = key + " : " + dictionary.get(key).toString();
			lines.add(index);

		}
		try {
			Files.write(file, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sorting and writing to disk each block (before merge)
	 * @param dictionary
	 */
	private void sortAndWriteBlockToFile(Map<String,List<Integer>> dictionary){
		this.blockNumber++;
		
		Path file = Paths.get("block"+this.blockNumber+".txt");
		
		dictionary.remove("");

		List<String> keys = new ArrayList<String>(dictionary.keySet());
		Collections.sort(keys);

		List<String> lines = new ArrayList<String>();
		for(String key : keys){
			Collections.sort(dictionary.get(key)); //sorting the postings list
			String index = key + " : " + dictionary.get(key).toString();
			lines.add(index);

		}
		try {
			Files.write(file, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Method to parse postings from line.
	private static List<Integer> getPostingsFromLine(String line){
		List<Integer> postingsList = new ArrayList<Integer>();
		
		line = line.replace(']', Character.MIN_VALUE); //equilavent of replacing with empty char
		
		
		 String [] lineComponents = line.split(" : \\["); //now we have the postings as string delimited with ','
		 String [] postings = lineComponents[1].split(",");
		
		 //converting from array to list
		 
		 for(String s : postings){
			 s = s.trim();
			 postingsList.add(Integer.valueOf(s));
		 }
		  
		  
		
		return postingsList;
	}
	
	//reading the block from disk.
	public Map<String,List<Integer>> readBlockAndConvertToDictionary(String blockFileName){
		Map<String, List<Integer>> blockDictionary = new LinkedHashMap<String, List<Integer>>();

		
		//read file into stream, try-with-resources
		try (Stream<String> stream = Files.lines(Paths.get(blockFileName))) {
			
			this.blockPostingsList = new ArrayList<Integer>();
		     
			
			
			stream.forEach(line -> {
				//filling up the dictionary
				String term = line.split(" :")[0];
			   
			    
				this.blockPostingsList = getPostingsFromLine(line);
				
				blockDictionary.put(term, blockPostingsList);
				
			} );
			
			

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return blockDictionary;
	}
}
