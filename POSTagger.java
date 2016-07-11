import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

class CustomMap {
	public Map<String, Info> map;

	public CustomMap() {
		map = new HashMap<String, Info>();
	}

	public void update(String key) {
		map.get(key).update();
	}

	public void put(String key) {
		map.put(key, new Info(1));
	}

	public void put(String key, int count) {
		if (!map.containsKey(key)) {
			map.put(key, new Info(count));
		} else {
			map.replace(key, new Info(count));
		}
	}

	public void put(String key, int count, int depth) {
		if (!map.containsKey(key)) {
			map.put(key, new Info(count, depth));
		} else {
			map.replace(key, new Info(count, depth));
		}
	}

	public void put(String key, int count, int depth, int section) {
		if (!map.containsKey(key)) {
			map.put(key, new Info(count, depth, section));
		} else {
			map.replace(key, new Info(count, depth, section));
		}
	}

	public boolean containsKey(String candidate) {
		return map.containsKey(candidate);
	}

	public Info get(String candidate) {
		return map.get(candidate);
	}
}

class Info implements Comparable<Info> {
	int objectType; // used for printing
	int count; // frequency of occurrence
	int depth; // expressed as number of words that occur before it
	int section; // integer representing the section of document

	public Info(int count, int depth, int section) {
		this.count = count;
		this.depth = depth;
		this.section = section;
		this.objectType = 3;
	}

	public Info(int count, int depth) {
		this.count = count;
		this.depth = depth;
		this.objectType = 2;
	}

	public Info(int count) {
		this.count = count;
		this.objectType = 1;
	}

	@Override
	public int compareTo(Info info) {
		return Integer.compare(this.count, info.count);
	}

	public void update() {
		this.count = this.count + 1;
	}
}

public class POSTagger {

	// maps with frequency count
	public CustomMap abbreviationMap;
	public CustomMap wordMap;
	public CustomMap ignoredWordMap;

	// predefinedTags
	ArrayList<String> tags = new ArrayList<>();

	// stop words list
	ArrayList<String> stopWords = new ArrayList<>();

	private void populateTags() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("abbrev.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				tags.add(line.trim());
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/*
	 * WARNING : This code works only with Java 8 !
	 * 
	 * Taken from
	 * http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-
	 * java Please check the link for code for Java 6 and 7. I haven't tried but
	 * it should be usable
	 */

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		Map<K, V> result = new LinkedHashMap<>();
		Stream<Map.Entry<K, V>> st = map.entrySet().stream();

		st.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.forEachOrdered(e -> result.put(e.getKey(), e.getValue()));

		return result;
	}

