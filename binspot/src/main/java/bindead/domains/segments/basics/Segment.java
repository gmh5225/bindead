package bindead.domains.segments.basics;

import java.util.List;

import javalx.data.products.P2;
import javalx.data.products.P3;
import javalx.numeric.Range;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Lin;
import bindead.abstractsyntax.memderef.AbstractMemPointer;
import bindead.abstractsyntax.memderef.AbstractPointer;
import bindead.data.MemVarSet;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.MemoryDomain;
import bindead.domainnetwork.interfaces.ProgramPoint;

/**
 * A segment set describes pieces of memory that behave in a uniform way. Examples are the stack, the heap, global
 * memory. This interface defines the common operations on a set of segments.
 *
 * @param <D> The type of the child domain
 * @param <S> The self-type
 *
 * @author Axel Simon
 */
public abstract class Segment<D extends MemoryDomain<D>> {

  /**
   * After creation of a fresh segment, perform any required initialization on the state.
   *
   * @param state initial state
   * @return a list of memory regions that should trigger a call to #trigger and
   *         a Boolean indicating if the #trigger for branches should be called on this segment and
   *         the new state in which the initial memory regions of the segment have been introduced
   */
  public abstract P3<List<MemVar>, Boolean, D> initialize (D state);

  /**
   * Inform the segment that the given variable has been written to.
   *
   * @param state the state after the variable has been assigned to
   * @param stmt the assignment that updated a variable for which #initialize has installed a trigger
   * @param the program point where the assignment happens
   *
   * @return a list of new segments and domain states
   */
  // XXX: do we need this anymore? See StackSeg initialize for the only place this is used.
  public abstract SegmentWithState<D> triggerAssignment (Lhs lhs, Rhs rhs, D state);

  /**
   * Resolve a pointer into code addresses.
   *
   * @param target the target variable or value
   * @param location the pointer used in a branch
   * @param state the child state before the branch
   * @return a list of addresses that the branch targets, the modified domain with child domain
   *         or @{code null} if this segment cannot resolve the pointer
   */
  public List<P2<RReilAddr, SegmentWithState<D>>> resolveJump (Lin target, AbstractMemPointer location, D state) {
    return null;
  }

  /**
   * Inform the segment that a given branch has been taken.
   *
   * @param target the branch target
   * @param ptrSize the size of the pointer in bits that is being dereferenced
   * @param state the child state before the branch has been taken
   * @param current the program point at which the branch lies
   * @param next the program point right after the branch (the fall-through successor point)
   * @return a list of segment, domain state pairs together with a program address where execution continues,
   *         or @{code null} if this segment cannot handle the branch
   */
  public SegmentWithState<D> evalJump (RReilAddr target, D state, ProgramPoint current, ProgramPoint next) {
    return null;
  }

  /**
   * Try to resolve a memory access. If successful, return the corresponding information of the memory dereference, the
   * possibly updated segment set and the possibly restricted state. Note that a single abstract address can turn into
   * several memory accesses if the offset allows this. If the domain does not know about the given address, {@code null}
   * is returned.
   *
   * @param pointerValue
   * @param dRef the unresolved memory reference
   * @param state the domain state in which this pointer is accessed
   * @param warn an object to send warnings to
   *
   * @return the list of resolved memory dereferences and states that are accessed or {@code null} if this segment cannot
   *         resolve the address. The states are restricted, so that the RegionAccess is definitely valid.
   *
   */
  public List<RegionAccess<D>> dereference (Lin pointerValue, AbstractPointer dRef, D state) {
    return null;
  }

  /**
   * Make two segment sets compatible as a preparation for performing a join on
   * the domains.
   *
   * @param other another segment set
   * @param state the state corresponding to this segment set
   * @param otherState the state corresponding to the other segment set
   *
   * @return the merged segment set together with the two states corresponding
   *         to the first and the second segment set
   */
  public abstract SegCompatibleState<D> makeCompatible (Segment<D> other, D state, D otherState);

  public SegmentWithState<D> summarizeHeap (D state) {
    return new SegmentWithState<D>(this, state);
  }


  /**
   * Execute the given primitive in this domain if possible.
   *
   * @param stmt the primitive including arguments and return values
   * @param state the child state
   * @return the updated segment and updated state or {@code null} if this domain cannot handle this primitive
   */
  public SegmentWithState<D> tryPrimitive (PrimOp stmt, D state) {
    return null;
  }

  /**
   * Add a new region to a segment.
   *
   * @param region identifier of the region that was used
   * @param ctx range of addresses that are mapped by this region
   * @return the segment containing the new mapping
   */
  public Segment<D> introduceRegion (MemVar region, ContentCtx ctx) {
    return null;
  }

  public abstract MemVarSet getChildSupportSet ();

  // NOP for all segments except heap segment
  public SegmentWithState<D> informAboutAssign (D state, MemVar toRegion, Range toOffset, MemVar fromRegion, Range fromOffset) {
    return new SegmentWithState<D>(this, state);
  }

  public boolean connectorsAreSane () {
    return true;
  }

  public abstract void toCompactString (StringBuilder builder, PrettyDomain childDomain);
}
