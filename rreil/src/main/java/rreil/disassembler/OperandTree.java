package rreil.disassembler;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javalx.numeric.Interval;



/**
 */
public class OperandTree {
  private final Node root;

  public OperandTree (final Node root) {
    this.root = root;
  }

  public Node getRoot () {
    return root;
  }

  public static enum Type {
    Size, Mem, Immf, Immi, Immr, Sym, Op
  }

  @Override public String toString () {
    return "Tree{" + root + '}';
  }

  public static final class Node {
    private final Type type;
    private final Object data;
    private final List<Node> children;

    private Node (final Type ty, final Object data, final List<Node> children) {
      this.type = ty;
      this.data = data;
      this.children = Collections.unmodifiableList(children);
    }

    public Object getData () {
      return data;
    }

    public Type getType () {
      return type;
    }

    public List<Node> getChildren () {
      return children;
    }

    public Node child (int n) {
      return children.get(n);
    }

    public StringBuilder asString (final StringBuilder pretty) {
      switch (type) {
        case Immf:
        case Immi:
          assert children.isEmpty() : "Invalid leaf node: immediate node has children";
          pretty.append(((Number) data).toString());
          break;
        case Immr:
          assert children.isEmpty() : "Invalid leaf node: immediate node has children";
          pretty.append(((Interval) data).toString());
          break;
        case Sym:
          assert children.isEmpty() : "Invalid leaf node: symbol node has children";
          pretty.append(data.toString());
          break;
        case Size:
          pretty.append('(').append(((Number) data).toString());
          for (Node n : children) {
            n.asString(pretty.append(' '));
          }
          pretty.append(')');
          break;
        case Op:
          pretty.append('(').append(data.toString());
          for (Node n : children) {
            n.asString(pretty.append(' '));
          }
          pretty.append(')');
          break;
        case Mem:
          pretty.append('*').append(((Number) data).toString()).append('(');
          final Iterator<Node> it = children.iterator();
          while (it.hasNext()) {
            final Node n = it.next();
            n.asString(pretty);
            if (it.hasNext())
              pretty.append(' ');
          }
          pretty.append(')');
          break;
      }
      return pretty;
    }

    @Override public String toString () {
      StringBuilder pretty = new StringBuilder();
      return asString(pretty).toString();
    }
  }

  public static final class NodeBuilder {
    private Type type;
    private Object data;
    private final List<Node> children = new LinkedList<Node>();

    public NodeBuilder type (Type type) {
      this.type = type;
      return this;
    }

    public NodeBuilder data (Object data) {
      this.data = data;
      return this;
    }

    public NodeBuilder link (Node child) {
      this.children.add(child);
      return this;
    }

    public Node build () {
      assert type != null : "null type";
      return new Node(type, data, children);
    }
  }
}
