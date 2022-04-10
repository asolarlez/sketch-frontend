package sketch.transformer;

import java.util.Map;
import java.util.TreeMap;

public class MapParam extends Param {
	Map<String, Param> map;

	public MapParam(Map<String, Param> _map)
	{
		map = _map;
	}

	public String toString() {
		String ret = new String();

		ret += "{";
		boolean is_first = true;
		for (String key : map.keySet()) {
			if (!is_first) {
				ret += ", ";
			}
			is_first = false;
			ret += '"' + key + '"' + " : " + map.get(key);
		}
		ret += "}";
		return ret;
	}

	public Map<String, String> get_map_string_to_string() {
		Map<String, String> ret = new TreeMap<String, String>();
		for (String key : map.keySet()) {
			ret.put(key, ((StringParam) map.get(key)).get_string());
		}
		return ret;
	}
}
