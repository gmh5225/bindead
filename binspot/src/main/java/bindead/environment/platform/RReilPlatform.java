package bindead.environment.platform;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rvar;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.lowlevel.RReilHighLevelToLowLevelWrapper;
import rreil.lang.util.RvarExtractor;
import bindead.analyses.Bootstrap;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.Disassembler;
import binparse.rreil.RReilBinary;

/**
 * Platform for a fictional RREIL virtual machine.
 */
public class RReilPlatform extends Platform {
  private static final String stackPointerName = "sp";
  private static final String instructionPointerName = "ip";
  private final MemVar initialStackRegionId;
  private final Map<String, Rvar> knownRegisters = new HashMap<>();

  private RReilPlatform (int defaultSize, Disassembler dis) {
    super(dis);
    this.initialStackRegionId = MemVar.getVarOrFresh("stack.rreil_" + defaultSize);
  }

  public RReilPlatform (int defaultSize, final SortedMap<RReilAddr, RReil> instructions) {
    this(defaultSize, new Disassembler(RReilBinary.rreilArch, defaultSize, null, ByteOrder.LITTLE_ENDIAN) {
      @Override public Instruction decodeOne (DecodeStream in, long pc) throws DecodeException {
        RReilAddr nativeAddress = RReilAddr.valueOf(pc);
        RReil firstInstruction = instructions.get(nativeAddress);
        if (firstInstruction == null)
          throw new IllegalArgumentException("No instruction at that address: " + pc);

        List<RReil> allIntraRReilInstructions = new LinkedList<RReil>();
        allIntraRReilInstructions.add(firstInstruction);
        int size = 1;
        SortedMap<RReilAddr, RReil> successorInstructions = instructions.tailMap(nativeAddress.nextOffset());
        while (!successorInstructions.isEmpty()) {
          RReilAddr nextAddress = successorInstructions.firstKey();
          if (nextAddress.offset() == 0) { // we encountered a native instruction
            size = (int) (nextAddress.base() - nativeAddress.base());
            break;
          }
          RReil nextInstruction = successorInstructions.get(nextAddress);
          allIntraRReilInstructions.add(nextInstruction);
          successorInstructions = successorInstructions.tailMap(nextAddress.nextOffset());
        }
        return new InstructionWrapper(size, allIntraRReilInstructions);
      }

      @Override public LowLevelRReilOpnd translateIdentifier (String name) throws TranslationException {
        throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override public String getArchitectureName () {
        return RReilBinary.rreilArch;
      }
    });
    extractRegisterMapping(instructions);
  }

  private void extractRegisterMapping (SortedMap<RReilAddr, RReil> instructions) {
    for (RReil insn : instructions.values()) {
      for (Rvar variable : RvarExtractor.getUnique(insn)) {
        String name = variable.getRegionId().getName();
        // prefer bigger variables and the ones without offsets if there is a name clash
        if (knownRegisters.containsKey(name)) {
          Rvar currentlyKnownVariable = knownRegisters.get(name);
          if (currentlyKnownVariable.getSize() < variable.getSize())
            knownRegisters.put(name, variable);
          else if (currentlyKnownVariable.getOffset() != 0 && variable.getOffset() == 0)
            knownRegisters.put(name, variable);
        } else {
          knownRegisters.put(name, variable);
        }
      }
    }
  }

  @Override public Rvar getRegisterAsVariable (String name) {
    return knownRegisters.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override public Bootstrap forwardAnalysisBootstrap () {
    return new Bootstrap() {
      @Override public <D extends RootDomain<D>> D bootstrap (D state, long initialPC) {
        // TODO: only here for the old ROOT domain. remove when root is deleted
        state = state.introduceRegion(MemVar.getVarOrFresh("stack"), RegionCtx.EMPTYSTICKY);
        state = initializeStackPointer(state);
        state = initializeInstructionPointer(state, initialPC);
        return state;
      }
    };
  }

  @Override public String getStackPointer () {
    return stackPointerName;
  }

  @Override public String getInstructionPointer () {
    return instructionPointerName;
  }

  @Deprecated @Override public MemVar getStackRegion () {
    return initialStackRegionId;
  }

  /**
   * An adapter hack to make an RREIL class look like an instruction.
   *
   * @author Bogdan Mihaila
   */
  private static class InstructionWrapper extends Instruction {
    private final int length;
    private final List<RReil> instructions;

    protected InstructionWrapper (int length, List<RReil> instructions) {
      super(instructions.get(0).getRReilAddress(), LowLevelRReil.oneSizeOpcode, instructions.toString(),
          new LowLevelRReilOpnd[0]);
      this.length = length;
      this.instructions = instructions;
    }

    @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
      List<LowLevelRReil> insns = new LinkedList<LowLevelRReil>();
      for (RReil instruction : instructions) {
        insns.add(new RReilHighLevelToLowLevelWrapper(instruction));
      }
      return insns;
    }

    @Override public int length () {
      return length;
    }

  }

}
