package bindead.domains.segments.machine;

import java.util.LinkedList;
import java.util.List;

import javalx.data.products.P3;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil.PrimOp;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import bindead.data.MemVarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domains.segments.basics.SegCompatibleState;
import bindead.domains.segments.basics.Segment;
import bindead.domains.segments.basics.SegmentWithState;
import bindead.exceptions.DomainStateException.InvariantViolationException;

/**
 * Track the set of memory regions that are used to represent registers in the processor.
 *
 * @author Axel Simon
 */
public class Processor<D extends MemoryDomain<D>> extends Segment<D> {
  public static final String NAME = "Processor";
  private final MemVarSet registers;

  public Processor () {
    registers = MemVarSet.empty();
  }

  private Processor (MemVarSet registers) {
    this.registers = registers;
  }

  @Override public P3<List<MemVar>, Boolean, D> initialize (D state) {
    for (MemVar region : registers)
      state = state.introduceRegion(region, RegionCtx.EMPTYSTICKY);
    return P3.<List<MemVar>, Boolean, D>tuple3(new LinkedList<MemVar>(), Boolean.FALSE, state);
  }

  @Override public SegmentWithState<D> triggerAssignment (Lhs lhs, rreil.lang.Rhs rhs, D state) {
    throw new InvariantViolationException();
  }

  @Override public SegCompatibleState<D> makeCompatible (Segment<D> otherRaw, D state, D otherState) {
    Processor<D> other = (Processor<D>) otherRaw;
    MemVarSet union = registers.insertAll(other.registers);
    D newState = state;
    for (MemVar region : union.difference(registers))
      newState = newState.introduceRegion(region, RegionCtx.EMPTYSTICKY);
    D newOtherState = otherState;
    for (MemVar region : union.difference(other.registers))
      newOtherState = newOtherState.introduceRegion(region, RegionCtx.EMPTYSTICKY);
    return new SegCompatibleState<D>(new Processor<D>(union), newState, newOtherState);
  }

  @Override public SegmentWithState<D> tryPrimitive (PrimOp prim, D state) {
    if (prim.is("addRegisters", Integer.MAX_VALUE, Integer.MAX_VALUE)) {
      List<MemVar> newRegisters = new LinkedList<MemVar>();
      for (Lhs variable : prim.getOutArgs()) {
        newRegisters.add(variable.getRegionId());
      }
      for (Rval rhs : prim.getInArgs()) {
        if (!(rhs instanceof Rvar))
          continue;
        newRegisters.add(((Rvar) rhs).getRegionId());
      }
      MemVarSet updatedRegisters = registers;
      D updatedState = state;
      for (MemVar register : newRegisters) {
        if (updatedRegisters.contains(register))
          continue;
        updatedRegisters = updatedRegisters.insert(register);
        updatedState = updatedState.introduceRegion(register, RegionCtx.EMPTYSTICKY);
      }
      return new SegmentWithState<D>(new Processor<D>(updatedRegisters), updatedState);
    }
    return null;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append(NAME + ": ").append(registers);
    return builder.toString();
  }

  @Override public MemVarSet getChildSupportSet () {
    return registers;
  }

  @Override public void toCompactString (StringBuilder builder, PrettyDomain childDomain) {
    builder.append(NAME + "\n");
    for (MemVar reg : registers) {
      builder.append("    " + reg + " = ");
      childDomain.memVarToCompactString(builder, reg);
      builder.append('\n');
    }
  }
}
