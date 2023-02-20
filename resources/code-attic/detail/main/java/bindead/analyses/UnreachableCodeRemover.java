package bindead.analyses;

import bindead.analysis.util.CallString;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.exceptions.Unreachable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javalx.data.Option;
import javalx.digraph.Digraph.Edge;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaTest;
import rreil.cfa.Cfa.TransitionType;

/**
 *
 */
public class UnreachableCodeRemover<D extends RootDomain<D>> {
  private final ReconstructionCallString<D> callString;
  private final AnalysisCtx<D> contexts;

  public UnreachableCodeRemover (ReconstructionCallString<D> callString) {
    this.callString = callString;
    this.contexts = callString.getAnalysisContexts();
  }

  public void run () {
    System.out.println(mergeOverAllContexts(collectUnsatisfiableTests()));
  }

  private Map<Cfa, List<Set<Edge>>> collectUnsatisfiableTests () {
    Map<Cfa, List<Set<Edge>>> unreachableCode = new HashMap<Cfa, List<Set<Edge>>>();
    for (Entry<CallString, Analysis<D>> ctx : contexts.entrySet()) {
      Analysis<D> analysis = ctx.getValue();
      Cfa cfa = analysis.getCfa();
      unreachableCode.put(cfa, new ArrayList<Set<Edge>>());
      Set<Edge> unsatisfiableTestEdges = new HashSet<Edge>();
      for (Edge edge : cfa.edges()) {
        TransitionType ty = cfa.getTransitionType(edge);
        if (ty instanceof CfaTest) {
          Option<D> state = analysis.getState(edge.getSource());
          if (state.isNone())
            continue;
          try {
            ty.accept(analysis.getEvaluator(), edge, state.get());
          } catch (Unreachable e) {
            state.get().unrollUnreachable();
            unsatisfiableTestEdges.addAll(collectUnreachableCode(analysis, edge));
          }
        }
      }
      unreachableCode.get(cfa).add(unsatisfiableTestEdges);
    }
    return unreachableCode;
  }

  private Map<Cfa, Set<Edge>> mergeOverAllContexts (Map<Cfa, List<Set<Edge>>> unreachableCode) {
    Map<Cfa, Set<Edge>> ctxInsensitiveUnreachableCode = new HashMap<Cfa, Set<Edge>>();
    for (Entry<Cfa, List<Set<Edge>>> entry : unreachableCode.entrySet()) {
      if (entry.getValue().isEmpty()) {
        ctxInsensitiveUnreachableCode.put(entry.getKey(), new HashSet<Edge>());
        continue;
      }
      Iterator<Set<Edge>> it = entry.getValue().iterator();
      Set<Edge> merged = it.next();
      while (it.hasNext()) {
        Set<Edge> edges = it.next();
        for (Edge e : edges) {
          if (!merged.contains(e))
            merged.remove(e);
        }
      }
      ctxInsensitiveUnreachableCode.put(entry.getKey(), merged);
    }
    return ctxInsensitiveUnreachableCode;
  }

  // Runs a depth first traversal starting at {@code test} collecting all unreachable transition edges.
  private Set<Edge> collectUnreachableCode (Analysis<D> analysis, Edge test) {
    Set<Edge> unreachableCode = new HashSet<Edge>();
    ArrayDeque<Edge> worklist = new ArrayDeque<Edge>();
    unreachableCode.add(test);
    worklist.addAll(test.getTarget().outgoing());
    while (!worklist.isEmpty()) {
      Edge e = worklist.pop();
      if (analysis.getState(e.getSource()).isNone()) {
        unreachableCode.add(e);
        worklist.addAll(e.getTarget().outgoing());
      }
    }
    return unreachableCode;
  }
}
