package sketch.transformer;

import java.util.Map;

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
			ret += key + " : " + map.get(key);
		}
		ret += "}";
		return ret;
	}
}
