package bindead.analysis.util;

import bindead.data.Range;
import bindead.domainnetwork.interfaces.RootDomain;
import javalx.data.BigInt;
import javalx.digraph.Digraph.Edge;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.Rhs.Rvar;
import rreil.cfa.Cfa;
import rreil.cfa.Cfa.CfaComputedFlow;
import rreil.cfa.util.CfaStateException;
import rreil.disassembly.DisassemblyProvider;

public class ForwardEvaluator<D extends RootDomain<D>> extends CfgEdgeTypeVisitorSkeleton<D> {
  // chosen by experience. encountered jump tables about that size
  private static final int maxAllowedJumpTargets = 200;
  private final Cfa extender;
  private final DisassemblyProvider dis;

  public ForwardEvaluator (Cfa cfa, DisassemblyProvider dis) {
    this.extender = cfa;
    this.dis = dis;
  }

  public Cfa getCfa () {
    return extender;
  }

  @Override public D visit (CfaComputedFlow flow, Edge flowEdge, D domain) {
    D state = super.visit(flow, flowEdge, domain);
    Rvar variable = flow.getVariable();
    Range targetAddresses = state.queryRange(variable.asUnresolvedField());
    if (!targetAddresses.isFinite())
      throw new CfaStateException(CfaStateException.ErrObj.TOO_MANY_JUMP_TARGETS, "infinitely many");
    if (targetAddresses.numberOfDiscreteValues() > maxAllowedJumpTargets)
      throw new CfaStateException(CfaStateException.ErrObj.TOO_MANY_JUMP_TARGETS,
          targetAddresses.numberOfDiscreteValues() + "");

    for (BigInt targetAddress : targetAddresses) {
      getCfa().extendFrom(flowEdge, RReilAddr.valueOf(targetAddress), dis);
    }
    return state;
  }
}
