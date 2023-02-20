package bindead.domainnetwork.channels;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.persistentcollections.MultiMap;
import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.data.Linear;
import bindead.data.VarSet;

/**
 * Class representing synthesized information-flow, that is information that
 * travels upwards, e.g. from a child domain to its parent.
 * TODO bm: we need to implement a join for the synthChannel to be able to join the equations, predicates
 * when a sequence of transfer functions is performed. Additionally it is necessary to make this channel mutable
 * in the domains to be able to set it for the last state in a sequence. Even more, each transfer function
 * would need to be applied to the synthChannel to invalidate the information stored here e.g. if a variable
 * has been overwritten that has equalities stored in this channel.<br>
 * Currently the channel is mutable because of {@link #addEquation(Linear)}, {@link #addEquations(SetOfEquations)},
 * {@link #setPredicatesOld(P3)} and at least one of them is necessary for the above problem case.<br>
 * The whole concept needs to be rethought and reimplemented such that it causes the least pain!
 *
 * hsi:
 *
 * confusion comes from the misunderstanding that a channel is an object.
 * a channel is a way to send messages to objects, that is, a reference and an Interface.
 * what we have implemented as SynthChannel is in fact a primitive domain.
 * This primitive domain is defined and used by us in an ad-hoc manner.
 *
 * Possible solution:
 * since childops are called from builders
 * and builders are mutable,
 * the child domain can send messages to the builder.
 * the builder will have to know how to react to these messages.
 *
 *  In order to make this work, we have to
 *  - abandon the childOp sequences, and
 *  - standardize a Builder interface
 *
 */
public class SynthChannel {
  private SetOfEquations equations = SetOfEquations.empty();
  private MultiMap<Test, Test> implications = MultiMap.empty();

  @Override
  public SynthChannel clone () {
    SynthChannel newChannel = new SynthChannel();
    newChannel.equations = this.equations;
    newChannel.implications = this.implications;
    return newChannel;
  }

  public boolean isEmpty () {
    return equations.isEmpty() && implications.isEmpty();
  }

  public void setEquations (SetOfEquations newEquations) {
    equations = newEquations;
  }

  public void addEquations (SetOfEquations newEquations) {
    equations = equations.union(newEquations);
  }

  public void addEquation (Linear eq) {
    equations = equations.add(eq.toEquality());
  }

  public SetOfEquations getEquations () {
    return equations;
  }

  public MultiMap<Test, Test> getImplications () {
    return implications;
  }

  public void setImplications (MultiMap<Test, Test> implications) {
    this.implications = implications;
  }

  public SynthChannel intersect (SynthChannel other) {
    SynthChannel merged = new SynthChannel();
    merged.equations = equations.intersect(other.equations);
    merged.implications = implications.intersection(other.implications);
    return merged;
  }

  /**
   * Query all variables mentioned in this channel.
   *
   * @return the set of all variables that are mentioned in this channel
   */
  public VarSet getVariables () {
    VarSet vars = VarSet.empty();
    for (Linear eq : equations) {
      vars = vars.union(eq.getVars());
    }
    for (P2<Test, Test> implication : implications) {
      vars = vars.union(implication._1().getVars()).union(implication._2().getVars());
    }
    return vars;
  }

  /**
   * Create a new synthesized channel that does not reveal any information
   * over the passed-in variables.
   *
   * @param toRemove the variables to remove from the channel information
   * @return the new channel containing no information on {@code toRemove}
   *
   */
  public SynthChannel removeVariables (VarSet toRemove) {
    if (toRemove.isEmpty())
      return this;
    SynthChannel res = new SynthChannel();
    for (Linear eq : equations) {
      if (!eq.getVars().containsAny(toRemove))
        res.equations = res.equations.add(eq);
    }
    MultiMap<Test, Test> newImplications = implications;
    for (P2<Test, Test> implication : implications) {
      if (implication._1().getVars().containsAny(toRemove) || implication._2().getVars().containsAny(toRemove))
        newImplications = newImplications.remove(implication._1(), implication._2());
    }
    res.setImplications(newImplications);
    return res;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Equations: ");
    builder.append(equations);
    builder.append("\n");
    builder.append("Implications: ");
    builder.append(implications);
    builder.append("\n");
    return builder.toString();
  }
}
