package bindead.analyses.systems.natives;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import rreil.lang.RReil;
import rreil.lang.RReil.Assertion;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.Native;
import rreil.lang.RReil.Nop;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.RReilAddr;

public class FunctionDefinition {
  private final SortedMap<RReilAddr, RReil> instructions;

  public FunctionDefinition (SortedMap<RReilAddr, RReil> instructions) {
    this.instructions = instructions;
  }

  public SortedMap<RReilAddr, RReil> getInstructions () {
    return instructions;
  }

  public boolean reachedEnd(RReilAddr addr) {
    return addr.equals(instructions.lastKey().nextBase());
  }


  public FunctionDefinition prepend (List<RReil> prolog) {
    SortedMap<RReilAddr, RReil> instructions = getInstructions();
    SortedMap<RReilAddr, RReil> newInstructions = new TreeMap<RReilAddr, RReil>();

    // Prepend prolog
    long addrBase = 0;
    addrBase = addInstructions(prolog, addrBase, newInstructions);

    // Refill old
    addrBase = addInstructions(instructions.values(), addrBase, newInstructions);

    return new FunctionDefinition(newInstructions);
  }

  public FunctionDefinition append (List<RReil> prolog) {
    SortedMap<RReilAddr, RReil> instructions = getInstructions();
    SortedMap<RReilAddr, RReil> newInstructions = new TreeMap<RReilAddr, RReil>();

    // Append prolog
    long addrBase = instructions.lastKey().base() + 1;
    addrBase = addInstructions(prolog, addrBase, newInstructions);

    return new FunctionDefinition(newInstructions);
  }

  private static long addInstructions(Collection<RReil> instructions, long newAddrBase, SortedMap<RReilAddr, RReil> to) {
    for (RReil rreil : instructions) {
      RReilAddr newAddr = RReilAddr.valueOf(newAddrBase);
      RReil newRreil = copyWithNewAddr(rreil, newAddr);
      to.put(newAddr, newRreil);
      newAddrBase++;
    }
    return newAddrBase;
  }

  private static RReil copyWithNewAddr(RReil rreil, RReilAddr addr) {
    RReil newRreil = null;
    if (rreil instanceof Assign) {
      Assign assign = (Assign) rreil;
      newRreil = new Assign(addr, assign.getLhs(), assign.getRhs());
    } else if (rreil instanceof Load) {
      Load load = (Load) rreil;
      newRreil = new Load(addr, load.getLhs(), load.getReadAddress());
    } else if (rreil instanceof Store) {
      Store store = (Store) rreil;
      newRreil = new Store(addr, store.getWriteAddress(), store.getRhs());
    } else if (rreil instanceof Branch) {
      Branch branch = (Branch) rreil;
      newRreil = new Branch(addr, branch.getTarget(), branch.getBranchType());
    } else if (rreil instanceof PrimOp) {
      PrimOp primOp = (PrimOp) rreil;
      newRreil = new PrimOp(addr, primOp.getName(), primOp.getOutArgs(), primOp.getInArgs());
    } else if (rreil instanceof Native) {
      Native nativee = (Native) rreil;
      newRreil = new Native(addr, nativee.getName(), nativee.getOpnd());
    } else if (rreil instanceof Assertion) {
      // FIXME: assertion instructions need to be distinguished by type, so not easy to clone here this way
      // better implement a clone() on RREIL instructions that this hack here!
    } else if (rreil instanceof Nop) {
      newRreil = new Nop(addr);
    } else {
      throw new UnsupportedOperationException("RReil type not supported yet!");
    }
    return newRreil;
  }
}
