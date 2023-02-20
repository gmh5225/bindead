package rreil.cfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javalx.data.BigInt;
import javalx.data.CollectionHelpers;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.fn.Fn;
import javalx.fn.Predicate;
import rreil.abstractsyntax.RReil.Nop;
import rreil.abstractsyntax.RReil.Statement;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.Test;
import rreil.abstractsyntax.util.CRReilFactory;
import rreil.abstractsyntax.util.RReilFactory;
import rreil.cfa.Cfa.CfaBlock;
import rreil.cfa.Cfa.CfaTest;
import rreil.cfa.Cfa.TransitionType;

/**
 * Provides methods to copy transform CFAs.
 *
 * @author Bogdan Mihaila
 */
class CfaCleaner {
  private final Cfa origCfg;
  private final Cfa newCfg;
  private final Map<Edge, Edge> edgesMapping = new HashMap<Edge, Edge>();
  private final Map<Vertex, Vertex> nodesMapping = new HashMap<Vertex, Vertex>();

  private CfaCleaner (Cfa origCfg, Cfa newCfg) {
    this.origCfg = origCfg;
    this.newCfg = newCfg;
    nodesMapping.put(origCfg.getEntry(), newCfg.getEntry());
    nodesMapping.put(origCfg.getExit(), newCfg.getExit());
  }

  /**
   * Removes nops and redundant jumps in the CFA.
   *
   * @param cfa The CFA to be worked on
   * @return A new CFA in which the nops and redundant jumps were removed
   */
  static Cfa cleanup (Cfa cfa) {
    Cfa tmpCfa = removeNops(cfa);
    return removeRedundantJumps(tmpCfa);
  }

  /**
   * Replaces the instructions in the CFA at the passed in addresses with no-op instructions.
   *
   * @param cfa The CFA to be worked on
   * @param addresses The instruction addresses of the dead code
   * @return A new CFA in which the dead code has been removed
   */
  static Cfa removeDeadCode (Cfa cfa, Set<RReilAddr> addresses) {
    CfaCleaner remover = new CfaCleaner(cfa, new Cfa(cfa.getName(), cfa.getEntryAddress()));
    return remover.removeDeadCodeImpl(addresses);
  }

  /**
   * Removes nops from the CFA.
   *
   * @param cfa The CFA to be worked on
   * @return A new CFA in which nop instructions have been removed
   */
  static Cfa removeNops (Cfa cfa) {
    CfaCleaner remover = new CfaCleaner(cfa, new Cfa(cfa.getName(), cfa.getEntryAddress()));
    return remover.removeNopsImpl();
  }

  /**
   * Removes any redundant jump edges in the CFA.
   * Redundant edges are jumps that jump unconditionally
   * to the next instruction. Thus performing a no-op.
   *
   * @param cfa The CFA to be worked on
   * @return A new CFA in which the redundant jump edges have been removed
   */
  static Cfa removeRedundantJumps (Cfa cfa) {
    CfaCleaner remover = new CfaCleaner(cfa, new Cfa(cfa.getName(), cfa.getEntryAddress()));
    return remover.cleanupImpl(getRemovableVertices(getRedundantJumpEdges(cfa)));
  }

