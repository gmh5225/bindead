package rreil.cfa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javalx.data.Option;
import javalx.digraph.Digraph;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReil.Return;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.Rhs;
import rreil.abstractsyntax.Test;
import rreil.cfa.util.CfaStateException;
import rreil.cfa.util.CfaStateException.ErrObj;
import rreil.cfa.util.GmlWriter;
import rreil.disassembly.DisassemblyProvider;
import rreil.disassembly.RReilDisassemblyCtx;

/**
 * Definition of control-flow automatons.
 */
public final class Cfa extends Digraph implements GmlWriter.VertexLabelRenderer, GmlWriter.EdgeLabelRenderer {
  public static final String $UnknownIdentifierPrefix = "unknown";
  public static final String $EntryVertexLabel = "E";
  public static final String $ExitVertexLabel = "X";
  private final Map<Edge, TransitionType> blocks = new HashMap<Edge, TransitionType>();
  private final Vertex entryVertex = createVertex();
  private final Vertex exitVertex = createVertex();
  private final String name;
  private final RReilAddr entryAddress;
  private final List<CfaExtensionListener> listeners = new ArrayList<CfaExtensionListener>();

  public Cfa (RReilAddr entryAddress) {
    this("<" + $UnknownIdentifierPrefix + "@" + entryAddress + ">", entryAddress);
  }

  public Cfa (String name, RReilAddr entryAddress) {
    this.name = name;
    this.entryAddress = entryAddress;
  }

  /**
   * @return This CFA's unique entry point.
   */
  public Vertex getEntry () {
    return entryVertex;
  }

  /**
   * @return This CFA's unique exit point.
   */
  public Vertex getExit () {
    return exitVertex;
  }

  /**
   * @return The symbolic name of {@code this} CFA.
   */
  public String getName () {
    return name;
  }

  /**
   * @return The entry address of {@code this} CFA
   */
  public RReilAddr getEntryAddress () {
    return entryAddress;
  }

  public Option<RReilAddr> getAddress (Vertex v) {
    if (getExit().equals(v)) {
      return Option.<RReilAddr>none();
    } else {
      RReilAddr address = null;
      for (Edge edge : v.outgoing()) {
        if (address == null)
          address = getTransitionType(edge).blockStartAddress();
        else if (address != getTransitionType(edge).blockStartAddress())
          throw new CfaStateException(ErrObj.INVARIANT_FAILURE, "The outgoing edges for vertex " + v +
              " do not have the same address.");
      }
      return Option.fromNullable(address);
    }
  }

  public TransitionType getTransitionType (Edge edge) {
    return blocks.get(edge);
  }

  public void setTransitionType (Edge edge, TransitionType type) {
    blocks.put(edge, type);
  }

  @Override public String labelOfEdge (Edge e) {
    TransitionType ty = blocks.get(e);
    if (ty == null)
      return "{}";
    return ty.toString();
  }

  @Override public String labelOfVertex (Vertex v) {
    StringBuilder builder = new StringBuilder();
    Option<RReilAddr> address = getAddress(v);
    if (v == entryVertex)
      builder.append($EntryVertexLabel + "@");
    else if (v == exitVertex)
      builder.append($ExitVertexLabel);

    if (address.isSome()) {
      builder.append(address.get().toString());
    } else {
      for (Edge e : v.outgoing()) {
        builder.append(getTransitionType(e).toString());
        break;
      }
    }
    return builder.toString();
  }

  @Override protected String asString (Vertex v) {
    return labelOfVertex(v);
  }

  @Override protected String asString (Edge e) {
    return labelOfEdge(e);
  }

  public void extendFrom (Edge computedFlow, RReilAddr targetAddress, DisassemblyProvider dis) {
    if (dis == null)
      throw new IllegalStateException("No disassembly provider.");
    RReilDisassemblyCtx decodeFrom;
    try {
      decodeFrom = dis.decodeFrom(targetAddress);
    } catch (IndexOutOfBoundsException cause) {
      throw new CfaExtensionException(cause, this, targetAddress);
    }
    boolean extended = CfaBuilder.extend(this, decodeFrom, computedFlow, targetAddress);
    if (extended)
      fireCfaExtendedEvent();
  }

  public void addExtensionListener (CfaExtensionListener listener) {
    listeners.add(listener);
  }

  private void fireCfaExtendedEvent () {
    for (CfaExtensionListener listener : listeners) {
      listener.cfaExtended(this);
    }
  }

  /**
   * Cleans this CFA by removing nops and redundant jumps.
   * @return A new CFA that is equal to this but has been trimmed.
   */
  public Cfa trim () {
    return CfaBuilder.cleanup(this);
  }

  /**
   * @return A new CFA that is equal to this but transformed to group consecutive instructions as basic blocks.
   */
  public Cfa withBasicBlocks () {
    return CfaBuilder.makeBasicBlocks(this);
  }

