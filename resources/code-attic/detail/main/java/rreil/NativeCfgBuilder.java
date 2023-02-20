package rreil;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javalx.data.Option;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReil.Statement;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa.CfaBlock;
import binspot.asm.NativeDisassembler;
import binspot.asm.NativeInstruction;
import rreil.cfa.Cfa;
import rreil.cfa.util.CfaHelpers;

/**
 * @author Bogdan Mihaila
 */
public class NativeCfgBuilder {
  private final Map<Vertex, Vertex> nodesMap = new HashMap<Vertex, Vertex>();
  private final NativeCfg origCfg;
  private final NativeCfg newCfg;
  private Set<Vertex> visitedNodes;

  private NativeCfgBuilder (NativeCfg origCfg, NativeCfg newCfg) {
    this.origCfg = origCfg;
    this.newCfg = newCfg;
  }

  /**
   * Returns a CFG containing the native instructions. The CFG structure is built according to the structure of the
   * passed in CFA.
   *
   * @param cfa The CFA to be retrieve the control flow from
   * @param instructionsMapping A mapping between addresses and native instructions
   * @return The native CFG for the passed in CFA
   */
  public static NativeCfg getNativeCfg (Cfa cfa, Map<Long, NativeInstruction> instructionsMapping) {
    NativeCfg cfg = new NativeCfg();
    Map<RReilAddr, Vertex> vertices = new HashMap<RReilAddr, Vertex>();
    Map<RReilAddr, List<RReilAddr>> successors = getNativeSuccessors(getRReilSuccessors(cfa));
    RReilAddr entryAddress = cfa.getAddress(cfa.getEntry()).get();
    vertices.put(entryAddress.withOffset(0), cfg.getEntry());
    cfg.setBlockInstructions(cfg.getEntry(), Collections.singletonList(instructionsMapping.get(entryAddress.base())));
    for (RReilAddr address : successors.keySet()) {
      if (address.base() != entryAddress.base()) { // skip the entry node
        Vertex vertex = cfg.createVertex();
        vertices.put(address.withOffset(0), vertex);
        cfg.setBlockInstructions(vertex, Collections.singletonList(instructionsMapping.get(address.base())));
      }
    }
    for (Entry<RReilAddr, List<RReilAddr>> entry : successors.entrySet()) {
      Vertex vertex = vertices.get(entry.getKey().withOffset(0));
      for (RReilAddr successor : entry.getValue()) {
        cfg.createEdge(vertex, vertices.get(successor.withOffset(0)));
      }
    }
    return cfg;
  }

  /**
   * Returns the native successor instructions for each native instruction.
   */
  // TODO: see how intra-RREIL jumps are handled
  private static Map<RReilAddr, List<RReilAddr>> getNativeSuccessors (Map<RReilAddr, List<RReilAddr>> rreilSuccessors) {
    // as a native instruction corresponds to a whole block of RREIL instructions only the last RREIL instruction in such
    // a block would have a successor in a different block belonging to the next native instruction. Hence the map has to
    // be traversed backwards to ensure that we see the last instruction in an instruction block first.
    SortedMap<RReilAddr, List<RReilAddr>> reversedRReilSuccessors =
        new TreeMap<RReilAddr, List<RReilAddr>>(Collections.reverseOrder());
    reversedRReilSuccessors.putAll(rreilSuccessors);

    Map<RReilAddr, List<RReilAddr>> nativeSuccessors = new HashMap<RReilAddr, List<RReilAddr>>();
    long currentBaseAddress = Long.MIN_VALUE;
    for (Entry<RReilAddr, List<RReilAddr>> entry : reversedRReilSuccessors.entrySet()) {
      if (currentBaseAddress != entry.getKey().base() || currentBaseAddress == Long.MIN_VALUE) {
        nativeSuccessors.put(entry.getKey(), entry.getValue());
        currentBaseAddress = entry.getKey().base();
      }
    }
    return nativeSuccessors;
  }

  /**
   * Returns the successor instructions for each RREIL instruction. A branch can have more than one successor.
   */
  private static Map<RReilAddr, List<RReilAddr>> getRReilSuccessors (Cfa cfa) {
    SortedMap<RReilAddr, List<RReilAddr>> rreilSuccessors = new TreeMap<RReilAddr, List<RReilAddr>>();
    // find the successors for each node
    for (Vertex vertex : cfa.vertices()) {
      Option<RReilAddr> maybeAddress = cfa.getAddress(vertex);
      if (maybeAddress.isSome())
        rreilSuccessors.put(maybeAddress.get(), getRealSuccessorsFor(vertex, cfa));
    }
    // find the successors for each instruction in a basic block
    for (Edge edge : CfaHelpers.getBlockEdges(cfa)) {
      List<RReilAddr> blockSuccessors = rreilSuccessors.get(cfa.getAddress(edge.getSource()).get());
      List<Statement> instructions = ((CfaBlock) cfa.getTransitionType(edge)).getBlock();
      RReil predecessor = null;
      for (RReil insn : instructions) {
        if (predecessor != null)
          rreilSuccessors.put(predecessor.getRReilAddress(), Collections.singletonList(insn.getRReilAddress()));

        predecessor = insn;
      }
      rreilSuccessors.put(predecessor.getRReilAddress(), blockSuccessors);
    }
    return rreilSuccessors;
  }

