/**
 *
 */
package sketch.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

/**
 * An undirected graph where each vertex is labeled with an int color.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class UndirectedColoredGraph<T> {
    Map<T, Vertex<T>> vertices = new HashMap<T, Vertex<T>>();

    public void addEdge(T v1, T v2) {
        Vertex<T> ver1 = find(v1), ver2 = find(v2);
        ver1.neighbors.add(ver2);
        ver2.neighbors.add(ver1);
    }

    public Set<T> neighbors(T v) {
        Set<T> nbors = new HashSet<T>();
        for (Vertex<T> n : find(v).neighbors)
            nbors.add(n.key);
        return nbors;
    }

    public Set<Vertex<T>> vertices() {
        return new HashSet<Vertex<T>>(vertices.values());
    }

    // reachability, etc.

    public Vertex<T> find(T k) {
        if (vertices.containsKey(k))
            return vertices.get(k);
        else {
            Vertex<T> v = makeVertex(k);
            vertices.put(k, v);
            return v;
        }
    }

    public UndirectedColoredGraph<T> color() {
        UndirectedColoredGraph<T> cG = new UndirectedColoredGraph<T>();
        PriorityQueue<Vertex<T>> pq =
                new PriorityQueue<Vertex<T>>(16, new Comparator<Vertex<T>>() {
                    /** Sorts DESCENDING by degree */
                    public int compare(Vertex<T> v1, Vertex<T> v2) {
                        return v2.degree() - v1.degree();
                    }
                });
        pq.addAll(vertices());

        while (!pq.isEmpty()) {
            T v = pq.remove().item();
            ColoredVertex<T> cv = (ColoredVertex<T>) cG.find(v);
            Set<Integer> nborColors = new TreeSet<Integer>();

            for (T n : neighbors(v)) {
                if (v.equals(n))
                    continue;
                cG.addEdge(v, n);
                ColoredVertex<T> cn = (ColoredVertex<T>) cG.find(n);
                nborColors.add(cn.color());
            }

            int lastColor = -1;
            for (int nborColor : nborColors) {
                if (nborColor - lastColor > 1)
                    break;
                lastColor = nborColor;
            }
            cv.setColor(lastColor + 1);
        }

        return cG;
    }

    public String toString() {
        String s = "graph G {\n";

        for (Vertex<T> v : vertices()) {
            String vs = v.toString();
            for (Vertex<T> n : v.neighbors)
                s += "  " + vs + " -- " + n + "\n";
            if (v.neighbors.size() == 0)
                s += "  " + vs + "\n";
        }

        return s + "}";
    }

    static public class Vertex<TV> {
        private TV key;
        private Set<Vertex> neighbors = new HashSet<Vertex>();

        public Vertex(TV v) {
            this.key = v;
        }

        public TV item() {
            return key;
        }

        public int degree() {
            return neighbors.size();
        }

        public int hashCode() {
            return key.hashCode();
        }

        public String toString() {
            return key.toString();
        }
    }

	public ColoredVertex<T> makeVertex (T key) {
		return new ColoredVertex<T> (key);
	}

	public int color (T v) {
		return ((ColoredVertex<T>) find (v)).color;
	}

	public static class ColoredVertex<TV> extends Vertex<TV> {
		private int color = -1;

		public ColoredVertex (TV key) {
			super (key);
		}

		public int color () {
			return color;
		}

		protected void setColor (int color) {
			this.color = color;
		}

		public String toString () {
			return super.toString () +"_color"+ color;
		}
	}
}
