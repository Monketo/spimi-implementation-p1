# SPIMI - Implementation - P1
Alessandro Rodi

This project is designed to implement **SPIMI**’s inverted index creation and also apply various dictionary compression techniques such as no number, case folding, 30 stop words, 150 stop words and case folding. 

It also has a designed query retrieval algorithm class named **QueryCommand**. It runs queries designed by myself, other students and the one posted online ( insert link here ).

# Design

Note that the dictionary has been designed with a Map abstracting the LinkedHashMap class, so the lookup of terms is **O(1)**. It is a **LinkedHashMap** so we can preserve the order of insertion, which is important when we merge the blocks together with a linear scan of blocks. The goal is to preserve the sorted output from the SPIMI implementation.

Specific classes have been made to run the implementation of SPIMI and create the inverted index.

### Tokenizer 
This class tokenizes all the document and parses the NEWDOCID in the document reuters header to set the docID associated to all the terms we’ll find in the document. ***JSoup*** has been used to parse the docID associated. 

### DocumentIndex 
This is a customized object class to represent EACH of the documents. It consists of an array of terms that are normalized, and the associated docID. This is then used in the SPIMI algorithm to find the specific terms and docID.

*The DocumentIndex constructor*:
```java
public DocumentIndex(String docID, String[] terms) {
		this.terms = terms;
		this.docID = docID;
	}
```

### SPIMI
One important change I made in the algorithm, is that the postingsList is not doubled, but the implicit implementation of Java’s ArrayList data structure is used. It is roughly the same performance, if not better, as Java’s ArrayList insert complexity is O(log(N)) because we are constantly adding elements. I thought about making the postings data structure a fixed-array of Integer entries, but no efficient method was found at keeping track of the next empty element in the array if the size was bigger than the elements in it. Therefore I used a List<Integer> abstracting the ArrayList.

```java
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
```

Here is the implementation of SPIMI in the project. We can also see the memory restriction, which is set when creating an instance of the SPIMI class. Our tokenized documents are grouped into a list of DocumentIndex instances, containing the terms and each of their docID. 

A block counter is kept throughout the class as a global variable. Each time a block file is written, this counter is written. (this.blocknumber++) The memory is also reset so we can write the next block. 

The SPIMI-invert method is called indefinitely while there are still documents in the iterator. It only ends when the documents are done being indexed. The loop is in the main.java class.

```java
public static void runSPIMIalgorithm(List<DocumentIndex> documents) throws IOException {

		SPIMI spimi = new SPIMI(650000, 650000);

		// send in the documentindex stream(all tokens, we can access
		// term(token) and docID(token)
		Iterator<DocumentIndex> documentStream = documents.iterator();
		spimi.setDocumentIndexStream(documentStream);

		while (documentStream.hasNext()) {
			spimi.SPIMIInvert();
		}

		// This will write the dictionary to disk.
		spimi.mergeAllBlocks();

	};
```

When the loop is done, we merge all the blocks with a simple linear scan of all the blocks. It is done in O(N) since the blocks are already sorted. We then write the merged blocks (dictionary) to the disk.
```java
//merge all the blocks using a linear scan
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
```
### Query Command
This class consists in creating the queries to test the project. Three methods can be used to run the queries on the dictionary. 
1.	`performKeywordQuery(String query)`: This method takes a parameters that can only be a keyword. A term that we want to search in the dictionary.
2.	`performAndQuery(String query)`: This method takes a parameter that has to be in the format :”term1 AND term2 AND term3…” If not in this format, a nullpointerexcpetion will be thrown because this method only executes queries with AND.
3.	`performOrQuery(String query)`: This method takes a parameters that has to be in the format: “term 1 OR term2 OR term3…” . If not in this format, a nullpointerexception will be thrown because this method only execute queries with OR.

### Main
This class is the main class used to run the project. In order: 
-	Tokenizing all the documents
-	Compiling the inverted index



## Sample Project Queries
*AND query 'Jimmy AND Carter' result:* [12136, 13540, 17023, 18005, 19432, 20614]\n
*AND query 'Green AND Party' result:* [10230, 21577]\n
*AND query 'Innovations AND in AND telecommunication' result:* []\n

***Alessandro Rodi*** 2017
