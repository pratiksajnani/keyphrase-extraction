import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Tagger {

	// max phrase length
	public static final int maxpl = 5;
	// min phrase length
	public static final int minpl = 2;
	// maps with frequency count
	public CustomMap abbreviationMap;
	public CustomMap wordMap;
	public CustomMap ignoredWordMap;
	public HashMap<Integer, String> reverseWordMap;
	public HashMap<String, Integer> phrases;
	// predefinedTags
	ArrayList<String> tags = new ArrayList<>();

	// stop words list
	ArrayList<String> stopWords = new ArrayList<>();

	void populateTags() {
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
	void populateStopWords() {
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
	 * @throws ClassNotFoundException 
	 */
	void preprocess() throws ClassNotFoundException {

		// initialize maps
		abbreviationMap = new CustomMap();
		wordMap = new CustomMap();
		ignoredWordMap = new CustomMap();
		reverseWordMap = new HashMap<Integer, String>();
		BufferedReader br = null;
		MaxentTagger tagger = null;

		try {
			// read the document
			br = new BufferedReader(new FileReader("1.txt"));

			// generate tagger object
			tagger = new MaxentTagger("taggers/bidirectional-distsim-wsj-0-18.tagger");

			String line = null;

			int depth = 0;
			int section = 0; //
			// iterate over file line by line
			while ((line = br.readLine()) != null) {
				String[] words = line.split(" ");
				if (words.length == 1) {
					section = getSection(words[0].trim(), 1);
				} else {
					if (words[0].contains("Abstract")) {
						line.replaceAll("Abstract: ", "");
						section = 1;
					}
					if (section != 4) {
						// filters the line using regex
						line = filter(line);
						// If filtered output is not empty
						if (!line.equals("") && !line.equals(" ")) {

							String taggedLine = tagger.tagString(line);

							// split tagged output on spaces to separate into
							// word-tag
							// pairs
							String[] wordsAndTags = taggedLine.split(" ");

							// temporary variables
							String[] wordTagPair;
							String candidate;

							for (String wordAndTag : wordsAndTags) {
								depth++;
								// split tagged line into word-tag pair
								// (Example.
								// Word/Tag becomes {"Word", "Tag"})
								wordTagPair = wordAndTag.split("/");

								// check stop words and bad tags
								if (!stopWords.contains(wordTagPair[0]) && !tags.contains(wordTagPair[1])) {

									// get the string associated with the tag -
									// candidate for word
									candidate = wordTagPair[0];

									// small words are either abbreviations or
									// they are
									// ignored
									if (candidate.length() < 4) {
										boolean matches = Pattern.compile("([A-Z])+").matcher(candidate).matches();
										if (matches && candidate.length() > 1) {
											// adds abbreviation
											if (!abbreviationMap.containsKey(candidate)) {
												abbreviationMap.put(candidate, 1, depth - 1, section);
											} else {
												abbreviationMap.get(candidate).updateCount();
												abbreviationMap.get(candidate).addDepth(depth - 1);
												abbreviationMap.get(candidate).setSection(section);
											}
										} else {
											// adds ignored word
											if (!ignoredWordMap.containsKey(candidate)) {
												ignoredWordMap.put(candidate, 1, depth - 1, section);
											} else {
												ignoredWordMap.get(candidate).updateCount();
												ignoredWordMap.get(candidate).addDepth(depth - 1);
												ignoredWordMap.get(candidate).setSection(section);
											}
										}
									}
									// big words are either abbreviations or
									// useful
									// words
									else {
										boolean matches = Pattern.compile("([A-Z])+").matcher(candidate).matches();
										if (matches) {
											// adds abbreviation
											String temp = candidate.toLowerCase();
											if (!wordMap.containsKey(temp)) {
												if (!abbreviationMap.containsKey(temp)) {
													abbreviationMap.put(candidate, 1, depth - 1, section);
												} else {
													abbreviationMap.get(candidate).updateCount();
													abbreviationMap.get(candidate).addDepth(depth - 1);
													abbreviationMap.get(candidate).setSection(section);
												}
											}
											if (!abbreviationMap.containsKey(candidate)) {
												abbreviationMap.put(candidate, 1, depth - 1, section);
											} else {
												abbreviationMap.get(candidate).updateCount();
												abbreviationMap.get(candidate).addDepth(depth - 1);
												abbreviationMap.get(candidate).setSection(section);
											}
										} else {
											// convert to lower case
											candidate = candidate.toLowerCase();

											// adds word
											if (!wordMap.containsKey(candidate)) {
												wordMap.put(candidate, 1, depth - 1, section);
												reverseWordMap.put(depth - 1, candidate);
											} else {
												wordMap.get(candidate).updateCount();
												wordMap.get(candidate).addDepth(depth - 1);
												wordMap.get(candidate).setSection(section);
												reverseWordMap.put(depth - 1, candidate);
											}
										}
									}
								}
							}

							// DEBUG - generate line output
							// printLineOutput(line);
						} // end of line processing
					} // end of abstract detection
				} // end of section detection
			} // end of while

			// remove words from abbreviations
			// if they exist as good words
			for (String word : abbreviationMap.map.keySet()) {
				String tempword = word.toLowerCase();
				if (wordMap.containsKey(tempword)) {
					wordMap.get(tempword).count += abbreviationMap.get(word).count;
					abbreviationMap.get(word).count = -1;
				}
			}

			// sort maps by value

			abbreviationMap.map = sortByValue(abbreviationMap.map);
			wordMap.map = sortByValue(wordMap.map);
			ignoredWordMap.map = sortByValue(ignoredWordMap.map);

			// extract phrases

			// initialize a map
			phrases = new HashMap<String, Integer>();

			for (String word : wordMap.keySet()) {

				Set<Integer> depths = wordMap.get(word).depth;

				
				
				boolean forward = true;

				for (Integer d : depths) {
					String phraseWords = word;
					for (int i = 1; i < maxpl && forward; i++) {
						if (reverseWordMap.containsKey(d + i)) {
							phraseWords = phraseWords + " " +reverseWordMap.get(d + i);
							System.out.println(phraseWords);
							if(phrases.containsKey(phraseWords)) {
								int count = phrases.get(phraseWords);
								phrases.replace(phraseWords, count + 1);
							}
							else {
								phrases.put(phraseWords, 1);
							}
						} else {
							forward = false;
						}
					}
				}

				// buffer to store phrase string
			}

			// DEBUG - prints maps (final output of preprocessing)
			printMaps();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 

	}

	// DEBUG code
	private void printLineOutput(String line) {
		StringBuffer abbrBuf = new StringBuffer("Abbreviations : \n");
		for (String abbr : abbreviationMap.keySet()) {
			abbrBuf.append(abbr + "\n");
		}

		StringBuffer wordBuf = new StringBuffer("Good Words : \n");

		for (String word : wordMap.keySet()) {
			wordBuf.append(word + "\n");
		}

		StringBuffer ignoredWordBuf = new StringBuffer("Ignored Words : \n");

		for (String ignoredWord : ignoredWordMap.keySet()) {
			ignoredWordBuf.append(ignoredWord + "\n");
		}
		if (!wordMap.isEmpty()) {
			System.out.println("Line Input : " + line + "\n");
			System.out.println("Line Output\n" + abbrBuf + "\n" + wordBuf + "\n" + ignoredWordBuf + "\n \n");
		}
	}

	// DEBUG code
	private void printMaps() {
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
		
		StringBuffer phraseBuf = new StringBuffer("Phrases : \n");
		for (String phrase : phrases.keySet()) {
			if(phrases.get(phrase) > 1){
				phraseBuf.append(phrase + ": " + phrases.get(phrase) + "\n");
			}
		}
		
		if (!wordMap.map.isEmpty()) {
			System.out.println("Line Output\n" + abbrBuf + "\n" + wordBuf + "\n" + ignoredWordBuf + "\n" + phraseBuf + "\n \n");
		}
	}

	// get section number
	private int getSection(String line, int sec) {
		int section = 0;
		if (sec == 1) {
			section = 5;
		}

		switch (line) {
		case "Introduction":
			if (sec == 1) {
				section = 2;
			}
			break;
		case "INTRODUCTION":
			if (sec == 1) {
				section = 2;
			}
			break;
		case "Conclusion":
			section = 3;
			break;
		case "References":
			section = 4;
			break;
		case "Abstract":
			section = 1;
			break;
		default:
			section = 5;
		}
		return section;
	}

	private String filter(String line) {
		// only include the following delims - ". , ; -"
		line = line.replaceAll("[^\\p{IsDigit}^\\p{IsAlphabetic}^\\.^\\s^\\-^\\,^\\']", "");

		// Replace commas
		line = line.replaceAll("\\,", " ");

		// Replace independent digits
		line = line.replaceAll("\\s([0-9])+\\s", " supaksprat ");

		// DEBUG code
		// System.out.println(line);

		// Replace a " - " with a " "
		line = line.replaceAll("\\s\\-\\s", " ");
		// DEBUG code
		// Replace more than one space with just one
		line = line.replaceAll("\\s(\\s)+", " ").trim();
		return line;
	}
}
