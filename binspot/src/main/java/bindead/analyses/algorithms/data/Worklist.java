package bindead.analyses.algorithms.data;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import bindead.analyses.algorithms.AnalysisProperties;

/**
 * A list of items in LIFO order (stack) or using a priority queue.
 * Thus the evaluation order can be adjusted.
 */
public class Worklist<T extends Comparable<? super T>> {
  private final boolean useGlobalOrdering = AnalysisProperties.INSTANCE.processAddressesInOrder.isTrue();
  private final PriorityQueue<T> queue;
  private final ArrayDeque<T> oldQueue;
  private final Set<T> inQueue;

  public Worklist () {
    this.queue = new PriorityQueue<>();
    this.oldQueue = new ArrayDeque<>();
    this.inQueue = new HashSet<>();
  }

  public void enqueue (T element) {
    if (useGlobalOrdering)
      enqueueOrdered(element);
    else
      enqueueUnordered(element);
  }

  private void enqueueOrdered (T element) {
    if (inQueue.contains(element))
      return;
    inQueue.add(element);
    queue.add(element);
  }

  private void enqueueUnordered (T element) {
    // if an element is already in the queue then there are two possible strategies:
    // either move it to the front for it to be processed next or don't add it again
    if (inQueue.contains(element))
      oldQueue.remove(element);
    inQueue.add(element);
    oldQueue.push(element);
  }

  public T dequeue () {
    T element;
    if (useGlobalOrdering)
      element = queue.remove();
    else
      element = oldQueue.pop();
    inQueue.remove(element);
    return element;
  }

  public boolean isEmpty () {
    if (useGlobalOrdering)
      return queue.isEmpty();
    else
      return oldQueue.isEmpty();
  }

  @Override public String toString () {
    if (useGlobalOrdering)
      return "WORKLIST" + queue;
    else
      return "WORKLIST" + oldQueue;
  }

}
