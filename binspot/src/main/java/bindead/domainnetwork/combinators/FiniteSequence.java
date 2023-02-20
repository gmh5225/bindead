package bindead.domainnetwork.combinators;

import java.util.ArrayList;
import java.util.List;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.Finite.Lhs;
import bindead.abstractsyntax.finite.Finite.Rhs;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.data.NumVar.AddrVar;
import bindead.data.VarSet;
import bindead.domainnetwork.combinators.FiniteChildOp.AssumeEdgeNG;
import bindead.domainnetwork.combinators.FiniteChildOp.BendBackGhostEdgesNG;
import bindead.domainnetwork.combinators.FiniteChildOp.BendGhostEdgesNG;
import bindead.domainnetwork.combinators.FiniteChildOp.ConcretizeAndDisconnectNG;
import bindead.domainnetwork.combinators.FiniteChildOp.CopyAndPaste;
import bindead.domainnetwork.combinators.FiniteChildOp.DerefTarget;
import bindead.domainnetwork.combinators.FiniteChildOp.ExpandNG;
import bindead.domainnetwork.combinators.FiniteChildOp.FoldNG;
import bindead.domainnetwork.combinators.FiniteChildOp.Subst;
import bindead.domainnetwork.combinators.FiniteChildOp.Test;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.exceptions.Unreachable;

/**
 * A sequence of operations on the child domain.
 */
public class FiniteSequence {
  private final List<FiniteChildOp> childTrans = new ArrayList<FiniteChildOp>(6);

  /**
   * Add an assignment to the child domain operations
   *
   * @param stmt the assignment
   */
  public void addAssignment (Lhs lhs, Rhs rhs) {
    childTrans.add(new FiniteChildOp.Assignment(FiniteFactory.getInstance().assign(lhs, rhs)));
  }

  public void addHardcopy (NumVar to, NumVar from) {
    childTrans.add(new FiniteChildOp.Hardcopy(to, from));
  }

  /**
   * Indicate that the domain is actually bottom. This action throws the {@link Unreachable} exception when
   * applied to the child domain.
   */
  public void addBottom () {
    childTrans.clear();
    childTrans.add(new FiniteChildOp.Bottom());
  }

  /**
   * Add a new variable of value TOP to the child domain.
   *
   * @param var the variable
   */
  public void addIntro (NumVar var) {
    childTrans.add(new FiniteChildOp.Introduction(var));
  }

  /**
   * Add a new variable to the child domain.
   *
   * @param var the variable
   * @param value the initial value
   */
  public void addIntro (NumVar var, BigInt value) {
    childTrans.add(new FiniteChildOp.Introduction(var, value));
  }

  /**
   * Add a new variable to the child domain with value zero.
   *
   * @param var the variable
   * @param snd the initial value
   */
  public void addIntroZero (NumVar var) {
    childTrans.add(new FiniteChildOp.Introduction(var, Bound.ZERO));
  }

  /**
   * Add a variable to be projected out.
   *
   * @param var the variable to be projected out
   */
  public void addKill (NumVar var) {
    childTrans.add(new FiniteChildOp.Kill(var));
  }

  /**
   * Add an instruction to copy and paste variables from another state into this state.
   *
   * @param variables The variables that should be copied over.
   * @param otherState The state for the variables.
   */
  public <D extends FiniteDomain<D>> void addCopyAndPaste (VarSet variables, D otherState) {
    if (!variables.isEmpty())
      childTrans.add(new CopyAndPaste<D>(variables, otherState));
  }

  /**
   * Add a substitution. All occurrences of {@code x} will be substituted with {@code y}.
   *
   * @param from The variable to be removed.
   * @param to The variable to be added.
   */
  public void addSubst (NumVar from, NumVar to) {
    childTrans.add(new Subst(from, to));
  }

  public void addTest (Finite.Test test) {
    childTrans.add(new Test(test));
  }

  public int length () {
    return childTrans.size();
  }

  public boolean isEmpty () {
    return childTrans.isEmpty();
  }



  <D extends FiniteDomain<D>> D apply (D state) {
    for (FiniteChildOp action : childTrans) {
      state = action.apply(state);
    }
    // the Undef domain calls apply() more than once, therefore we
    // must remove the applied ops from the queue.
    childTrans.clear();
    return state;
  }

  @Override public String toString () {
    String res = "[";
    String sep = "";
    for (FiniteChildOp ct : childTrans) {
      res += sep + ct.toString();
      sep = "; ";
    }
    return res + "]";
  }

  public void addDerefTarget (Rlin pointerNumVar, AddrVar target, VarSet contents) {
    childTrans.add(new DerefTarget(pointerNumVar, target, contents));
  }

  public void addAssumeEdgeNG (Rlin pointerNumVar, AddrVar address) {
    childTrans.add(new AssumeEdgeNG(pointerNumVar, address));
  }

  public void addExpandNG (ListVarPair numVars) {
    childTrans.add(new ExpandNG(numVars));
  }

  public void addExpandNG (AddrVar address, AddrVar address2, ListVarPair numVars) {
    childTrans.add(new ExpandNG(address, address2, numVars));
  }

  public void addConcretiseAndDisconnectNG (AddrVar summary, VarSet concreteVars) {
    childTrans.add(new ConcretizeAndDisconnectNG(summary, concreteVars));
  }

  public void addBendBackGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    childTrans.add(new BendBackGhostEdgesNG(summary, concrete, svs, cvs,pts, ptc));
  }

  public void addFoldNG (AddrVar summary, AddrVar concrete, ListVarPair vps) {
    childTrans.add(new FoldNG(summary, concrete, vps));

  }
  public void addFoldNG (ListVarPair vps) {
    childTrans.add(new FoldNG(null, null,vps));

  }

  public void addBendGhostEdgesNG (AddrVar summary, AddrVar concrete, VarSet svs, VarSet cvs, VarSet pts, VarSet ptc) {
    childTrans.add(new BendGhostEdgesNG(summary, concrete, svs, cvs,pts,ptc));
  }



}