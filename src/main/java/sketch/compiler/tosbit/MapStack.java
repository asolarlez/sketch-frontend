package streamit.frontend.tosbit;

import java.util.HashMap;

class MapStack{
	HashMap curVT; //HashMap<String, String>
	MapStack kid;
	MapStack(){
		curVT = new HashMap();
	}
	String transName(String nm){
		String t = (String) curVT.get(nm);
		if(t != null)
			return t;
		else
			if(kid != null)
				return kid.transName(nm);
			else
				return nm;
	}
}