  /**
   * @return A new CFA that is equal to this but transformed to not have consecutive instructions grouped as basic blocks.
   */
  public Cfa withoutBasicBlocks () {
    return CfaBuilder.expandBasicBlocks(this);
  }

  /**
   * Interface for listeners that are interested to get notified on CFA modifications.
   *
   * @author Bogdan Mihaila
   */
  public interface CfaExtensionListener {
    public void cfaExtended (Cfa cfa);
  }

  public static abstract class TransitionType {
    public abstract <R, T> R accept (CfaEdgeTypeVisitor<R, T> visitor, Edge correspondingEdge, T state);

    public abstract RReilAddr blockStartAddress ();

    @Override public abstract String toString ();

    public static CfaCall call (RReil.Call insn) {
      return new CfaCall(insn);
    }

    public static CfaReturn ret (RReil.Return insn) {
      return new CfaReturn(insn);
    }

    public static CfaBlock block (Block block) {
      return new CfaBlock(block);
    }

    public static CfaBlock block (RReil.Statement insn) {
      return new CfaBlock(Block.singleton(insn));
    }

    public static CfaTest test (RReilAddr address, Test test) {
      return new CfaTest(address, test);
    }

    public static CfaComputedFlow flow (RReilAddr address, Rhs.Rvar variable) {
      return new CfaComputedFlow(address, variable);
    }
  }

  public static final class CfaBlock extends TransitionType {
    private final Block block;

    private CfaBlock (Block block) {
      this.block = block;
    }

    @Override public String toString () {
      return block.toString();
    }

    @Override public <R, T> R accept (CfaEdgeTypeVisitor<R, T> visitor, Edge correspondingEdge, T state) {
      return visitor.visit(this, correspondingEdge, state);
    }

    public Block getBlock () {
      return block;
    }

    @Override public RReilAddr blockStartAddress () {
      return block.get(0).getRReilAddress();
    }
  }

  public static final class CfaCall extends TransitionType {
    private final RReil.Call callInstruction;

    private CfaCall (RReil.Call callInstruction) {
      this.callInstruction = callInstruction;
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(callInstruction.getRReilAddress());
      builder.append(": ");
      builder.append(callInstruction);
      return builder.toString();
    }

    @Override public <R, T> R accept (CfaEdgeTypeVisitor<R, T> visitor, Edge correspondingEdge, T state) {
      return visitor.visit(this, correspondingEdge, state);
    }

    public RReil.Call getCallInstruction () {
      return callInstruction;
    }

    @Override public RReilAddr blockStartAddress () {
      return callInstruction.getRReilAddress();
    }
  }

  public static final class CfaReturn extends TransitionType {
    private final RReil.Return returnInstruction;

    public CfaReturn (Return returnInstruction) {
      this.returnInstruction = returnInstruction;
    }

    public Return getReturnInstruction () {
      return returnInstruction;
    }

    @Override public <R, T> R accept (CfaEdgeTypeVisitor<R, T> visitor, Edge correspondingEdge, T state) {
      return visitor.visit(this, correspondingEdge, state);
    }

    @Override public RReilAddr blockStartAddress () {
      return returnInstruction.getRReilAddress();
    }

    @Override public String toString () {
      StringBuilder builder = new StringBuilder();
      builder.append(returnInstruction.getRReilAddress());
      builder.append(": ");
      builder.append(returnInstruction);
      return builder.toString();
    }
  }

  public static final class CfaTest extends TransitionType {
    private final Test test;
    private final RReilAddr address;

    private CfaTest (RReilAddr address, Test test) {
      this.address = address;
      this.test = test;
    }

    @Override public String toString () {
      return test.toString();
    }

    @Override public <R, T> R accept (CfaEdgeTypeVisitor<R, T> visitor, Edge correspondingEdge, T state) {
      return visitor.visit(this, correspondingEdge, state);
    }

    public Test getTest () {
      return test;
    }

    @Override public RReilAddr blockStartAddress () {
      return address;
    }
  }

  public static final class CfaComputedFlow extends TransitionType {
    private final Rhs.Rvar variable;
    private final RReilAddr address;

    private CfaComputedFlow (RReilAddr address, Rhs.Rvar variable) {
      this.address = address;
      this.variable = variable;
    }

    @Override public String toString () {
      return "jmp " + variable.toString();
    }

    @Override public <R, T> R accept (CfaEdgeTypeVisitor<R, T> visitor, Edge correspondingEdge, T state) {
      return visitor.visit(this, correspondingEdge, state);
    }

    public Rhs.Rvar getVariable () {
      return variable;
    }

    @Override public RReilAddr blockStartAddress () {
      return address;
    }
  }
}
