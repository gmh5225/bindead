package bindead.abstractsyntax.memderef;

import javalx.data.Option;
import javalx.numeric.Range;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.data.Linear;
import bindead.data.NumVar.AddrVar;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.interfaces.MemoryDomain;

public class AbstractPointer {
  private static final FiniteFactory fin = FiniteFactory.getInstance();

  public final AddrVar address;
  public final Linear offset;

  static public AbstractPointer relativeTo (final Linear offset, final AddrVar addr) {
    return new AbstractPointer(addr, offset.sub(addr));
  }

  static public AbstractPointer absolute (Linear offset) {
    return new AbstractPointer(null, offset);
  }

  private AbstractPointer (AddrVar addr, Linear offset) {
    this.address = addr;
    this.offset = offset;
  }

  final public AbstractPointer setAddr (AddrVar a) {
    return new AbstractPointer(a, offset);
  }

  final public AbstractPointer addOffset (Linear ofs) {
    return new AbstractPointer(address, offset.add(ofs));
  }

  /**
   * Query the l-value of this memory access.
   *
   * @return {@link Option.none} if this access is to an absolute address
   */
  public Option<AddrVar> getAddrVar () {
    return Option.fromNullable(address);
  }

  @Override public String toString () {
    return address + "[" + offset + "]";
  }

  /**
   * Ask for a set of integer offsets of this pointer.
   *
   * @param state the state in which this memory dereference object is valid
   * @return the possible offsets
   */
  public final Range getExplicitOffset (QueryChannel state) {
    return state.queryRange(offset);
  }

  /**
   * Compute the state space in which it is possible to access a memory region before it begins.
   * Throws an #Unreachable exception if no erroneous access is possible.
   *
   * @param state the state to restrict
   * @param lower the first valid byte at the beginning of the region
   * @return the erroneous state that accesses in front of the first valid byte
   */
  public final <D extends MemoryDomain<D>> D calcBelowAccess (D state, Linear lower) {
    return state.eval(fin.unsignedLessThan(0, offset, lower));
  }

  /**
   * Compute the state space in which it is possible to access a memory region beyond where it ends.
   * Throws an #Unreachable exception if no erroneous access is possible.
   *
   * @param state the state to restrict
   * @param upper the first invalid byte at the end of the region
   * @return the erroneous state that accesses beyond the last valid byte
   */
  public final <D extends MemoryDomain<D>> D calcAboveAccess (D state, Linear upper) {
    return state.eval(fin.unsignedLessThanOrEqualTo(0, upper, offset));
  }

  public boolean isAbsolute () {
    return address == null;
  }

  public static AbstractPointer createMemDeref (Finite.Rlin l, AddrVar addr) {
    Linear linearTerm = l.getLinearTerm();
    if (addr != null)
      linearTerm = linearTerm.sub(addr);
    return new AbstractPointer(addr, linearTerm);
  }

}
