package rreil.cfa;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReil.Statement;
import rreil.cfa.Cfa.CfaBlock;
import rreil.cfa.Cfa.TransitionType;

/**
 * Transforms a CFG to a CFG where paths of consecutive statements
 * are merged together to form basic blocks. It also provides the
 * reversal operation on CFAs.
 *
 * @author Bogdan Mihaila
 */
class BasicBlocksTool {
  private final Cfa origCfa;
  private final Cfa newCfa;
  private final Set<Vertex> visitedNodes;
  private final Map<Vertex, Vertex> nodesMap;

  private BasicBlocksTool (Cfa origCfg, Cfa newCfg) {
    this.origCfa = origCfg;
    this.newCfa = newCfg;
    visitedNodes = new HashSet<Vertex>();
    nodesMap = new HashMap<Vertex, Vertex>();
  }

  /**
   * Return a new CFG that contains the same
   * instructions as the passed in one but where
   * paths of consecutive statements are merged
   * together to one basic block.
   */
  static Cfa makeBasicBlocks (Cfa cfa) {
    BasicBlocksTool builder = new BasicBlocksTool(cfa, new Cfa(cfa.getName(), cfa.getEntryAddress()));
    return builder.mkBasicBlocks();
  }

  /**
   * Returns a new CFG that contains the same
   * instructions as the passed in one but where
   * the basic blocks are split up to contain
   * only one single instruction per block.
   */
  static Cfa expandBasicBlocks (Cfa cfa) {
    boolean hasBasicBlocks = false;
    for (Vertex node : cfa.vertices()) {
      for (Edge edge : node) {
        TransitionType edgeType = cfa.getTransitionType(edge);
        if (edgeType instanceof CfaBlock) {
          List<Statement> instrs = ((CfaBlock) edgeType).getBlock();
          if (instrs.size() > 1) {
            hasBasicBlocks = true;
            break;
          }
        }
      }
    }

    if (hasBasicBlocks) {
      BasicBlocksTool builder = new BasicBlocksTool(cfa, new Cfa(cfa.getName(), cfa.getEntryAddress()));
      return builder.expandBasicBlocks();
    } else
      return cfa;
  }

  private Cfa mkBasicBlocks () {
    nodesMap.put(origCfa.getEntry(), newCfa.getEntry());
    nodesMap.put(origCfa.getExit(), newCfa.getExit());
    Queue<Vertex> workQueue = new ArrayDeque<Vertex>();
    workQueue.add(origCfa.getEntry());
    // add also all nodes that cannot be reached from the entry vertex by depth-first-search
    for (Vertex node : origCfa.vertices()) {
      if (node.incoming().isEmpty())
        workQueue.add(node);
    }

    while (!workQueue.isEmpty()) {
      Vertex node = workQueue.remove();
      if (visitedNodes.contains(node))
        continue;
      else
        visitedNodes.add(node);

      for (Edge origEdge : node.outgoing()) {
        if (origCfa.getTransitionType(origEdge) instanceof CfaBlock) { // try to compact a series of statements
          List<Edge> blocksTrack = maxBlocksMunchFrom(origEdge);
          Vertex beginNode = origEdge.getSource();
          Vertex endNode = blocksTrack.get(blocksTrack.size() - 1).getTarget();
          Vertex parent = getCorrespondingNode(beginNode);
          Vertex child = getCorrespondingNode(endNode);
          Edge basicBlockEdge = newCfa.createEdge(parent, child);
          CfaBlock block = combine(convert(blocksTrack));
          newCfa.setTransitionType(basicBlockEdge, block);
          workQueue.add(endNode);
        } else { // just copy the edge and node
          copyEdge(origEdge);
          workQueue.add(origEdge.getTarget());
        }
      }
    }
    return newCfa;
  }

