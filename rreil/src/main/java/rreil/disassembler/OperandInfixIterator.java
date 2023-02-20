package rreil.disassembler;

/* From zynamics zylib */
import java.util.Stack;

import javalx.data.products.P2;
import rreil.disassembler.OperandTree.Node;

public class OperandInfixIterator {
  private final Stack<P2<Node, Integer>> m_traversalStack = new Stack<P2<Node, Integer>>();
  private final Node root;
  private boolean started = false;

  public OperandInfixIterator (final Node root) {
    this.root = root;
  }

  private void pushLongestPathFrom (final Node node) {
    Node current = node;
    do {
      m_traversalStack.push(new P2<Node, Integer>(current, 0));
      if (current.getChildren().size() <= 1)
        break;
      current = current.getChildren().get(0);
    } while (true);
  }

  public Node current () {
    return m_traversalStack.lastElement()._1();
  }

  public boolean next () {
    if (!started) {
      pushLongestPathFrom(root);
      started = true;
    } else {
      if (m_traversalStack.empty()) {
        throw new RuntimeException("Internal Error: Traversal already finished");
      }
      // The top of the stack always contains the last processed node.
      // To find out what to do next, we pop the last processed node off the
      // stack and examine it.
      P2<Node, Integer> lastProcessed = m_traversalStack.pop();
      // The node that was last processed.
      Node lastProcessedNode = lastProcessed._1();
      // The number of children of the last processed node that were
      // already processed.
      int lastProcessedChildrenProcessed = lastProcessed._2();
      if (lastProcessedChildrenProcessed < lastProcessedNode.getChildren().size()) {
        // If we're here, there are more children to process. Now we have
        // to distinguish between nodes with two or more children and nodes
        // with just one child. Those nodes with two or more children are
        // infix operators, meaning that we have to process them after
        // each but the last of their children. Nodes with just one child
        // are prefix operators and were already handled when moving down
        // through the tree.
        if (lastProcessed._1().getChildren().size() > 1) {
          // We're dealing with an infix operator. The infix operator
          // must be revisited after each of its children is processed.
          // That's why it needs to go back onto the stack.
          m_traversalStack.add(lastProcessed);
        }
        pushLongestPathFrom(lastProcessedNode.getChildren().get(lastProcessedChildrenProcessed));
        return true;
      } else {
        if (m_traversalStack.empty()) {
          // That's it. The stack is empty and all children of the last
          // processed node were processed previously. We're done.
          return false;
        } else {
          // If the stack is not empty we need to pop all completely
          // processed nodes off the stack. Those are the nodes whose
          // children were all completely processed before.
          do {
            final P2<Node, Integer> parent = m_traversalStack.pop();
            if (parent._2() < parent._1().getChildren().size() - 1) {
              // We found a node that still needs processing. Increase
              // its number of processed children and push it back onto
              // the stack.
              m_traversalStack.push(new P2<Node, Integer>(parent._1(), parent._2() + 1));
              return true;
            }
          } while (!m_traversalStack.empty());
        }
      }
    }
    return !m_traversalStack.empty();
  }
}
