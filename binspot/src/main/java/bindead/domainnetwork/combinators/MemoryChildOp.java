package bindead.domainnetwork.combinators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javalx.data.Option;
import javalx.numeric.BigInt;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.Rhs;
import rreil.lang.util.Type;
import bindead.data.NumVar;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RegionCtx;

/**
 * A datatype containing pending modifications to the child domain of a cardinal power domain.
 */
public abstract class MemoryChildOp {
  public abstract <D extends MemoryDomain<D>> D apply (D state);

  private static class RegionIntroduction extends MemoryChildOp {
    private final MemVar region;
    private final RegionCtx ctx;

    protected RegionIntroduction (MemVar region, RegionCtx ctx) {
      this.region = region;
      this.ctx = ctx;
    }

    @Override public String toString () {
      return "intro " + region + ": " + ctx;
    }

    @Override public <D extends MemoryDomain<D>> D apply (D state) {
      return state.introduceRegion(region, ctx);
    }
  }

  private static class Introduction extends MemoryChildOp {
    private final NumVar variable;
    private final Type type;

    protected Introduction (NumVar variable, Type type) {
      this.variable = variable;
      this.type = type;
    }

    @Override public String toString () {
      return "intro " + variable.toString();
    }

    @Override public <D extends MemoryDomain<D>> D apply (D state) {
      return state.introduce(variable, type, Option.<BigInt>none());
    }
  }

  private static class Assignment extends MemoryChildOp {
    private final Lhs lhs;
    private final Rhs rhs;
    private final Option<ProgramPoint> point;

    protected Assignment (Lhs lhs, Rhs rhs, Option<ProgramPoint> point) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.point = point;
    }

    @Override public String toString () {
      return lhs + " := " + rhs + " @ " + point;
    }

    @Override public <D extends MemoryDomain<D>> D apply (D state) {
      return state.evalAssign(lhs, rhs);
    }
  }

  /**
   * A sequence of operations on the child domain.
   */
  public static class Sequence implements Iterable<MemoryChildOp> {
    private final List<MemoryChildOp> childTrans = new ArrayList<MemoryChildOp>(6);

    @Override public Iterator<MemoryChildOp> iterator () {
      return childTrans.iterator();
    }

    /**
     * Add an assignment to the child domain operations
     *
     * @param stmt the assignment
     */
    public void addAssignment (Lhs lhs, Rhs rhs, Option<ProgramPoint> point) {
      childTrans.add(new MemoryChildOp.Assignment(lhs, rhs, point));
    }

    /**
     * Add a new region with the given context to the child domain.
     *
     * @param var the variable
     * @param value the initial value
     */
    public void addRegionIntro (MemVar region, RegionCtx ctx) {
      childTrans.add(new MemoryChildOp.RegionIntroduction(region, ctx));
    }

    public void addIntro (NumVar variable, Type type) {
      childTrans.add(new Introduction(variable, type));
    }


    public <D extends MemoryDomain<D>> D apply (D state) {
      for (MemoryChildOp action : childTrans) {
        state = action.apply(state);
      }
      return state;
    }

    @Override public String toString () {
      String res = "";
      String sep = "";
      for (MemoryChildOp ct : childTrans) {
        res += sep + ct.toString();
        sep = "; ";
      }
      return res;
    }
  }
}
