package rreil.cfa.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javalx.data.CollectionHelpers;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.fn.Fn;
import javalx.fn.Predicate;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReil.Call;
import rreil.abstractsyntax.RReil.Return;
import rreil.abstractsyntax.RReil.Statement;
import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Block;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaBlock;
import rreil.cfa.Cfa.CfaCall;
import rreil.cfa.Cfa.CfaComputedFlow;
import rreil.cfa.Cfa.CfaReturn;
import rreil.cfa.Cfa.CfaTest;
import rreil.cfa.Cfa.TransitionType;
import rreil.cfa.util.GraphMLWriter;

/**
 * A collection of utility methods for a CFG.
 *
 * @author Bogdan Mihaila
 */
public class CfaHelpers {
  private static Map<Class<?>, Integer> transitionPriorities = new HashMap<Class<?>, Integer>();

  static {
    transitionPriorities.put(CfaCall.class, 3);
    transitionPriorities.put(CfaTest.class, 2);
    transitionPriorities.put(CfaComputedFlow.class, 1);
  }

  private CfaHelpers () { // no public instantiation
  }

  public static Set<Edge> getBlockEdges (final Cfa cfa) {
    Predicate<Edge> filter = new Predicate<Edge>() {
      @Override public Boolean apply (Edge edge) {
        return (cfa.getTransitionType(edge) instanceof CfaBlock);
      }
    };
    return CollectionHelpers.filter(cfa.edges(), filter);
  }

  public static Set<Edge> getTestEdges (final Cfa cfa) {
    Predicate<Edge> filter = new Predicate<Edge>() {
      @Override public Boolean apply (Edge edge) {
        return (cfa.getTransitionType(edge) instanceof CfaTest);
      }
    };
    return CollectionHelpers.filter(cfa.edges(), filter);
  }

  public static Set<Edge> getCallEdges (final Cfa cfa) {
    Predicate<Edge> filter = new Predicate<Edge>() {
      @Override public Boolean apply (Edge edge) {
        return (cfa.getTransitionType(edge) instanceof CfaCall);
      }
    };
    return CollectionHelpers.filter(cfa.edges(), filter);
  }

  public static Set<Edge> getComputedFlowEdges (final Cfa cfa) {
    Predicate<Edge> filter = new Predicate<Edge>() {
      @Override public Boolean apply (Edge edge) {
        return (cfa.getTransitionType(edge) instanceof CfaComputedFlow);
      }
    };
    return CollectionHelpers.filter(cfa.edges(), filter);
  }

  public static Set<Edge> getReturnEdges (final Cfa cfa) {
    Predicate<Edge> filter = new Predicate<Edge>() {
      @Override public Boolean apply (Edge edge) {
        return (cfa.getTransitionType(edge) instanceof CfaReturn);
      }
    };
    return CollectionHelpers.filter(cfa.edges(), filter);
  }

  /**
   * Returns the RREIL instructions in the CFG.
   *
   * @param cfa The CFG to be processed
   * @return All the RREIL instructions of the CFG
   */
  public static Set<RReil> getInstructions (Cfa cfa) {
    Set<RReil> instrs = new HashSet<RReil>();

    for (Edge edge : getBlockEdges(cfa)) {
      Block block = ((CfaBlock) cfa.getTransitionType(edge)).getBlock();
      for (Statement instr : block) {
        instrs.add(instr);
      }
    }

    for (Edge edge : getCallEdges(cfa)) {
      Call call = ((CfaCall) cfa.getTransitionType(edge)).getCallInstruction();
      instrs.add(call);
    }

    for (Edge edge : getReturnEdges(cfa)) {
      Return returnInstruction = ((CfaReturn) cfa.getTransitionType(edge)).getReturnInstruction();
      instrs.add(returnInstruction);
    }

    return instrs;
  }

  public static Set<Call> getCallInstructions (final Cfa cfa) {
    return CollectionHelpers.map(getCallEdges(cfa), new Fn<Edge, Call>() {
      @Override public Call apply (Edge e) {
        return ((CfaCall) cfa.getTransitionType(e)).getCallInstruction();
      }
    });
  }

