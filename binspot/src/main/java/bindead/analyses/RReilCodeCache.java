package bindead.analyses;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import javalx.data.Option;
import javalx.numeric.BigInt;
import rreil.lang.AssemblerParseable;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.LinRval;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rval;
import bindead.debug.StringHelpers;
import binparse.Binary;
import binparse.Symbol;

/**
 * Caches and manages the RREIL instructions for an analysis.
 *
 * @author Bogdan Mihaila
 */
public class RReilCodeCache implements AssemblerParseable {
  private final NavigableMap<RReilAddr, RReil> instructions = new TreeMap<RReilAddr, RReil>();
  private final Option<Binary> binary;

  public RReilCodeCache () {
    this(null);
  }

  public RReilCodeCache (Binary binary) {
    this.binary = Option.fromNullable(binary);
  }

  /**
   * Returns the instructions list sorted by their addresses.
   */
  public SortedMap<RReilAddr, RReil> getInstructions () {
    return instructions;
  }

  public RReil getInstruction (RReilAddr address) {
    if (!hasInstruction(address))
      throw new IllegalArgumentException("Instruction for address " + address + " not found.");
    return instructions.get(address);
  }

  public boolean hasInstruction (RReilAddr address) {
    return instructions.containsKey(address);
  }

  /**
   * Return the address of the next instruction with same base or native address
   * in this code cache or {@code none} if the
   * passed in address is the last instruction.
   */
  public Option<RReilAddr> getNextInstructionAddressWithSameBase (RReilAddr address) {
    RReilAddr nextInstruction = instructions.higherKey(address);
    if (nextInstruction != null && nextInstruction.base() == address.base())
      return Option.some(nextInstruction);
    else
      return Option.none();
  }

  public void addInstruction (RReil insn) {
    instructions.put(insn.getRReilAddress(), insn);
  }

  public int instructionsCount () {
    return instructions.size();
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    for (Entry<RReilAddr, RReil> entry : instructions.entrySet()) {
      RReilAddr address = entry.getKey();
      RReil instruction = entry.getValue();
      if (address.offset() == 0) {
        String label = getLabel(instruction.getRReilAddress());
        if (!label.isEmpty())
          builder.append(label + ":\n");
      }
      String instructionString = toRichInstructionString(instruction);
      builder.append(address + ": " + instructionString + "\n");
    }
    return builder.toString();
  }

  /**
   * Pretty print this code cache with a separator between groups of RREIL
   * instructions that comprise a native instruction.
   */
  public String toDisassemblyString () {
    boolean showBlockSeparator = haveSubAddresses();
    StringBuilder builder = new StringBuilder();
    for (Entry<RReilAddr, RReil> entry : instructions.entrySet()) {
      RReilAddr address = entry.getKey();
      RReil instruction = entry.getValue();
      String insnString = address + ":  " + toRichInstructionString(instruction);
      if (address.offset() == 0) {
        String currentLabel = getLabel(address);
        if (!currentLabel.isEmpty())
          builder.append("\n" + currentLabel + ":\n");
        if (showBlockSeparator)
          builder.append(StringHelpers.repeatString("-", insnString.length()) + "\n");
      }
      builder.append(insnString + "\n");
    }
    return builder.toString();
  }

  private boolean haveSubAddresses () {
    for (RReilAddr address : instructions.keySet()) {
      if (address.offset() != 0)
        return true;
    }
    return false;
  }

  /**
   * Prints the code cache as Java String formatted to be parsed by the disassembler.
   */
  @Override public String toAssemblerString () {
    StringBuilder builder = new StringBuilder();
    for (RReil instruction : instructions.values()) {
      if (instruction.getRReilAddress().base() == 0) {
        builder.append("\"");
        builder.append(" // -- next native instruction --");
        builder.append("\",\n");
      }
      builder.append("\"");
      builder.append(instruction.toAssemblerString());
      builder.append("\",\n");
    }
    return builder.toString();
  }

  /**
   * Pretty print an instruction with the label name for the target of branches if available.
   */
  public String toRichInstructionString (RReil instruction) {
    return appendLabelForJumps(instruction);
  }

  private static Option<RReilAddr> toLiteralAddress (Lin target) {
    if (target instanceof LinRval) {
      Rval rval = ((LinRval) target).getRval();
      if (rval instanceof Rlit) {
        BigInt value = ((Rlit) rval).getValue();
        return Option.some(RReilAddr.valueOf(value));
      }
    }
    return Option.none();
  }

  private String appendLabelForJumps (RReil instruction) {
    if (instruction.isBranch() && !instruction.isIndirectBranch()) {
      if (instruction instanceof RReil.BranchToNative) {
        RReil.BranchToNative branch = (RReil.BranchToNative) instruction;
        Option<RReilAddr> target = toLiteralAddress(branch.getTarget());
        if (target.isSome()) {
          String targetLabel = getLabel(target.get());
          if (!targetLabel.isEmpty())
            return instruction + " <" + targetLabel + ">";
        }
      } else if (instruction instanceof RReil.Branch) {
        RReil.Branch branch = (RReil.Branch) instruction;
        Option<RReilAddr> target = toLiteralAddress(branch.getTarget());
        if (target.isSome()) {
          String targetLabel = getLabel(target.get());
          if (!targetLabel.isEmpty())
            return instruction + " <" + targetLabel + ">";
        }
      }
    }
    return instruction.toString();
  }

  public String getLabel (RReilAddr address) {
    if (binary.isNone())
      return "";
    Option<Symbol> symbol = binary.get().getSymbol(address.base());
    if (symbol.isNone())
      return "";
    return symbol.get().getName().getOrElse("");
  }

  public Option<Long> getAddressForLabel (String label) {
    if (binary.isNone())
      return Option.none();
    Option<Symbol> symbol = binary.get().getSymbol(label);
    if (symbol.isNone())
      return Option.none();
    return Option.some(symbol.get().getAddress());
  }

}