  private Cfa expandBasicBlocks () {
    nodesMap.put(origCfa.getEntry(), newCfa.getEntry());
    nodesMap.put(origCfa.getExit(), newCfa.getExit());
    for (Vertex node : origCfa.vertices()) {
      if (visitedNodes.contains(node))
        continue;
      else
        visitedNodes.add(node);

      for (Edge origEdge : node.outgoing()) {
        TransitionType edgeType = origCfa.getTransitionType(origEdge);
        if (edgeType instanceof CfaBlock) {
          List<Statement> instrs = ((CfaBlock) edgeType).getBlock();
          if (instrs.size() == 1) { // just copy the edge and node
            copyEdge(origEdge);
          } else { // expand block to a series of statements
            Vertex parent = getCorrespondingNode(node);
            Vertex child = null;
            for (Iterator<Statement> iterator = instrs.iterator(); iterator.hasNext();) {
              Statement stmt = iterator.next();
              if (iterator.hasNext())
                child = newCfa.createVertex();
              else
                child = getCorrespondingNode(origEdge.getTarget());
              Edge newEdge = newCfa.createEdge(parent, child);
              newCfa.setTransitionType(newEdge, Cfa.TransitionType.block(stmt));
              parent = child;
            }
          }
        } else { // just copy the edge and node
          copyEdge(origEdge);
        }
      }
    }

    return newCfa;
  }

  /**
   * Returns a node in the new CFG that corresponds
   * to the one passed in from the old CFG.
   */
  private Vertex getCorrespondingNode (Vertex origNode) {
    Vertex otherNode = nodesMap.get(origNode);
    if (otherNode == null) {
      otherNode = newCfa.createVertex();
      nodesMap.put(origNode, otherNode);
    }
    return otherNode;
  }

  /**
   * Copies an edge and the source/target nodes of it
   * from the original CFG to the clone CFG.
   */
  private void copyEdge (Edge origEdge) {
    Vertex beginNode = origEdge.getSource();
    Vertex endNode = origEdge.getTarget();
    Vertex parent = getCorrespondingNode(beginNode);
    Vertex child = getCorrespondingNode(endNode);
    Edge copiedEdge = newCfa.createEdge(parent, child);
    newCfa.setTransitionType(copiedEdge, origCfa.getTransitionType(origEdge));
  }

  /**
   * Returns a list of edges starting with the passed in one
   * that are all statements and follow each other in the CFG as a
   * single path that is not interrupted or crossed by another path.
   */
  private List<Edge> maxBlocksMunchFrom (Edge startEdge) {
    LinkedList<Edge> edges = new LinkedList<Edge>();
    edges.add(startEdge);
    Vertex target = startEdge.getTarget();
    Edge newEdge;
    while (target.outgoing().size() == 1 && target.incoming().size() == 1 && !target.equals(origCfa.getEntry())) {
      newEdge = target.outgoing().iterator().next();
      if (origCfa.getTransitionType(newEdge) instanceof CfaBlock) {
        edges.addLast(newEdge);
        visitedNodes.add(newEdge.getSource());
        target = newEdge.getTarget();
      } else
        break;
    }
    return edges;
  }

  /**
   * Convert a list of edges to a list of blocks.
   */
  private List<CfaBlock> convert (List<Edge> edges) {
    LinkedList<CfaBlock> blocks = new LinkedList<CfaBlock>();
    for (Edge edge : edges) {
      blocks.addLast((CfaBlock) origCfa.getTransitionType(edge));
    }
    return blocks;
  }

  /**
   * Combines a list of blocks to a single aggregated block.
   */
  private static CfaBlock combine (List<CfaBlock> cfgBlocks) {
    LinkedList<RReil.Statement> instructions = new LinkedList<RReil.Statement>();
    for (CfaBlock block : cfgBlocks) {
      for (RReil.Statement stmt : block.getBlock()) {
        instructions.addLast(stmt);
      }
    }
    return TransitionType.block(Block.block(instructions));
  }
}
