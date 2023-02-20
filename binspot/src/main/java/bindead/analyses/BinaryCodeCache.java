package bindead.analyses;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import javalx.data.Option;
import javalx.numeric.FiniteRange;
import javalx.persistentcollections.tree.FiniteRangeTree;
import javalx.persistentcollections.tree.OverlappingRanges;
import rreil.disassembler.BlockOfInstructions;
import rreil.disassembler.Instruction;
import rreil.disassembler.UnknownInstruction;
import rreil.lang.RReilAddr;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.analyses.warnings.UnknownInstructionWarning;
import bindead.analyses.warnings.WarningsMap;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.exceptions.UnknownCodeAddressException;
import bindis.DecodeCtx;
import bindis.DecodeException;
import bindis.Disassembler;
import bindis.x86.common.X86PrettyPrinter;
import binparse.AbstractBinary;
import binparse.Binary;
import binparse.Segment;
import binparse.Symbol;

/**
 * Caches and manages the disassembled instructions for a binary file.
 *
 * @author Bogdan Mihaila
 */
public class BinaryCodeCache {
  private final Binary binary;
  private final Disassembler disassembler;
  private FiniteRangeTree<Segment> segments = FiniteRangeTree.empty();
  private final SortedMap<Long, Instruction> instructions = new TreeMap<>();
  private final SortedMap<Long, BlockOfInstructions> blocks = new TreeMap<>();
  private final Map<Long, String> symbolsCache = new HashMap<Long, String>();
  private final boolean SKIPUNKNOWNINSNS = AnalysisProperties.INSTANCE.skipDisassembleErrors.isTrue();
  private final boolean DISASSEMBLEBLOCKS = AnalysisProperties.INSTANCE.disassembleBlockWise.isTrue();
  private WarningsMap warningsMap;

  public BinaryCodeCache (Binary binary, Disassembler dis, WarningsMap warningsMap) {
    this.binary = binary;
    this.disassembler = dis;
    this.warningsMap = warningsMap;
    loadCodeSegments();
  }

  public Binary getBinary () {
    return binary;
  }

  public int instructionsCount () {
    return instructions.size();
  }

  /**
   * Returns the instructions list sorted by their addresses.
   */
  public SortedMap<Long, Instruction> getInstructions () {
    return instructions;
  }

  public Instruction getInstruction (long nativeAddress) {
    if (!hasInstruction(nativeAddress))
      throw new IllegalArgumentException("Instruction for address " + nativeAddress + " not found.");
    return instructions.get(nativeAddress);
  }

  public boolean hasInstruction (long nativeAddress) {
    if (DISASSEMBLEBLOCKS)
      return blocks.containsKey(nativeAddress);
    else
      return instructions.containsKey(nativeAddress);
  }

  private void addWarning (long nativeAddress, WarningMessage warning) {
    ProgramAddress location = new ProgramAddress(RReilAddr.valueOf(nativeAddress));
    WarningsContainer warnings = warningsMap.get(location);
    warnings.addWarning(warning);
    warningsMap.put(location, 0, warnings);
  }

  /**
   * Decode an instruction at the given address {@code nativeAddress} and add it to {@code this}.
   *
   * @param nativeAddress The code address.
   * @return The instruction at the given address.
   */
  public Instruction decodeInstruction (long nativeAddress) {
    Segment segment = findSegment(nativeAddress);
    Instruction insn;
    try {
      insn = disassembler.decodeOne(segment.getData(), (int) (nativeAddress - segment.getAddress()), nativeAddress);
    } catch (DecodeException e) {
      if (SKIPUNKNOWNINSNS) {
        DecodeCtx ctx = e.getErrCtx().getDecodeCtx();
        insn = new UnknownInstruction(ctx.getStartPc(), ctx.slice());
        addWarning(nativeAddress, new UnknownInstructionWarning(insn));
      } else {
        throw e;
      }
    }
    instructions.put(nativeAddress, insn);
    return insn;
  }

  /**
   * Decode a block of instructions at the given address {@code nativeAddress} and add it to {@code this}.
   *
   * @param nativeAddress The code address.
   * @return The instructions block at the given address.
   */
  public BlockOfInstructions decodeBlock (long nativeAddress) {
    Segment segment = findSegment(nativeAddress);
    BlockOfInstructions block =
      disassembler.decodeBlock(segment.getData(), (int) (nativeAddress - segment.getAddress()), nativeAddress);
    blocks.put(nativeAddress, block);
    for (Instruction insn : block.getInstructions()) {
      instructions.put(insn.baseAddress(), insn);
    }
    return block;
  }

  /**
   * Returns the address that follows the instruction or instruction block at the given address.
   * The returned address is the one to be used for the next disassembling attempt.
   * @see #decodeInstruction(long)
   * @see #decodeBlock(long)
   */
  public long getNextDisassemblyAddress (long nativeAddress) {
    BlockOfInstructions block = blocks.get(nativeAddress);
    if (block != null)
      return nativeAddress + block.byteLength();
    Instruction insn = instructions.get(nativeAddress);
    if (insn != null)
      return nativeAddress + insn.length();
    throw new IllegalArgumentException("Instruction for address " + nativeAddress + " not found.");
  }

