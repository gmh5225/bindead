package bindead.analysis.util;

import bindead.domainnetwork.interfaces.RootDomain;
import bindead.exceptions.EvaluationException;
import bindead.exceptions.Unreachable;
import java.util.ListIterator;
import javalx.digraph.Digraph.Edge;
import rreil.abstractsyntax.RReil;
import rreil.cfa.Block;
import rreil.cfa.Cfa.CfaBlock;

public class BackwardEvaluator<D extends RootDomain<D>> extends CfgEdgeTypeVisitorSkeleton<D> {
  @Override public D visit (final CfaBlock block, Edge correspondingEdge, D domain) {
    Block instructions = block.getBlock();
    ListIterator<RReil.Statement> it = instructions.listIterator(instructions.size());
    while (it.hasPrevious()) {
      RReil.Statement insn = it.previous();
      try {
        domain = domain.eval(insn);
      } catch (Unreachable bottom) {
        // this exception is needed for control flow and thus needs to be propagated up the call stack
        throw bottom;
      } catch (Throwable cause) {
        // by using Throwable we catch here everything, i.e. also VM errors and whatever. Might be too much but
        // might also be useful to have a partial analysis result even on unexpected errors. Also we need to catch
        // assertion errors.
        throw new EvaluationException(cause, insn.getRReilAddress(), insn.toString());
      }
    }
    return domain;
  }
}
