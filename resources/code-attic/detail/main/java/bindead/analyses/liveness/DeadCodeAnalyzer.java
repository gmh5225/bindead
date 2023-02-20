package bindead.analyses.liveness;

import bindead.analyses.Analysis;
import bindead.domainnetwork.interfaces.RootDomain;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import javalx.digraph.Digraph.Edge;
import javalx.digraph.Digraph.Vertex;
import javalx.digraph.algorithms.dfs.Dfs;
import javalx.digraph.algorithms.dfs.DfsVertexVisitor;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReil.Assign;
import rreil.abstractsyntax.RReil.Lhs;
import rreil.abstractsyntax.RReil.Load;
import rreil.abstractsyntax.RReil.Native;
import rreil.abstractsyntax.RReil.Nop;
import rreil.abstractsyntax.RReil.PrimOp;
import rreil.abstractsyntax.RReil.Statement;
import rreil.abstractsyntax.RReil.Store;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.util.RReilStatementVisitor;
import rreil.cfa.Block;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaBlock;
import rreil.cfa.Cfa.CfaCall;
import rreil.cfa.Cfa.CfaComputedFlow;
import rreil.cfa.Cfa.CfaReturn;
import rreil.cfa.Cfa.CfaTest;
import rreil.cfa.CfaEdgeTypeVisitor;

/**
 * Run this analyzer after a liveness analysis to get rid of dead-assignments.
 */
public final class DeadCodeAnalyzer implements DfsVertexVisitor, CfaEdgeTypeVisitor<Void, Vertex> {
  private final SortedSet<RReilAddr> deadCodeAddresses = new TreeSet<RReilAddr>(RReilAddr.comparator);
  private final Cfa cfa;
  private final Analysis<?> analysis;

  public DeadCodeAnalyzer (Analysis<?> analysis) {
    this.cfa = analysis.getCfa();
    this.analysis = analysis;
  }

  public void run () {
    final Dfs dfs = new Dfs(this);
    dfs.run(cfa.getEntry());
  }

  public SortedSet<RReilAddr> getDeadCodeAddresses () {
    return deadCodeAddresses;
  }

  @Override public void visitOnDiscovery (Vertex v) {
    for (Edge e : v.incoming()) {
      cfa.getTransitionType(e).accept(this, e, v);
    }
  }

  @Override public void visitWhenFinished (Vertex v) {
  }

  @Override public Void visit (CfaBlock block, Edge correspondingEdge, Vertex v) {
    Block instructions = block.getBlock();
    ListIterator<RReil.Statement> it = instructions.listIterator(instructions.size());
    RootDomain<?> state = analysis.getState(v).get();
    while (it.hasPrevious()) {
      RReil.Statement stmt = it.previous();
      Lhs lhs = LhsGrabber.run(stmt);
      if (lhs != null && !state.queryLiveness(lhs.asUnresolvedField()))
        deadCodeAddresses.add(stmt.getRReilAddress());
      state = state.eval(stmt);
    }
    return null;
  }

  @Override public Void visit (CfaTest test, Edge correspondingEdge, Vertex v) {
    return null;
  }

  @Override public Void visit (CfaCall call, Edge correspondingEdge, Vertex v) {
    return null;
  }

  @Override public Void visit (CfaReturn ret, Edge correspondingEdge, Vertex domain) {
    return null;
  }

  @Override public Void visit (CfaComputedFlow flow, Edge correspondingEdge, Vertex v) {
    return null;
  }

  private static final class LhsGrabber implements RReilStatementVisitor<Lhs, Void> {
    private static final LhsGrabber $ = new LhsGrabber();

    public static Lhs run (Statement stmt) {
      return stmt.accept($, null);
    }

    @Override public Lhs evalAssign (Assign insn, Void _) {
      return insn.getLhs();
    }

    @Override public Lhs evalAssign (Load insn, Void _) {
      return insn.getLhs();
    }

    @Override public Lhs evalAssign (Store insn, Void _) {
      return null;
    }

    @Override public Lhs evalAssign (Nop insn, Void _) {
      return null;
    }

    @Override public Lhs evalNative (Native insn, Void data) {
      return null;
    }

    @Override public Lhs evalPrimOp (PrimOp insn, Void data) {
      return null;
    }
  }
}