  private Segment findSegment (long nativeAddress) throws UnknownCodeAddressException {
    OverlappingRanges<Segment> overlapping = segments.searchOverlaps(FiniteRange.of(nativeAddress));
    switch (overlapping.size()) {
    case 0:
      throw new UnknownCodeAddressException();
    case 1:
      return overlapping.getFirst()._2();
    default:
      throw new UnknownCodeAddressException();
    }
  }

  private void loadCodeSegments () {
    for (Segment s : binary.getSegments()) {
      if (s.getSize() <= 0)
        continue;
      FiniteRange key = intervalKey(s);
      segments = segments.bind(key, s);
    }
  }

  private static FiniteRange intervalKey (Segment s) {
    long size = s.getSize();
    long address = s.getAddress();
    assert size > 0 : "invalid Segment size";
    return FiniteRange.of(address, address + size - 1);
  }

  @Override public String toString () {
    return toDisassemblyString();
  }

  /**
   * Pretty print this code cache as disassembler output, in a style similar to objdump.
   */
  public String toDisassemblyString () {
    Long endOfPreviousInstruction = null;
    StringBuilder builder = new StringBuilder();
    for (Entry<Long, Instruction> entry : getInstructions().entrySet()) {
      Long address = entry.getKey();
      Instruction instruction = entry.getValue();
      // if there is a gap between the last instruction and this one then some bytes are not in the disassembly
      if (endOfPreviousInstruction != null && !endOfPreviousInstruction.equals(address)) {
        long gapLength = address - endOfPreviousInstruction;
        builder.append("...........");
        builder.append("(" + gapLength + " bytes)\n");
      }
      endOfPreviousInstruction = address + instruction.length();
      Option<Symbol> symbol = binary.getSymbol(address);
      if (symbol.isSome())
        builder.append("\n" + symbol.get() + ":\n");
      String addressString = String.format("%08x", address);
      // assuming an 11 byte opcode as the most common maximum length. In hex and with whitespace it takes up 33 chars
      String opcode = String.format("%-33s", instruction.opcode(new StringBuilder()).toString());
      builder.append(addressString + ": ");
      builder.append(" " + opcode + " ");
      builder.append(toRichInstructionString(instruction));
      builder.append("\n");
    }
    return builder.toString();
  }

  /**
   * Pretty print an instruction with the symbol name for calls if available.
   */
  public String toRichInstructionString (Instruction instruction) {
    return appendSymbolForJumpsCalls(instruction);
  }

  /**
   * See if the instruction is a call and try to resolve the symbol name for the call target. Note that this is x86
   * specific at the moment and uses instruction mnemonics to find calls.
   *
   * @param instruction
   * @return The instruction string modified to contain the symbol name of the call target
   */
  private String appendSymbolForJumpsCalls (Instruction instruction) {
    String architecture = binary.getArchitectureName();
    // currently only implemented for x86
    if (architecture.equals(AbstractBinary.x86_32) || architecture.equals(AbstractBinary.x86_64)) {
      X86PrettyPrinter printer = new X86PrettyPrinter(instruction);
      return printer.getRichInstruction(binary);
    }
    return instruction.toString();
  }

  /**
   * Return the name of the function the instruction belongs to or a special string if no function could be found.<br>
   * As only function beginnings are associated with symbol names we perform a linear search to lower
   * addresses to find the nearest function beginning that has a symbol name. Hence we may return the wrong symbol
   * in some cases where some functions are not associated with symbols or the binary does not contain symbols at all,
   * i.e. a stripped binary.
   */
  public String getEnclosingFunction (Instruction instruction) {
    Long address = instruction.baseAddress();
    String functionName = symbolsCache.get(address);
    if (functionName == null) {
      // try to see if the instruction is the beginning of a function
      Option<Symbol> functionSymbol = binary.getSymbol(address);
      if (functionSymbol.isSome()) {
        functionName = functionSymbol.get().toString();
      } else {
        // instruction is part of the body of a function, look for last function beginning
        Long previousInstructionAddress = address;
        while (true) {
          try {
            previousInstructionAddress = instructions.headMap(previousInstructionAddress).lastKey();
          } catch (IllegalArgumentException _) {
            // reached the beginning of the map without finding a symbol
            functionName = "<unkn>";
            break;
          } catch (NoSuchElementException _) {
            // map is empty
            functionName = "<unkn>";
            break;
          }
          functionSymbol = binary.getSymbol(previousInstructionAddress);
          if (functionSymbol.isSome()) {
            functionName = functionSymbol.get().toString();
            break;
          }
        }
      }
      symbolsCache.put(address, functionName);
    }
    return functionName;
  }

}