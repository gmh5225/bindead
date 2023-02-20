package bindead.domains.segments.heap;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javalx.data.products.P2;
import javalx.persistentcollections.AVLMap;
import rreil.lang.MemVar;
import bindead.data.MemVarSet;

public class HeapPartitioning implements Iterable<MemVarSet> {
  final List<MemVarSet> plist;
  final AVLMap<MemVar, MemVarSet> heapToRegs;

  public HeapPartitioning (AVLMap<MemVar, MemVarSet> heapToRegs) {
    this.heapToRegs = heapToRegs;
    plist = new LinkedList<MemVarSet>();
    MemVarSet done = MemVarSet.empty();
    for (P2<MemVar, MemVarSet> x : heapToRegs) {
      if (done.contains(x._1()))
        continue;
      MemVarSet current = MemVarSet.of(x._1());
      heapToRegs = heapToRegs.remove(x._1());
      for (P2<MemVar, MemVarSet> y : heapToRegs) {
        if (!y._2().containsTheSameAs(x._2()))
          continue;
        current = current.insert(y._1());
        done = done.insert(y._1());
        heapToRegs = heapToRegs.remove(y._1());
      }
      plist.add(current);
    }
  }

  @Override public String toString () {
    return "[" + heapToRegs + " ==> " + plist + "]";
  }

  @Override public Iterator<MemVarSet> iterator () {
    return plist.iterator();
  }
}
