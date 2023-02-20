package rreil.disassembler;

import java.util.Stack;

import javalx.data.products.P2;
import rreil.disassembler.OperandTree.Node;
import rreil.disassembler.OperandTree.Type;

public class OperandPostorderIterator {
  private final Stack<P2<Node, Integer>> traversalStack = new Stack<P2<Node, Integer>>();
  private final Stack<Number> sizeScope = new Stack<Number>();
  private final Node root;
  private boolean started = false;

  public OperandPostorderIterator (final Node root) {
    this.root = root;
  }

  private void pushLeftmostPath (final Node node) {
    Node current = node;

    do {
      if (current.getType() == Type.Size)
        sizeScope.push((Number) current.getData());
      else if (current.getType() == Type.Mem)
        sizeScope.push((Number) current.getData());

      traversalStack.push(new P2<Node, Integer>(current, 0));

      if (current.getChildren().isEmpty())
        break;
      current = current.getChildren().get(0);
    } while (true);
  }

  public Node current () {
    return traversalStack.lastElement()._1();
  }

  public Number currentSizeScope () {
    return sizeScope.peek();
  }

  @SuppressWarnings("unchecked")
  public Stack<Number> getOperandSizeStack () {
    return (Stack<Number>) sizeScope.clone();
  }

  public boolean next () {
    if (!started) {

      pushLeftmostPath(root);

      started = true;
    } else {
      if (traversalStack.empty())
        throw new RuntimeException("Internal Error: Traversal already finished");

      final P2<Node, Integer> lastProcessed = traversalStack.pop();

      final Node lastProcessedNode = lastProcessed._1();

      final int lastProcessedChildrenProcessed = lastProcessed._2();

      if (lastProcessedNode.getType() == Type.Size || lastProcessedNode.getType() == Type.Mem)
        sizeScope.pop();

      if (lastProcessedChildrenProcessed >= lastProcessedNode.getChildren().size())
        if (traversalStack.empty())
          return false;
        else {
          final P2<Node, Integer> parent = traversalStack.pop();
          final Node parentNode = parent._1();
          final Integer parentChild = parent._2();

          traversalStack.push(new P2<Node, Integer>(parentNode, parentChild + 1));

          if (parentChild + 1 < parentNode.getChildren().size())
            pushLeftmostPath(parentNode.getChildren().get(parentChild + 1));

          // Handle infix operators with more than 2 operands
          if (parentChild > 1)
            traversalStack.push(parent);
        }
    }

    return !traversalStack.empty();
  }
}
