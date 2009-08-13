/**
 *
 */
package sketch.util;

/**
 * An undirected graph where each vertex is labeled with an int color.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class UndirectedColoredGraph<T> extends UndirectedGraph<T> {
	@Override
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