  /**
   * Actual implementation of the cleanup operation.
   */
  private Cfa cleanupImpl (Set<Vertex> removeableVertices) {
    Queue<Vertex> workQueue = new ArrayDeque<Vertex>(); // FIFO queue
    workQueue.add(origCfg.getEntry());

    // add also all nodes that cannot be reached from the entry vertex by depth-first-search
    for (Vertex node : origCfg.vertices()) {
      if (node.incoming().isEmpty())
        workQueue.add(node);
    }

    Set<Vertex> visitedNodes = new HashSet<Vertex>();
    while (!workQueue.isEmpty()) {
      Vertex vertex = workQueue.remove();
      if (visitedNodes.contains(vertex))
        continue;
      else
        visitedNodes.add(vertex);

      for (Edge origEdge : vertex.outgoing()) {
        Vertex origTarget = origEdge.getTarget();
        if (removeableVertices.contains(origTarget)) { // remove all the vertices and edges till the next valid vertex
          List<Vertex> deadNodesTrack = getMaxVertexMunchFrom(origTarget, removeableVertices);
          Vertex origFirstAliveNode = deadNodesTrack.get(deadNodesTrack.size() - 1);
          Vertex copyFirstAliveNode = copyNode(origFirstAliveNode);

          // edges pointing somewhere in the middle of the removed track will now have their target point to the end
          for (Vertex node : deadNodesTrack) {
            nodesMapping.put(node, copyFirstAliveNode);
          }

          Vertex source = copyNode(origEdge.getSource());
          Edge copyEdge = newCfg.createEdge(source, copyFirstAliveNode);
          newCfg.setTransitionType(copyEdge, origCfg.getTransitionType(origEdge));
          workQueue.add(origFirstAliveNode);
        } else { // just copy the edge
          Vertex source = copyNode(origEdge.getSource());
          Vertex target = copyNode(origEdge.getTarget());
          Edge copyEdge = newCfg.createEdge(source, target);
          newCfg.setTransitionType(copyEdge, origCfg.getTransitionType(origEdge));
          workQueue.add(origTarget);
        }
      }
    }
    return newCfg;
  }

  /**
   * Actual implementation of the remove dead code operation.
   */
  private Cfa removeDeadCodeImpl (Set<RReilAddr> addresses) {
    for (Edge origEdge : origCfg.edges()) {
      if (origCfg.getTransitionType(origEdge) instanceof CfaBlock)
        copyEdgeAndInsertNops(origEdge, addresses);
      else
        copyEdge(origEdge);
    }
    return newCfg;
  }

  /**
   * Actual implementation of the remove nops operation.
   */
  private Cfa removeNopsImpl () {
    for (Edge origEdge : origCfg.edges()) {
      if (origCfg.getTransitionType(origEdge) instanceof CfaBlock)
        copyEdgeAndRemoveNops(origEdge);
      else
        copyEdge(origEdge);
    }
    return newCfg;
  }

  /**
   * Returns a node in the new CFA that corresponds
   * to the one passed in from the old CFA. Lazily builds
   * new nodes if there isn't yet a corresponding one.
   */
  private Vertex copyNode (Vertex origNode) {
    Vertex otherNode = nodesMapping.get(origNode);
    if (otherNode == null) {
      otherNode = newCfg.createVertex();
      nodesMapping.put(origNode, otherNode);
    }
    return otherNode;
  }

  /**
   * Returns an edge in the new CFA that corresponds
   * to the one passed in from the old CFA. Lazily builds
   * new edges if there isn't yet a corresponding one.
   */
  private Edge copyEdge (Edge origEdge) {
    Edge otherEdge = edgesMapping.get(origEdge);
    if (otherEdge == null) {
      Vertex source = copyNode(origEdge.getSource());
      Vertex target = copyNode(origEdge.getTarget());
      otherEdge = newCfg.createEdge(source, target);
      newCfg.setTransitionType(otherEdge, origCfg.getTransitionType(origEdge));
      edgesMapping.put(origEdge, otherEdge);
    }
    return otherEdge;
  }

  /**
   * Copies an edge analogous to {@link #copyEdge(Edge)} but inserts nop instructions for the given addresses.
   *
   * @param blockEdge The edge to be copied
   * @param deadCodeAddresses A set of dead code addresses
   */
  private void copyEdgeAndInsertNops (Edge blockEdge, Set<RReilAddr> deadCodeAddresses) {
    RReilFactory rreilFactory = CRReilFactory.getInstance();
    List<Statement> instrs = ((CfaBlock) origCfg.getTransitionType(blockEdge)).getBlock();
    List<Statement> copiedInstrs = new ArrayList<Statement>();
    boolean changed = false;
    for (Statement stmt : instrs) {
      RReilAddr address = stmt.getRReilAddress();
      if (deadCodeAddresses.contains(address)) {
        copiedInstrs.add(rreilFactory.nop(address));
        changed = true;
      } else {
        copiedInstrs.add(stmt);
      }
    }
    if (changed) {
      Edge copiedEdge = copyEdge(blockEdge);
      newCfg.setTransitionType(copiedEdge, TransitionType.block(Block.block(copiedInstrs)));
    } else {
      // if no instructions were removed then keep the original annotation for memory sharing purpose
      copyEdge(blockEdge);
    }
  }

