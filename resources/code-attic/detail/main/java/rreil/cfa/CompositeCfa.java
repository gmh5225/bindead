package rreil.cfa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javalx.data.BigInt;
import javalx.data.Option;
import rreil.abstractsyntax.RReilAddr;
import rreil.disassembly.DisassemblyProvider;

/**
 * {@code CompositeCfa}'s represent a collection of control-flow automatons, i.e. a CFA forest.
 */
public class CompositeCfa implements Iterable<Cfa> {
  private final Map<RReilAddr, Cfa> cfaForest = new HashMap<RReilAddr, Cfa>();
  private final Map<String, RReilAddr> names = new HashMap<String, RReilAddr>();
  private final String identifier;
  private final DisassemblyProvider dis;

  public CompositeCfa (DisassemblyProvider dis) {
    this(UUID.randomUUID().toString(), dis);
  }

  public CompositeCfa (String identifier, DisassemblyProvider dis) {
    this.identifier = identifier;
    this.dis = dis;
  }

  /**
   * @return This composite-cfa's identifier.
   */
  public Cfa scanCfa (BigInt address) {
    RReilAddr entryAddress = RReilAddr.valueOf(address);
    Option<Cfa> maybeCfa = lookupCfa(entryAddress);
    if (maybeCfa.isSome()) {
      return maybeCfa.get();
    } else {
      Cfa newCfa = CfaBuilder.build(dis.decodeFrom(entryAddress), entryAddress);
      put(newCfa);
      return newCfa;
    }
  }

  public DisassemblyProvider getDisassemblyProvider () {
    return dis;
  }

  /**
   * Add a CFA to this collection and associate it with the CFA's entry address. If there is already a CFA for that
   * address then it will be replaced by the new one.
   *
   * @param cfa The CFA to be added to this CFA forest.
   */
  public final void put (Cfa cfa) {
    cfaForest.put(cfa.getEntryAddress(), cfa);
  }

  /**
   * Lookup a CFA by its address.
   *
   * @param address The address of the CFA.
   * @return The corresponding CFA or none if the CFA can't be found.
   */
  public Option<Cfa> lookupCfa (RReilAddr address) {
    return Option.fromNullable(cfaForest.get(address));
  }

  /**
   * Lookup a CFA by its name.
   *
   * @param address The name of the CFA.
   * @return The corresponding CFA or none if the CFA can't be found.
   */
  public Option<Cfa> lookupCfa (String name) {
    return Option.fromNullable(cfaForest.get(names.get(name)));
  }

  public Collection<Cfa> cfaForest () {
    return cfaForest.values();
  }

  /**
   * Returns a read-only iterator over the CFAs in this collection of CFAs.
   *
   * @return The iterator for the CFAs
   */
  @Override public Iterator<Cfa> iterator () {
    return Collections.unmodifiableCollection(cfaForest.values()).iterator();
  }
}
