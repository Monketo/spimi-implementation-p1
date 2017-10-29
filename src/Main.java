import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import tokenizer.DocumentIndex;
import tokenizer.Tokenizer;
import indexing.SPIMI;
import query.QueryCommand;

public class Main {

	public static void main(String[] args) {

		// Tokenizing is split into multiple threads of thousands of documents.
		// So it is faster.
		// Comment or uncomment this piece of code to tokenize and run the
		// indexer.
		/*
		 * try { 
		 * 
		 * //Tokenize the reuters collection 
		 * List<DocumentIndex> documents = tokenizeAllDocuments();
		 * 
		 * //Run spimi with the relevant documents 
		 * runSPIMIalgorithm(documents);
		 * 
		 * } 
		 * catch (InterruptedException e) 
		 * { e.printStackTrace(); }
		 * catch(IOException e) 
		 * { e.printStackTrace(); }
		 */

		performTestQueries();
	}

	/**
	 * This method tokenizes all of the given Reuters collection using the
	 * Tokenizer.java class.
	 * 
	 * @return a list of document indexes --> terms with their associated
	 *         docID's
	 * @throws InterruptedException
	 */
	public static List<DocumentIndex> tokenizeAllDocuments() throws InterruptedException {
		List<DocumentIndex> allDocuments = new ArrayList<DocumentIndex>();

		// manage the pool of threads and start the SPIMI when we're done.
		ExecutorService es = Executors.newCachedThreadPool();
		for (int i = 0; i < 22; i++) {
			final int iterator = i;
			Runnable task = () -> {
				String threadName = Thread.currentThread().getName();
				System.out.println("Hello " + threadName);

				Tokenizer tokenizer = new Tokenizer("reut2-0" + String.format("%02d", iterator) + ".sgm");
				tokenizer.readDocuments();

				allDocuments.addAll(tokenizer.getDocumentList());

			};
			es.execute(task);
		}
		es.shutdown();

		// when the tokenizing has finished
		@SuppressWarnings("unused")
		boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);

		// all documents are tokenized asynchronously for fast execution
		return allDocuments;
	}

	/**
	 * To start performing the test queries for our dictionary. This might throw
	 * a nullpointerexception, in which there are no documents matching the
	 * query. (This will be improved if this project has a second version, for
	 * the moment i ran out of time.)
	 */
	public static void performTestQueries() {

		SPIMI spimi = new SPIMI(0, 0);

		QueryCommand qc = new QueryCommand();
		qc.setDictionary(spimi.readBlockAndConvertToDictionary("dictionary.txt"));

		// NULL MEANS THERE ARE NO RESULTS!

		// Uncomment this to test yourself.
		 //performTestQueries(qc); //designed by myself
		 //performProjectQueries(qc);
		performStudentQueries(qc);

	}

	/**
	 * Queries designed by myself to test.
	 * (the three deliverables to deliver -- queries)
	 * @param qc
	 */
	public static void performTestQueries(QueryCommand qc) {
		
		//1st query deliverable.
		String query1 = "Honda";
		List<Integer> postings1 = qc.performKeywordQuery(query1);
		String testQuery1;
		if(postings1!=null){
		testQuery1 = postings1.toString(); 
		System.out.println("Keyword query "+ query1 +" result: " + testQuery1);
		}else{
			System.out.println("No result for: " + query1);
		}

		//2nd query deliverable
		String query2 = "honda AND car";
		List<Integer> postings2 = qc.performAndQuery(query2);
		String testQuery2;
		if(postings2!=null){
		testQuery2 = postings2.toString(); 
		System.out.println("Keyword query "+ query2 +" result: " + testQuery2);
		}else{
			System.out.println("No result for: " + query2);
		}

		//3rd query deliverable.
		String query3 = "IRS OR STC";
		List<Integer> postings3 = qc.performOrQuery(query3);
		String testQuery3;
		if(postings3!=null){
		testQuery3 = postings3.toString(); 
		System.out.println("Keyword query "+ query3 +" result: " + testQuery3);
		}else{
			System.out.println("No result for: " + query3);
		}
	}

	// This will perform the test queries that are online
	/**
	 * Queries for Project 1 1. Jimmy Carter 2. Green Party 3. Innovations in
	 * telecommunication
	 */
	public static void performProjectQueries(QueryCommand qc) {
		// online project 1 queries -- first one from
		String firstTestAndQuery = qc.performAndQuery("Jimmy AND Carter").toString();
		String firstTestOrQuery = qc.performOrQuery("Jimmy OR Carter").toString();
		System.out.println("AND query 'Jimmy AND Carter' result: " + firstTestAndQuery);
		System.out.println("OR query 'Jimmy OR Carter' result: " + firstTestOrQuery);

		String secondTestAndQuery = qc.performAndQuery("Green AND Party").toString();
		String secondTestOrQuery = qc.performOrQuery("Green OR Party").toString();
		System.out.println("AND query 'Green AND Party' result: " + secondTestAndQuery);
		System.out.println("OR query 'Green OR Party' result: " + secondTestOrQuery);

		String thirdTestAndQuery = qc.performAndQuery("Innovations AND in AND telecommunication").toString();
		String thirdTestOrQuery = qc.performOrQuery("Innovations OR in OR telecommunication").toString();
		System.out.println("AND query 'Innovations AND in AND telecommunication' result: " + thirdTestAndQuery);
		System.out.println("OR query 'Innovations OR in OR telecommunication' result: " + thirdTestOrQuery);

	}

	/**
	 * Perform queries exchanged with students
	 * 
	 * @param qc
	 *            the instance of performing queries to our dictionary.
	 */
	public static void performStudentQueries(QueryCommand qc) {

		// student queries -- first one from
		//Query from Sunanda Bansal
		String firstTestQuery = qc.performAndQuery("Mediterranean AND Oil").toString();
		System.out.println("AND query 'Mediterranean AND Oil' result: " + firstTestQuery);

		//Query from Darrell Guerrero
		String thirdTestQuery = qc.performKeywordQuery("audi").toString(); 
	    System.out.println("Keyword query 'audi' result: " + thirdTestQuery);

	    //Query from Marwah Alsadun	
		String fourthTestQuery = qc.performAndQuery("William AND Reynolds").toString();
		System.out.println("AND query 'William AND Reynolds' result: " + fourthTestQuery);
	}

	/**
	 * This runs the spimi algorithm (SPIMI-Invert) until all documents have
	 * finished processing.
	 * 
	 * @param documents
	 *            -- List of document indexes.
	 * @throws IOException
	 *             if there are problems with writing the blocks/dictionary to
	 *             disk
	 */
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

}