  /**
   * Returns all the successor nodes that are not artificial CFA nodes as e.g. inserted for computed jumps.
   */
  private static List<RReilAddr> getRealSuccessorsFor (Vertex node, Cfa cfa) {
    RReilAddr initialAddress = cfa.getAddress(node).get();
    LinkedList<RReilAddr> successors = new LinkedList<RReilAddr>();
    Deque<Vertex> workQueue = new ArrayDeque<Vertex>();
    for (Vertex successor : node.successors()) {
      workQueue.addFirst(successor);
    }

    Vertex currentPoint;
    while (!workQueue.isEmpty()) {
      currentPoint = workQueue.removeFirst();
      Option<RReilAddr> blockStartAddress = cfa.getAddress(currentPoint);
      if (blockStartAddress.isSome() &&
          !isArtificialPath(node, initialAddress, currentPoint, blockStartAddress.get())) {
        successors.add(blockStartAddress.get());
      } else {
        for (Vertex successor : currentPoint.successors()) {
          workQueue.addFirst(successor);
        }
      }
    }
    return successors;
  }

  /**
   * Returns <code>true</code> if a path between two vertices is artificial---i.e. a path that contains vertices with the
   * same address but that are not the same vertices like in a self loop. These paths occur when tests or computed jumps
   * are modeled in the CFA.
   */
  private static boolean isArtificialPath (Vertex initialVertex, RReilAddr initialAddress, Vertex currentVertex,
      RReilAddr currentAddress) {
    return initialAddress.equals(currentAddress) && !initialVertex.equals(currentVertex);
  }

  /**
   * Return a new CFG that contains the same
   * instructions as the passed in one but where
   * paths of consecutive statements are merged
   * together to one basic block.
   */
  public static NativeCfg mkBasicBlocks (NativeCfg origCfg) {
    NativeCfgBuilder builder = new NativeCfgBuilder(origCfg, new NativeCfg());
    return builder.mkBasicBlocks();
  }

  private NativeCfg mkBasicBlocks () {
    nodesMap.put(origCfg.getEntry(), newCfg.getEntry());
    Queue<Vertex> workQueue = new ArrayDeque<Vertex>();
    workQueue.add(origCfg.getEntry());
    // add also all nodes that cannot be reached from the entry vertex by depth-first-search
    for (Vertex node : origCfg.vertices()) {
      if (node.incoming().isEmpty())
        workQueue.add(node);
    }

    visitedNodes = new HashSet<Vertex>();
    while (!workQueue.isEmpty()) {
      Vertex node = workQueue.remove();
      if (!visitedNodes.contains(node)) {
        visitedNodes.add(node);
        List<Vertex> blocksTrack = maxBlocksMunchFrom(node);
        Vertex beginNode = getCorrespondingNode(blocksTrack.get(0));
        newCfg.setBlockInstructions(beginNode, flatten(origCfg, blocksTrack));
        for (Vertex successor : blocksTrack.get(blocksTrack.size() - 1).successors()) {
          Vertex target = getCorrespondingNode(successor);
          newCfg.createEdge(beginNode, target);
          workQueue.add(successor);
        }
      }
    }
    return newCfg;
  }

  /**
   * Creates a list of vertices that can be reached as a non-branching path of vertices from the passed in node.
   */
  private List<Vertex> maxBlocksMunchFrom (Vertex node) {
    List<Vertex> path = new LinkedList<Vertex>();
    if (node.outgoing().isEmpty()) {
      path.add(node);
      return path;
    }
    Vertex target = node;
    do {
      path.add(target);
      visitedNodes.add(target);
      target = target.successors().iterator().next();
      if (target.outgoing().size() != 1) {
        path.add(target);
        visitedNodes.add(target);
        break;
      }
    } while (target.incoming().size() == 1 && !target.equals(origCfg.getEntry()));
    return path;
  }

  /**
   * Compacts a series of vertices containing instructions to a flat list of instructions.
   */
  private static List<NativeInstruction> flatten (NativeCfg cfg, List<Vertex> blocksTrack) {
    List<NativeInstruction> result = new LinkedList<NativeInstruction>();
    for (Vertex node : blocksTrack) {
      result.addAll(cfg.getBlockInstructions(node));
    }
    return result;
  }

  /**
   * Returns a node in the new CFG that corresponds
   * to the one passed in from the old CFG.
   */
  private Vertex getCorrespondingNode (Vertex origNode) {
    Vertex otherNode = nodesMap.get(origNode);
    if (otherNode == null) {
      otherNode = newCfg.createVertex();
      nodesMap.put(origNode, otherNode);
    }
    return otherNode;
  }

  public static class InstructionsMapper implements Map<Long, NativeInstruction> {
    private final NativeDisassembler dis;
    private final byte[] data;
    private final long baseAddress;

    public InstructionsMapper (NativeDisassembler dis, byte[] data, long baseAddress) {
      this.dis = dis;
      this.data = data;
      this.baseAddress = baseAddress;
    }

    @Override public NativeInstruction get (Object key) {
      if (key instanceof Long) {
        Long address = (Long) key;
        return dis.decodeOne(data, (int) (address - baseAddress), address);
      }

      return null;
    }

    @Override public int size () {
      return 0;
    }

    @Override public boolean isEmpty () {
      return false;
    }

    @Override public boolean containsKey (Object key) {
      return false;
    }

    @Override public boolean containsValue (Object value) {
      return false;
    }

    @Override public NativeInstruction put (Long key, NativeInstruction value) {
      throw new UnsupportedOperationException();
    }

    @Override public NativeInstruction remove (Object key) {
      throw new UnsupportedOperationException();
    }

    @Override public void putAll (Map<? extends Long, ? extends NativeInstruction> m) {
      throw new UnsupportedOperationException();
    }

    @Override public void clear () {
      throw new UnsupportedOperationException();
    }

    @Override public Set<Long> keySet () {
      throw new UnsupportedOperationException();
    }

    @Override public Collection<NativeInstruction> values () {
      throw new UnsupportedOperationException();
    }

    @Override public Set<java.util.Map.Entry<Long, NativeInstruction>> entrySet () {
      throw new UnsupportedOperationException();
    }
  }
}