  public static Set<Return> getReturnInstructions (final Cfa cfa) {
    return CollectionHelpers.map(getReturnEdges(cfa), new Fn<Edge, Return>() {
      @Override public Return apply (Edge e) {
        return ((CfaReturn) cfa.getTransitionType(e)).getReturnInstruction();
      }
    });
  }

  /**
   * Returns the RREIL instructions in the CFG and their addresses.
   *
   * @param cfa The CFG to be processed
   * @return A map from RREIL instructions to their addresses
   */
  public static Map<RReil, RReilAddr> getInstructionsAddresses (Cfa cfa) {
    Set<RReil> instructions = getInstructions(cfa);
    Map<RReil, RReilAddr> addresses = new HashMap<RReil, RReilAddr>(instructions.size());
    for (RReil instr : instructions) {
      addresses.put(instr, instr.getRReilAddress());
    }
    return addresses;
  }

  /**
   * Returns the addresses in the CFG and the
   * RREIL instructions at that addresses.
   *
   * @param cfa The CFG to be processed
   * @return A map from addresses to RREIL instructions
   */
  public static Map<RReilAddr, RReil> getAddressableInstructions (Cfa cfa) {
    Set<RReil> instructions = getInstructions(cfa);
    Map<RReilAddr, RReil> addresses = new HashMap<RReilAddr, RReil>(instructions.size());
    for (RReil instr : instructions) {
      addresses.put(instr.getRReilAddress(), instr);
    }
    return addresses;
  }

  /**
   * Returns the addresses for the instructions.
   * @param instructions A set of RREIL instructions
   * @return A set of addresses corresponding to the passed in instructions
   */
  public static <T extends RReil> Set<RReilAddr> getAddressesFor (Set<T> instructions) {
    return CollectionHelpers.map(instructions, new Fn<T, RReilAddr>() {
      @Override public RReilAddr apply (T insn) {
        return insn.getRReilAddress();
      }
    });
  }

  /**
   * Returns the base addresses for the instructions.
   * @param instructions A set of RREIL instructions
   * @return A set of base addresses corresponding to the passed in instructions
   */
  public static <T extends RReil> Set<Long> getBaseAddressesFor (Set<T> instructions) {
    return CollectionHelpers.map(instructions, new Fn<T, Long>() {
      @Override public Long apply (T insn) {
        return insn.getRReilAddress().base();
      }
    });
  }

  /**
   * Returns the addresses in the CFG and the vertices at that addresses.
   *
   * @param cfa The CFG to be processed
   * @return A map from addresses vertices
   */
  public static Map<RReilAddr, Vertex> getAddressableVertices (Cfa cfa) {
    Map<RReilAddr, Vertex> result = new HashMap<RReilAddr, Vertex>();
    Map<RReilAddr, TransitionType> addressSource = new HashMap<RReilAddr, TransitionType>();
    for (Edge edge : cfa.edges()) {
      TransitionType transition = cfa.getTransitionType(edge);
      RReilAddr address = transition.blockStartAddress();
      if (!addressSource.containsKey(address) || overrides(transition, addressSource.get(address))) {
        addressSource.put(address, transition);
        result.put(address, edge.getSource());
      }
    }
    return result;
  }

  /**
   * Implements the priorities for transition types to assure that the uppermost edge is chosen for "artificial" edges
   * (series of edges for one instruction) as are produced by calls or computed calls.
   * @return <code>true</code> if the first edge should be preferred to the second
   */
  private static boolean overrides (TransitionType first, TransitionType second) {
    // call edges are inserted as: call -> test(true) -> call return point
    // computed jump edges are inserted as: test -> computed jump -> assertion -> computed flow target
    // where redundant tests might be removed by cleanup actions
    return getTransitionPriority(first.getClass()) > getTransitionPriority(second.getClass());
  }

  private static int getTransitionPriority (Class<?> type) {
    Integer priority = transitionPriorities.get(type);
    if (priority == null)
      return 0;
    else
      return priority;
  }

  public static void renderCfa (Cfa cfa, String filePath) throws IOException {
    PrintWriter out = new PrintWriter(filePath);
    GraphMLWriter renderer = new GraphMLWriter(cfa);
    renderer.renderTo(out);
    out.close();
  }
}
