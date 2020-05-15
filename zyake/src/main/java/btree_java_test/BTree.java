package btree_java_test;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * The {@code BTree} class represents an ordered symbol table of generic
 * key-value pairs. It supports the <em>put</em>, <em>get</em>,
 * <em>contains</em>, <em>size</em>, and <em>is-empty</em> methods. A symbol
 * table implements the <em>associative array</em> abstraction: when associating
 * a value with a key that is already in the symbol table, the convention is to
 * replace the old value with the new value. Unlike {@link java.util.Map}, this
 * class uses the convention that values cannot be {@code null}—setting the
 * value associated with a key to {@code null} is equivalent to deleting the key
 * from the symbol table.
 * <p>
 * This implementation uses a B-tree. It requires that the key type implements
 * the {@code Comparable} interface and calls the {@code compareTo()} and method
 * to compare two keys. It does not call either {@code equals()} or
 * {@code hashCode()}. The <em>get</em>, <em>put</em>, and <em>contains</em>
 * operations each make log<sub><em>m</em></sub>(<em>n</em>) probes in the worst
 * case, where <em>n</em> is the number of key-value pairs and <em>m</em> is the
 * branching factor. The <em>size</em>, and <em>is-empty</em> operations take
 * constant time. Construction takes constant time.
 * <p>
 * For additional documentation, see
 * <a href="https://algs4.cs.princeton.edu/62btree">Section 6.2</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class BTree<Key extends Comparable<Key>, Value>  {
    // max children per B-tree node = M-1
    // (must be even and greater than 2)
    private static final int maxChildrenPerBTreeNode = 4;

    private Node root;       // root of the B-tree
    private int height;      // height of the B-tree
    private int n;           // number of key-value pairs in the B-tree

    // helper B-tree node data type
    private static final class Node {
        private int numberOfChildren;                             // number of children
        private Entry[] children = new Entry[maxChildrenPerBTreeNode];   // the array of children

        // create a node with k children
        private Node(int k) {
            numberOfChildren = k;
        }

        @Override
        public String toString() {
            return String.format("node[numberOfChildren=%d, children=%s]", numberOfChildren, Arrays.toString(children));
        }
    }

    // internal nodes: only use key and next
    // external nodes: only use key and value
    private static class Entry {
        private Comparable key;
        private final Object val;
        private Node next;     // helper field to iterate over array entries
        public Entry(Comparable key, Object val, Node next) {
            this.key  = key;
            this.val  = val;
            this.next = next;
        }

        @Override
        public String toString() {
            return String.format("entry[key=%s, value=%s, next=%s]", key, val, next);
        }
    }

    /**
     * Initializes an empty B-tree.
     */
    public BTree() {
        root = new Node(0);
    }
 
    /**
     * Returns true if this symbol table is empty.
     * @return {@code true} if this symbol table is empty; {@code false} otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns the number of key-value pairs in this symbol table.
     * @return the number of key-value pairs in this symbol table
     */
    public int size() {
        return n;
    }

    /**
     * Returns the height of this B-tree (for debugging).
     *
     * @return the height of this B-tree
     */
    public int height() {
        return height;
    }


    /**
     * Returns the value associated with the given key.
     *
     * @param  key the key
     * @return the value associated with the given key if the key is in the symbol table
     *         and {@code null} if the key is not in the symbol table
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public Value get(Key key) {
        if (key == null) throw new IllegalArgumentException("argument to get() is null");
        return search(root, key, height);
    }

    private Value search(Node x, Key key, int ht) {
        Entry[] children = x.children;

        // external node
        if (ht == 0) {
            for (int j = 0; j < x.numberOfChildren; j++) {
                if (eq(key, children[j].key)) return (Value) children[j].val;
            }
        }

        // internal node
        else {
            for (int j = 0; j < x.numberOfChildren; j++) {
                if (j + 1 == x.numberOfChildren || less(key, children[j + 1].key))
                    return search(children[j].next, key, ht - 1);
            }
        }
        return null;
    }


    /**
     * Inserts the key-value pair into the symbol table, overwriting the old value
     * with the new value if the key is already in the symbol table.
     * If the value is {@code null}, this effectively deletes the key from the symbol table.
     *
     * @param  key the key
     * @param  val the value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public void put(Key key, Value val) {
        System.out.printf("start put..., key=%s, Value=%s\n", key, val);

        if (key == null) throw new IllegalArgumentException("argument key to put() is null");
        Node u = insert(root, key, val, height); 
        n++;
        if (u == null) return;

        // need to split root
        System.out.println("Splitting root node...");
        Node newNode = new Node(2);
        newNode.children[0] = new Entry(root.children[0].key, null, root);
        newNode.children[1] = new Entry(u.children[0].key, null, u);
        root = newNode;
        height ++;

        System.out.printf("root node splitted...: new root node=%s\n", newNode);

    }

    private Node insert(Node node, Key key, Value val, int height) {
        System.out.printf("start insert..., Node=%s, key=%s, Value=%s, height=%d\n", node, key, val, height);
        
        int childrenIndex;
        Entry newEntry = new Entry(key, val, null);

        // external node
        if (height == 0) {
            System.out.println("external node found. Checking children...");
            for (childrenIndex = 0; childrenIndex < node.numberOfChildren; childrenIndex ++) {
                boolean currentKeyIsLessThan = less(key, node.children[childrenIndex].key);
                System.out.printf("Two keys were compared.:current key=%s, compared key=%s\n", key, node.children[childrenIndex].key);
                if (currentKeyIsLessThan) break;
            }
        }

        // internal node
        else {
            System.out.println("internal node found. Checking children...");
            for (childrenIndex = 0; childrenIndex < node.numberOfChildren; childrenIndex++) {
                if ((childrenIndex + 1 == node.numberOfChildren) || less(key, node.children[childrenIndex + 1].key)) {
                    Node u = insert(node.children[childrenIndex ++].next, key, val, height - 1);
                    if (u == null) return null;

                    // extract middle key(first element of new node)
                    newEntry.key = u.children[0].key;
                    newEntry.next = u;
                    break;
                }
            }
        }

        System.out.printf("moving nodes...: before=%s\n", Arrays.toString(node.children));
        for (int childrenTraverseIndex = node.numberOfChildren; childrenTraverseIndex > childrenIndex; childrenTraverseIndex --) {
            node.children[childrenTraverseIndex] = node.children[childrenTraverseIndex - 1];
        }
        node.children[childrenIndex] = newEntry;
        node.numberOfChildren ++;

        System.out.printf("New node inserted:%s\n", Arrays.toString(node.children));

        if (node.numberOfChildren < maxChildrenPerBTreeNode) {
            System.out.println("Node is not filled up. returning...");
            return null;
        }
        else         return split(node);
    }

    // split node in half
    private Node split(Node existingNode) {
        System.out.println("Splitting a node...");
        // heigher part of half
        Node newNode = new Node(maxChildrenPerBTreeNode/2);
        existingNode.numberOfChildren = maxChildrenPerBTreeNode/2;
        for (int leftNodeIndex = 0; leftNodeIndex < maxChildrenPerBTreeNode/2; leftNodeIndex ++)
            newNode.children[leftNodeIndex] = existingNode.children[maxChildrenPerBTreeNode/2 + leftNodeIndex]; 
        
        // cleanup unnecessary references.
        for (int leftOverEntryIndex = existingNode.numberOfChildren ; leftOverEntryIndex < existingNode.children.length; leftOverEntryIndex++) {
            existingNode.children[leftOverEntryIndex] = null;
        }

        System.out.printf("new node=%s, existing node=%s\n", newNode, existingNode);
        return newNode;    
    }

    /**
     * Returns a string representation of this B-tree (for debugging).
     *
     * @return a string representation of this B-tree.
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("root: ");
        for (int childrenIndex = 0; childrenIndex < root.numberOfChildren ; childrenIndex ++) {
            s.append(root.children[childrenIndex].key);
            s.append(",");
        }
        s.deleteCharAt(s.length() - 1);
        s.append('\n');
        return s.append(toString(root, height, "") + "\n").toString();
    }

    private String toString(Node h, int ht, String indent) {
        StringBuilder s = new StringBuilder();
        Entry[] children = h.children;

        if (ht == 0) {
            for (int j = 0; j < h.numberOfChildren; j ++) {
                s.append(indent + children[j].key + " " + children[j].val + "\n");
            }
        }
        else {
            for (int j = 0; j < h.numberOfChildren; j ++) {
                if (j > 0) s.append(indent + "(" + children[j].key + ")\n");
                s.append(toString(children[j].next, ht-1, indent + "     "));
            }
        }
        return s.toString();
    }


    // comparison functions - make Comparable instead of Key to avoid casts
    private boolean less(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) < 0;
    }

    private boolean eq(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) == 0;
    }


    /**
     * Unit tests the {@code BTree} data type.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        final BTree<Integer, String> st = new BTree<Integer, String>();

        st.put(1, "128.112.136.12");
        st.put(2, "128.112.136.11");
        st.put(3,    "128.112.128.15");
        final PrintStream StdOut = System.out;

        StdOut.println("size:    " + st.size());
        StdOut.println("height:  " + st.height());
        StdOut.println(st);
        StdOut.println();

        st.put(4,     "209.052.165.60");

        StdOut.println("size:    " + st.size());
        StdOut.println("height:  " + st.height());
        StdOut.println(st);
        StdOut.println();

        st.put(5,     "209.052.165.60");
        st.put(6,     "209.052.165.60");
        st.put(7,     "209.052.165.60");
        st.put(8,     "209.052.165.60");
        st.put(9,     "209.052.165.60");
        st.put(10,     "209.052.165.60");

        
        StdOut.println("size:    " + st.size());
        StdOut.println("height:  " + st.height());
        StdOut.println(st);
        StdOut.println();


    }

}
