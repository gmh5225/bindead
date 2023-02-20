package bindead.domainnetwork.combinators;


import javalx.data.Option;
import javalx.numeric.BigInt;
import rreil.lang.util.Type;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Assign;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.exceptions.Unreachable;

/**
 * A datatype containing pending modifications to the child domain of a cardinal power domain.
 */
public abstract class FiniteChildOp {
  public abstract <D extends FiniteDomain<D>> D apply (D state);

  /**
   * @return <code>true</code> if this operation is a kill
   */
  public Kill isKill () {
    return null;
  }

  static class Introduction extends FiniteChildOp {
    private final NumVar var;
    private final BigInt value;

    protected Introduction (NumVar var) {
      this(var, null);
    }

    protected Introduction (NumVar var, BigInt value) {
      this.var = var;
      this.value = value;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.introduce(var, Type.Zeno, Option.fromNullable(value));
    }

    @Override public String toString () {
      return "intro " + var.toString() + (value == null ? "" : " := " + value);
    }
  }



  public static class Kill extends FiniteChildOp {
    private final NumVar victim;

    protected Kill (NumVar kill) {
      this.victim = kill;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.project(victim);
    }

    @Override public String toString () {
      return "kill " + victim.toString();
    }

    @Override public Kill isKill () {
      return this;
    }

    public NumVar getVar () {
      return victim;
    }
  }

  static class Assignment extends FiniteChildOp {
    private final Assign stmt;

    protected Assignment (Assign stmt) {
      this.stmt = stmt;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.eval(stmt);
    }

    @Override public String toString () {
      return stmt.toString();
    }
  }


  static class Hardcopy extends FiniteChildOp {

    private final NumVar to;
    private final NumVar from;

    protected Hardcopy (NumVar t, NumVar f) {
      this.to = t;
      this.from = f;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.copyVariable(to, from);
    }

    @Override public String toString () {
      return "copy(" + to + "<-" + from + ")";
    }
  }

  static class CopyAndPaste<T extends FiniteDomain<T>> extends FiniteChildOp {
    VarSet variables;
    private final T otherState;

    protected CopyAndPaste (VarSet variables, T otherState) {
      this.variables = variables;
      this.otherState = otherState;
    }

    @SuppressWarnings({"unchecked"}) @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.copyAndPaste(variables, (D) otherState);
    }

    @Override public String toString () {
      return "copyAndPaste: " + variables;
    }
  }

  static class Subst extends FiniteChildOp {
    private final NumVar from;
    private final NumVar to;

    protected Subst (NumVar from, NumVar to) {
      this.from = from;
      this.to = to;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.substitute(from, to);
    }

    @Override public String toString () {
      return "[" + from + "\\" + to + "]";
    }
  }

  static class Test extends FiniteChildOp {
    private final Finite.Test test;

    protected Test (Finite.Test test) {
      this.test = test;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.eval(test);
    }

    @Override public String toString () {
      return "[" + test + "]";
    }
  }

  static class AssumeEdgeNG extends FiniteChildOp {
    private final Rlin pointerVar;
    private final AddrVar targetAddr;

    protected AssumeEdgeNG (Rlin pointerNumVar, AddrVar address) {
      this.pointerVar = pointerNumVar;
      this.targetAddr = address;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.assumeEdgeNG(pointerVar, targetAddr);
    }

    @Override public String toString () {
      return "[assumeEdge " + pointerVar + "->" + targetAddr + "]";
    }
  }

  static class ExpandNG extends FiniteChildOp {
    private final AddrVar p;
    private final AddrVar e;
    private final ListVarPair nvps;

    protected ExpandNG (AddrVar p, AddrVar e, ListVarPair nvps) {
      this.p = p;
      this.e = e;
      this.nvps = nvps;
    }

    public ExpandNG (ListVarPair numVars) {
      this(null, null, numVars);
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      if (p == null) {
        assert e == null;
        D expanded = state.expandNG(nvps);
        return expanded;
      } else {
        assert e != null;
        D expanded = state.expandNG(p, e, nvps);
        return expanded;
      }
    }

    @Override public String toString () {
      String addrTrans = p == null ? "" : p + "->" + e + " ";
      return "[expand " + addrTrans + "on " + nvps + "]";
    }
  }


  static class FoldNG extends FiniteChildOp {
    private final AddrVar p;
    private final AddrVar e;
    private final ListVarPair nvps;

    protected FoldNG (AddrVar p, AddrVar e, ListVarPair nvps) {
      this.p = p;
      this.e = e;
      this.nvps = nvps;
    }

    public FoldNG (ListVarPair numVars) {
      this(null, null, numVars);
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      if (p == null) {
        assert e == null;
        D folded = state.foldNG(nvps);
        return folded;
      } else {
        assert e != null;
        D folded = state.foldNG(p, e, nvps);
        return folded;
      }
    }

    @Override public String toString () {
      String addrTrans = p == null ? "" : p + "->" + e + " ";
      return "[fold " + addrTrans + "on " + nvps + "]";
    }
  }

  static class ConcretizeAndDisconnectNG extends FiniteChildOp {
    private final AddrVar summary;
    private final VarSet concreteVars;

    protected ConcretizeAndDisconnectNG (AddrVar s, VarSet cs) {
      this.summary = s;
      this.concreteVars = cs;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      D expanded = state.concretizeAndDisconnectNG(summary, concreteVars);
      return expanded;
    }

    @Override public String toString () {
      return "[concretise " + concreteVars + " and disconnect " + summary + "]";
    }
  }

  static class BendGhostEdgesNG extends FiniteChildOp {
    private final AddrVar summary;
    private final AddrVar concrete;
    private final VarSet svs;
    private final VarSet cvs;
    private final VarSet pts;
    private final VarSet ptc;

    protected BendGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
      this.concrete = c;
      this.summary = s;
      this.svs = svs;
      this.cvs = cvs;
      this.pts = pts;
      this.ptc = ptc;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.bendGhostEdgesNG(summary, concrete, svs, cvs, pts, ptc);
    }

    @Override public String toString () {
      return "[bendGhostEdges s:" + summary + svs + " c:" + concrete + cvs + "]";
    }
  }

  static class BendBackGhostEdgesNG extends FiniteChildOp {
    private final AddrVar summary;
    private final AddrVar concrete;
    private VarSet svs;
    private VarSet cvs;
    private VarSet pts;
    private VarSet ptc;

    protected BendBackGhostEdgesNG (AddrVar s, AddrVar c, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
      this.concrete = c;
      this.summary = s;
      this.svs = svs;
      this.cvs = cvs;
      this.pts = pts;
      this.ptc = ptc;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.bendBackGhostEdgesNG(summary, concrete, svs, cvs,pts,ptc);
    }

    @Override public String toString () {
      return "[bendBackGhostEdges<" + summary + ";" + concrete + "> " + svs + ", " + cvs + "]";
    }
  }

  static class DerefTarget extends FiniteChildOp {
    private final Rlin reference;
    private final AddrVar target;
    private final VarSet contents;

    protected DerefTarget (Rlin pointerNumVar, AddrVar target, VarSet c) {
      this.reference = pointerNumVar;
      this.target = target;
      this.contents = c;
    }

    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      return state.assumePointsToAndConcretize(reference, target, contents);
    }

    @Override public String toString () {
      return "derefTarget(" + reference + ", " + contents + ")";
    }
  }

  static class Bottom extends FiniteChildOp {
    @Override public <D extends FiniteDomain<D>> D apply (D state) {
      throw new Unreachable();
    }

    @Override public String toString () {
      return "unreachable";
    }
  }
}
