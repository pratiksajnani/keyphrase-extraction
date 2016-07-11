
public class Test {
	public static void main(String[] args) throws ClassNotFoundException {
		Tagger tagger = new Tagger();

		// get predefined tags
		tagger.populateTags();

		// get stop words
		tagger.populateStopWords();

		// initializes the word and abbreviation hash maps with frequency counts
		tagger.preprocess();

	}
}
