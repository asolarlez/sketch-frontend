package sketch.util;

import java.util.HashMap;

public class DisjointSet<E> {

    HashMap<E, Node<E>> map;
    
    private static class Node<E> {
        Node<E> parent;
        E obj;
        int rank = 0;
        public Node(E obj) {
            this.obj = obj;
            this.parent = this;
        }
    }
    
    protected Node<E> getNode(E x) {
        Node<E> node;
        if (!map.containsKey(x)) {
            node = new Node<E>(x);
            map.put(x, node);
        } else {
            node = map.get(x);
        }
        return node;
    }
    
    public DisjointSet() {
        map = new HashMap<E, Node<E>>();
    }
    
    public void union(E x, E y) {
        
        Node<E> xRoot = getNode(find(x));
        Node<E> yRoot = getNode(find(y));
        
        if (xRoot.rank > yRoot.rank)
            yRoot.parent = xRoot;
        else if (xRoot.rank < yRoot.rank)
            xRoot.parent = yRoot;
        else if (xRoot != yRoot) {
            yRoot.parent = xRoot;
            xRoot.rank++;
        }
    }
    
    public E find(E x) {
        Node<E> xNode = getNode(x);
        if (xNode.parent == xNode)
            return xNode.obj;
        else {
            xNode.parent = getNode(find(xNode.parent.obj));
            return xNode.parent.obj;
        }
    }
}