	/**
	 * This method reads stop-words from a stored text file "stopwords_en.txt".
	 * It updates a global arraylist of stopwords. Stop words are removed by
	 * using this list.
	 * 
	 */
	private void populateStopWords() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("stopwords_en.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				stopWords.add(line.trim());
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The method reads in line by line and only accepts characters that are
	 * alphabetic or one of the following delimiters - . \s - , ; where \s is a
	 * single space.
	 * 
	 * The result is update of the following three hash maps stored as global
	 * variables -
	 * 
	 * abbreviationMap - holds abbreviations with their frequency count
	 * 
	 * ignoredWordMap - holds words we will not use with their frequency count.
	 * This is only for testing purposes
	 * 
	 * wordMap - holds meaningful words(with frequency count) that we want to
	 * use to generate key phrases and for further processing.
	 */
	public void preprocess() {

		// initialize maps
		abbreviationMap = new CustomMap();
		wordMap = new CustomMap();
		ignoredWordMap = new CustomMap();

		BufferedReader br = null;
		MaxentTagger tagger = null;

		try {
			// read the document
			br = new BufferedReader(new FileReader("1.txt"));

			// generate tagger object
			tagger = new MaxentTagger("taggers/bidirectional-distsim-wsj-0-18.tagger");

			String line = null;

			// iterate over file line by line
			while ((line = br.readLine()) != null) {

				// only include the following delims - ". , ; -"
				line = line.replaceAll("[^p\\{IsNumeric}^\\p{IsAlphabetic}^\\.^\\s^\\-^\\,^\\;]", "").trim();

				// Replace commas
				line = line.replaceAll("\\,", " ");

				// Replace a " - " with a " "
				line = line.replaceAll("\\s\\-\\s", " ");

				// Replace more than one space with just one
				line = line.replaceAll("\\s(\\s)+", " ");

				// If filtered output is not empty
				if (!line.equals("") && !line.equals(" ")) {

					String taggedLine = tagger.tagString(line);

					// split tagged output on spaces to separate into word-tag
					// pairs
					String[] wordsAndTags = taggedLine.split(" ");

					// temporary variables
					String[] wordTagPair;
					String candidate;

					for (String wordAndTag : wordsAndTags) {

						// split tagged line into word-tag pair (Example.
						// Word/Tag becomes {"Word", "Tag"})
						wordTagPair = wordAndTag.split("/");

						// check stop words and bad tags
						if (!stopWords.contains(wordTagPair[0]) && !tags.contains(wordTagPair[1])) {

							// get the string associated with the tag -
							// candidate for word
							candidate = wordTagPair[0];

							// small words are either abbreviations or they are
							// ignored
							if (candidate.length() < 4) {
								boolean matches = Pattern.compile("([A-Z])+").matcher(candidate).matches();
								if (matches && candidate.length() > 1) {
									// add abbreviation
									if (!abbreviationMap.containsKey(candidate)) {
										abbreviationMap.put(candidate);
									} else {
										abbreviationMap.get(candidate).update();
									}
								} else {
									// add ignored word
									if (!ignoredWordMap.containsKey(candidate)) {
										ignoredWordMap.put(candidate);
									} else {
										ignoredWordMap.get(candidate).update();
									}
								}
							}
							// big words are either abbreviations or useful
							// words
							else {
								boolean matches = Pattern.compile("([A-Z])+").matcher(candidate).matches();
								if (matches) {
									// add abbreviation
									if (!abbreviationMap.containsKey(candidate)) {
										abbreviationMap.put(candidate, 1);
									} else {
										abbreviationMap.get(candidate).update();
									}
								} else {
									// convert to lower case
									candidate = candidate.toLowerCase();

									// add word
									if (!wordMap.containsKey(candidate)) {
										wordMap.put(candidate, 1);
									} else {
										wordMap.get(candidate).update();
										;
									}
								}
							}
						}
					}

					// generate line output - TEST CODE - can be enabled to see
					// line by line results
					/*
					 * StringBuffer abbrBuf = new StringBuffer(
					 * "Abbreviations : \n");
					 * 
					 * for (String abbr : abbreviationMap.keySet()) {
					 * abbrBuf.append(abbr + "\n"); }
					 * 
					 * StringBuffer wordBuf = new StringBuffer("Good Words : \n"
					 * );
					 * 
					 * for (String word : wordMap.keySet()) {
					 * wordBuf.append(word + "\n"); }
					 * 
					 * StringBuffer ignoredWordBuf = new StringBuffer(
					 * "Ignored Words : \n");
					 * 
					 * for (String ignoredWord : ignoredWordMap.keySet()) {
					 * ignoredWordBuf.append(ignoredWord + "\n"); } if
					 * (!wordMap.isEmpty()) { System.out.println("Line Input : "
					 * + line + "\n"); System.out .println("Line Output\n" +
					 * abbrBuf + "\n" + wordBuf + "\n" + ignoredWordBuf +
					 * "\n \n"); }
					 */

				} // end of line processing
			} // end of while

			// sort maps by value

			abbreviationMap.map = sortByValue(abbreviationMap.map);
			wordMap.map = sortByValue(wordMap.map);
			ignoredWordMap.map = sortByValue(ignoredWordMap.map);

			// prints results of this stage to console

			StringBuffer abbrBuf = new StringBuffer("Abbreviations : \n");

			for (String abbr : abbreviationMap.map.keySet()) {
				abbrBuf.append(abbr + ":" + abbreviationMap.get(abbr).count + "\n");
			}

			StringBuffer wordBuf = new StringBuffer("Good Words : \n");

			for (String word : wordMap.map.keySet()) {
				wordBuf.append(word + " : " + wordMap.get(word).count + "\n");
			}

			StringBuffer ignoredWordBuf = new StringBuffer("Ignored Words : \n");

			for (String ignoredWord : ignoredWordMap.map.keySet()) {
				ignoredWordBuf.append(ignoredWord + ": " + ignoredWordMap.get(ignoredWord).count + "\n");
			}
			if (!wordMap.map.isEmpty()) {
				System.out.println("Line Output\n" + abbrBuf + "\n" + wordBuf + "\n" + ignoredWordBuf + "\n \n");
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		POSTagger tagger = new POSTagger();

		// get predefined tags to use with tagger
		tagger.populateTags();

		// get stop words to remove them
		tagger.populateStopWords();

		// initializes the word and abbreviation hash maps with frequency counts
		tagger.preprocess();

	}
}
