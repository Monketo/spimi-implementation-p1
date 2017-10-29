package query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryCommand {
	
	
	private Map<String,List<Integer>> dictionary;
	
	/**
	 * basic access to get the dictionary
	 * @return
	 */
	public Map<String, List<Integer>> getDictionary() {
		return dictionary;
	}

	/**
	 * setter for the dictionary
	 * @param dictionary
	 */
	public void setDictionary(Map<String, List<Integer>> dictionary) {
		this.dictionary = dictionary;
	}

	/**
	 * Performs a single keyword query
	 * @param query the keyword query, needs to be in "keyword" format
	 * @return List<Integer> the result postings , with duplicate postings removed.
	 */
	public List<Integer> performKeywordQuery(String query){
		List<Integer> results = null;
		
		results = this.dictionary.get(query.toLowerCase());
		
		//removing duplicates
		if(results!=null) {
			List<Integer> noDuplicatesResults = new ArrayList<>(new LinkedHashSet<>(results));	
			return noDuplicatesResults;
		}else return results;
			   
		
	}
	
	/**
	 * Perfoms an AND query
	 * @param query --> needs to be in "term1 AND term2 AND term3.." format
	 * @return List<Integer> the result postings, with duplicate postings removed.
	 */
	public List<Integer> performAndQuery(String query){
		List<Integer> results = null;
		
		//intersecting the results, giving the union
		
		String [] queryTerms = query.split(" AND ");
		
		
		for (int i = 0; i < queryTerms.length; i++) {
			queryTerms[i] = queryTerms[i].toLowerCase();
			if(results == null){
				results = this.dictionary.get(queryTerms[i]);
			}else if(this.dictionary.get(queryTerms[i]) != null){
				results = intersection(results,this.dictionary.get(queryTerms[i]));
			}
			
		}
		
		//removing duplicates
				if(results!=null) {
					List<Integer> noDuplicatesResults = new ArrayList<>(new LinkedHashSet<>(results));	
					return noDuplicatesResults;
				}else return results;
					   
	}
	

	/**
	 * Perfoms an OR query
	 * @param query --> needs to be in "term1 OR term2 OR term3.." format
	 * @return List<Integer> the result postings, ordered by term frequency.
	 */
	public List<Integer> performOrQuery(String query){
		List<Integer> results = null;
		
		//just a union of the results
		String [] queryTerms = query.split(" OR ");
		
		
		for (int i = 0; i < queryTerms.length; i++) {
			queryTerms[i] = queryTerms[i].toLowerCase();			
			if(results == null && this.dictionary.get(queryTerms[i])!=null){
				results = this.dictionary.get(queryTerms[i]);
			}else if(this.dictionary.get(queryTerms[i]) != null){
				results = union(results,this.dictionary.get(queryTerms[i]));
			}
			
		}
		List<Integer> sortedPostings = sortByTermFrequency(results);

		return sortedPostings;	
	}
	
	/**
	 * Basic union algorithm
	 * @param list1
	 * @param list2
	 * @return union of list1 and list2
	 */
	public List<Integer> union(List<Integer> list1, List<Integer> list2) {
        Set<Integer> set = new HashSet<Integer>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<Integer>(set);
    }

	/**
	 * Basic intersection algorithm
	 * @param list1
	 * @param list2
	 * @return union of list1 and list2
	 */
    public List<Integer> intersection(List<Integer> list1, List<Integer> list2) {
        List<Integer> list = new ArrayList<Integer>();

        for (Integer t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
	
    /**
     * Sorts result by term frequency. The postings list will start with the document in which the queried term will appear the most.
     * @param postingsList the postings list containing the duplicate document id's
     * @return the sorted postings list by term frequency
     */
	public static List<Integer> sortByTermFrequency(List<Integer> postingsList){
		
		List<Integer> sortedPostings = new ArrayList<Integer>();

		Map<Integer,Integer> postingCountMap = new LinkedHashMap<Integer,Integer>();

		for(Integer posting : postingsList){
			Integer count = postingCountMap.get(posting);
			if(count == null){
				count = 1;
				postingCountMap.put(posting,count);
			}else{
				count++;
				postingCountMap.put(posting,count);
			}

		}

		//sort by biggest count
		Map<Integer,Integer> sorted =
			    postingCountMap.entrySet().stream()
			       .sorted(Map.Entry.comparingByValue()).collect(Collectors.toMap(e -> e.getKey(),
                                           e -> e.getValue()));
		
		

		Set<Integer> postings = sorted.keySet();

		//convert postings to list<integer>
		sortedPostings.addAll(postings);
		
		return sortedPostings;
	}
	
}
