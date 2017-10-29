package tokenizer;

public class DocumentIndex {

	private String[] terms;
	private String docID;

	public DocumentIndex(String docID, String[] terms) {
		this.terms = terms;
		this.docID = docID;
	}

	public String[] getTerms() {
		return terms;
	}

	public void setTerms(String[] terms) {
		this.terms = terms;
	}

	public String getDocID() {
		return docID;
	}

	public void setDocID(String docID) {
		this.docID = docID;
	}
}
