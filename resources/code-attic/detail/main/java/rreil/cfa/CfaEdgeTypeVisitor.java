package rreil.cfa;

import javalx.digraph.Digraph.Edge;
import rreil.cfa.Cfa.CfaBlock;
import rreil.cfa.Cfa.CfaCall;
import rreil.cfa.Cfa.CfaComputedFlow;
import rreil.cfa.Cfa.CfaReturn;
import rreil.cfa.Cfa.CfaTest;

public interface CfaEdgeTypeVisitor<R, T> {
  public R visit (CfaBlock block, Edge correspondingEdge, T domain);

  public R visit (CfaTest test, Edge correspondingEdge, T domain);

  public R visit (CfaCall call, Edge correspondingEdge, T domain);

  public R visit (CfaReturn ret, Edge correspondingEdge, T domain);

  public R visit (CfaComputedFlow flow, Edge correspondingEdge, T domain);
}