  /**
   * Copies an edge analogous to {@link #copyEdge(Edge)} but removes nop instructions from basic blocks and transforms
   * empty basic blocks to jumps.
   *
   * @param blockEdge The edge to be copied
   */
  private void copyEdgeAndRemoveNops (Edge blockEdge) {
    List<Statement> instrs = ((CfaBlock) origCfg.getTransitionType(blockEdge)).getBlock();
    List<Statement> copiedInstrs = new ArrayList<Statement>();
    for (Statement stmt : instrs) {
      if (!(stmt instanceof Nop))
        copiedInstrs.add(stmt);
    }
    if (copiedInstrs.isEmpty()) { // the whole instructions block was dead
      RReilFactory rreilFactory = CRReilFactory.getInstance();
      Vertex source = copyNode(blockEdge.getSource());
      Vertex target = copyNode(blockEdge.getTarget());
      Edge copiedEdge = newCfg.createEdge(source, target);
      Test alwaysTrueCond = rreilFactory.testIfNonZero(rreilFactory.literal(1, BigInt.ONE));
      RReilAddr startAdress = ((CfaBlock) origCfg.getTransitionType(blockEdge)).blockStartAddress();
      newCfg.setTransitionType(copiedEdge, TransitionType.test(startAdress, alwaysTrueCond));
      edgesMapping.put(blockEdge, copiedEdge);
    } else if (instrs.size() != copiedInstrs.size()) {
      Edge copiedEdge = copyEdge(blockEdge);
      newCfg.setTransitionType(copiedEdge, TransitionType.block(Block.block(copiedInstrs)));
    } else {
      // if no instructions were removed then keep the original annotation for memory sharing purpose
      copyEdge(blockEdge);
    }
  }

  /**
   * Returns a list of vertices starting with the passed in one,
   * that are all removable and follow each other as a single path
   * in the CFA. The last vertex in the list is the one that ends
   * the path and is thus not removable.
   *
   * @param start A removable vertex to start the munch with
   * @param removeableVertices A set of all the vertices in the CFA that are removable
   * @return A list of the longest path or removable vertices ending with the first non-removable vertex
   */
  private static List<Vertex> getMaxVertexMunchFrom (Vertex start, Set<Vertex> removeableVertices) {
    List<Vertex> result = new ArrayList<Vertex>();
    result.add(start);
    Vertex nextNode = start;

    do {
      nextNode = nextNode.successors().iterator().next(); // a removable node has exactly one successor
      result.add(nextNode);
    } while (removeableVertices.contains(nextNode));

    return result;
  }

  /**
   * Returns jump edges in the CFA that are jumping unconditionally to the next instruction.
   */
  private static Set<Edge> getRedundantJumpEdges (final Cfa cfa) {
    Predicate<Edge> filter = new Predicate<Edge>() {
      @Override public Boolean apply (Edge edge) {
        return ((cfa.getTransitionType(edge) instanceof CfaTest) && edge.getSource().outgoing().size() == 1);
      }
    };
    return CollectionHelpers.filter(cfa.edges(), filter);
  }

  /**
   * Returns the vertices in the CFA that can be removed because they only belong to a redundant jump edge.
   */
  private static Set<Vertex> getRemovableVertices (Set<Edge> removableEdges) {
    return CollectionHelpers.map(removableEdges, new Fn<Edge, Vertex>() {
      @Override public Vertex apply (Edge edge) {
        return edge.getSource();
      }
    });
  }
}
