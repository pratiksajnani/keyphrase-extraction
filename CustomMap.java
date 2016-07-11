import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomMap {
	public Map<String, Metrics> map;

	public CustomMap() {
		map = new HashMap<String, Metrics>();
	}

	public void update(String key) {
		map.get(key).updateCount();
	}

	public void put(String key, int count, int initialDepth, int section) {
		Set<Integer> depth = new HashSet<Integer>();
		depth.add(initialDepth);
		if (!map.containsKey(key)) {
			map.put(key, new Metrics(count, depth, section));
		} else {
			map.replace(key, new Metrics(count, depth, section));
		}
	}

	public boolean containsKey(String candidate) {
		return map.containsKey(candidate);
	}

	public Metrics get(String candidate) {
		return map.get(candidate);
	}

	public Set<String> keySet() {
		// TODO Auto-generated method stub
		return map.keySet();
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return map.isEmpty();
	}
}
