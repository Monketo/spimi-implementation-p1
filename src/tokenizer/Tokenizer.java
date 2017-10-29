package tokenizer;

import tokenizer.DocumentIndex;
import stemmer.Stemmer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;


public class Tokenizer {

	private String fileName;
	private String fileContents;

	private List<DocumentIndex> documentList;

	public List<DocumentIndex> getDocumentList() {
		return documentList;
	}

	public void setDocumentList(List<DocumentIndex> documentList) {
		this.documentList = documentList;
	}

	public Tokenizer(String filename) {
		this.fileName = filename;
	}

	// Getters and setters
	public String getFileContents() {
		return fileContents;
	}

	public void setFileContents(String fileContents) {
		this.fileContents = fileContents;
	}

	public String getFilename() {
		return fileName;
	}

	public void setFilename(String filename) {
		this.fileName = filename;
	}

	//Reads the file 
	private String readFile(Charset encoding) throws IOException, URISyntaxException {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URL url = classloader.getResource(this.fileName);
		byte[] encoded = Files.readAllBytes(Paths.get(url.toURI()));
		return new String(encoded, encoding);
	}


	/**
	 * Reads the files with reuters news
	 */
	public void readDocuments() {
		String fileContents = "";
		try {
			fileContents = this.readFile(Charset.forName("utf-8"));
			this.fileContents = fileContents;
			this.tokenizeDocument();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException use) {
			use.printStackTrace();
		}

	}

	/**
	 * tokenizes the document
	 */
	private void tokenizeDocument() {
		this.removeStringGarbage();
		this.splitFileContents();
	}
	
	/**
	 * Removes special characters
	 */
	private void removeStringGarbage() {
		this.fileContents = this.fileContents.replaceAll("(?:&#[0-9]*;)", " ");
		this.fileContents = this.fileContents.replaceAll("\\,*", "");
		this.fileContents = this.fileContents.replaceAll("\\.*", "");
	};

	/**
	 * Splits the file contents.
	 * Compression techniques are here.
	 * Document ID is parsed using the NEWID tag.
	 * We remove the html tags, and split by parsing the REUTERS HEADER.
	 * WE apply the stemmer here, after that the documents are normalized/tokenized.
	 */
	private void splitFileContents() {
		// Consists of docId and list of all documents
		this.documentList = new ArrayList<DocumentIndex>();

		// Splitting string by keeping delimiter
		List<String> news = new ArrayList<String>();

		news.addAll(Arrays.asList(this.fileContents.split("(?=(?:<REUTERS)(?:.)+(?:NEWID=\".+\">))+")));

		news.remove(0);

		//For each word in the news.
		for (String tokens : news) {
			String docID = parseDocumentID(tokens.split("\n")[0]);
			tokens = Jsoup.clean(tokens, Whitelist.none());
			tokens = cleanToken(tokens);
			
			//COMPRESSION TECHNIQUES
			tokens = removeNumbers(tokens);
			tokens = caseFolding(tokens);
			//tokens = apply30stopwords(tokens);
			//tokens = apply150stopwords(tokens);
			
			//Splits to get the terms.
			String[] terms = tokens.split("\\s");

			//filling up the documentlist.
			DocumentIndex docIndex = new DocumentIndex(docID, terms);
			this.documentList.add(docIndex);
		}
		
		//PorterStemmer
		//applyStemmer();
	}
	
	/**
	 * Removing useless special characters found in the collection that were interfering with the creating of a good index.
	 * @param tokens the string to clean
	 * @return the cleaned token.
	 */
	private static String cleanToken(String tokens){
		tokens = tokens.replaceAll("\n|\r", " ");
		tokens = tokens.replaceAll("\"", "");
		tokens = tokens.replaceAll("&lt;", "");
		tokens = tokens.replaceAll("&gt;", "");
		tokens = tokens.replaceAll("\\+", "");
		tokens = tokens.replaceAll("\\(|\\)", "");
		tokens = tokens.replaceAll("\\*", "");
		tokens = tokens.replaceAll("'", "");
		tokens = tokens.replaceAll("&amp;", "");
		tokens = tokens.replaceAll("-", "");
		return tokens;
	}
	
	
	//COMPRESSION TECHNIQUES
	private static String removeNumbers(String tokens){
		tokens = tokens.replaceAll("[0-9]+", "");
		return tokens;
	}
	
	private static String caseFolding(String tokens){
		tokens = tokens.toLowerCase();
		return tokens;
	}
	
	@SuppressWarnings("unused")
	private static String apply30stopwords(String tokens){
		tokens = removeStopWords(tokens,30);
		return tokens;
	}
	
	@SuppressWarnings("unused")
	private static String apply150stopwords(String tokens){
		tokens = removeStopWords(tokens,150);
		return tokens;
	}

		
	@SuppressWarnings("unused")
	private void applyStemmer(){
		Stemmer stemmer;
		for(DocumentIndex docIndex : this.documentList){
			 String [] terms = docIndex.getTerms();
			 for(int i = 0;i<terms.length;i++){
				 String term = terms[i];
				 //starting the stemming
				 stemmer = new Stemmer();
				 stemmer.add(term.toCharArray(),term.length());
				 stemmer.stem();
				 terms[i] = stemmer.toString();
			 }
		}
	}
	
	private static String removeStopWords(String tokens, int numberOfStopwords){
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

		
		//to not break the program if we put bigger amt of stop words
		if(numberOfStopwords >= stopwords.length) numberOfStopwords = stopwords.length;
		
		for (int i = 0; i < numberOfStopwords; i++) {

			tokens = tokens.replaceAll(stopwords[i], " ");

		}
		
		return tokens;
	}

	/**
	 * Parses the document id from the NEWID tag from the reuters header.
	 * @param reutersHeader
	 * @return the docid (NEWID)
	 */
	private static String parseDocumentID(String reutersHeader) {
		Document doc = Jsoup.parse(reutersHeader);
		Element reuters = doc.select("REUTERS").first();
		return reuters.attr("NEWID");
	}
}
