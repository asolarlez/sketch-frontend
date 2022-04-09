package sketch.transformer;

import java.util.Map;

public class MapParam extends Param {
	Map<String, Param> map;

	public MapParam(Map<String, Param> _map)
	{
		map = _map;
	}

//	public String toString() {
//		String ret = new String();
//
//		return identifier.toString();
//	}
}
