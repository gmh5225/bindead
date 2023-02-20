package bindead.analyses.algorithms.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import rreil.lang.RReilAddr;
import bindead.analyses.algorithms.data.Flows.Successor;
import bindead.domainnetwork.interfaces.RootDomain;

/**
 * A container for control flow transitions from a program point to its successor points.
 */
public class Flows<D extends RootDomain<D>> implements Iterable<Successor<D>> {
  private final List<Successor<D>> successors;

  /**
   * Used to improve the evaluation order in the CFG, i.e. evaluate a loop till it is stable before propagating
   * the state outside of it. The heuristic to achieve this is to first visit lower addresses in the program,
   * independently if it is a jump, call or a fall-through. For loops this helps as a jump over the loop would go
   * to a higher address and a jump backwards to the loop beginning leads to a lower address, thus we would
   * always take the jump that stays inside the loop until it becomes stable.
   * As this is just a heuristic there are of course cases where it does not help but for the most common
   * loop constructions in binary code it is very useful.
   */
  private final Comparator<Successor<D>> smallerAddressesFirstComparator = new Comparator<Successor<D>>() {

    @Override public int compare (Successor<D> o1, Successor<D> o2) {
      if (o1.getAddress() == null && o2.getAddress() == null)
        return 0;
      else if (o1.getAddress() == null)
        return 1;
      else if (o2.getAddress() == null)
        return -1;
      else
        return o1.getAddress().compareTo(o2.getAddress());
    }
  };

  public Flows () {
    successors = new ArrayList<Successor<D>>();
  }

  public static <D extends RootDomain<D>> Flows<D> next (RReilAddr address, D state) {
    return new Flows<D>().addNext(address, state);
  }

  public static <D extends RootDomain<D>> Flows<D> halt (D state) {
    return new Flows<D>().addHalt(state);
  }

  public static <D extends RootDomain<D>> Flows<D> error (D state) {
    return new Flows<D>().addError(state);
  }

  /**
   * The fall-through case.
   *
   * @param address The address of the successor.
   * @param state The state to be propagated to the successor.
   */
  public Flows<D> addNext (RReilAddr address, D state) {
    successors.add(new Successor<D>(FlowType.Next, address, state));
    return this;
  }

  /**
   * A jump to an address.
   *
   * @param address The address of the successor.
   * @param state The state to be propagated to the successor.
   */
  public Flows<D> addJump (RReilAddr address, D state) {
    successors.add(new Successor<D>(FlowType.Jump, address, state));
    return this;
  }

  /**
   * A call-jump to an address.
   *
   * @param address The address of the successor.
   * @param state The state to be propagated to the successor.
   */
  public Flows<D> addCall (RReilAddr address, D state) {
    successors.add(new Successor<D>(FlowType.Call, address, state));
    return this;
  }

  /**
   * A return-jump to an address.
   *
   * @param address The address of the successor.
   * @param state The state to be propagated to the successor.
   */
  public Flows<D> addReturn (RReilAddr address, D state) {
    successors.add(new Successor<D>(FlowType.Return, address, state));
    return this;
  }

  /**
   * The analysis should stop as the program ends here.
   *
   * @param state The result state propagated to the exit point.
   */
  public Flows<D> addHalt (D state) {
    successors.add(new Successor<D>(FlowType.Halt, null, state));
    return this;
  }

  /**
   * The analysis encountered an error.
   *
   * @param state The state while encountering the error.
   */
  public Flows<D> addError (D state) {
    successors.add(new Successor<D>(FlowType.Error, null, state));
    return this;
  }

  public boolean isEmpty () {
    return successors.isEmpty();
  }

  @Override public Iterator<Successor<D>> iterator () {
    // NOTE: this is not needed anymore when using the global order of addresses in the worklist
    // Still for some experiments it might be useful, so it is kept around for now.
//    Collections.sort(successors, smallerAddressesFirstComparator);
    return successors.iterator();
  }

  @Override public String toString () {
    return successors.toString();
  }

  /**
   * The type of control flow change by which the successor was reached.
   */
  public static enum FlowType {
    Jump,
    Next,
    Call,
    Return,
    Halt,
    Error;
  }

  /**
   * A successor for a program point in the fixpoint analysis.
   * A successor is represented by its code address, the state
   * to propagate and the type of the transition leading to it.
   */
  public static class Successor<D extends RootDomain<D>> {
    final FlowType type;
    final RReilAddr address;
    final D state;

    public Successor (FlowType type, RReilAddr address, D state) {
      this.type = type;
      this.address = address;
      this.state = state;
    }

    /**
     * The type of the transition that lead to this successor.
     */
    public FlowType getType () {
      return type;
    }

    /**
     * The target address of the successor (not where it was reached from!).
     */
    public RReilAddr getAddress () {
      return address;
    }

    /**
     * The state to propagate to the target of this successor. This is the state
     * coming from the point this successor was reached and not the one at the address of the successor!
     */
    public D getState () {
      return state;
    }

    @Override public String toString () {
      if (address != null)
        return type + "(" + address.toShortString() + ")";
      else
        return type.toString();
    }
  }
}