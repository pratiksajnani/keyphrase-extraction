import java.util.Set;

public class Metrics implements Comparable<Metrics> {
	int objectType; // used for printing
	int count; // frequency of occurrence
	Set<Integer> depth; // expressed as number of words that occur before it
	int section; // integer representing the section of document

	public Metrics(int count, Set<Integer> depth, int section) {
		this.count = count;
		this.depth = depth;
		this.section = section;
		this.objectType = 3;
	}

	public Metrics(int count, Set<Integer> depth) {
		this.count = count;
		this.depth = depth;
		this.objectType = 2;
	}

	public Metrics(int count) {
		this.count = count;
		this.objectType = 1;
	}

	@Override
	public int compareTo(Metrics info) {
		return Integer.compare(this.count, info.count);
	}

	// updates the count
	public void updateCount() {
		this.count = this.count + 1;
	}

	// adds a positional depth
	public void addDepth(int d) {
		this.depth.add(d);
	}

	// set section number
	public void setSection(int section) {
		this.section = section;
	}
}
