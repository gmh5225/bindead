package bindis.x86.common;

import java.util.Iterator;

import bindis.gdsl.GdslInstruction;
import javalx.data.Option;
import javalx.numeric.Interval;
import rreil.disassembler.Instruction;
import rreil.disassembler.OperandInfixIterator;
import rreil.disassembler.OperandTree;
import rreil.disassembler.OperandTree.Node;
import rreil.lang.RReilAddr;
import binparse.Binary;
import binparse.Symbol;

/**
 * A printer for x86 instructions trying to be similar to the objdump pretty printing.
 *
 * @author Bogdan Mihaila
 */
public class X86PrettyPrinter {
  private final Instruction insn;

  public X86PrettyPrinter (Instruction insn) {
    this.insn = insn;
  }

  public static String print (Instruction insn) {
    return new X86PrettyPrinter(insn).getInstruction();
  }

  /**
   * Return the pretty printed instruction string with address, mnemonic and operands.
   */
  public String getInstruction () {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("%-6s", getMnemonic()));
    builder.append(" ");
    builder.append(getOperands(true));
    return builder.toString();
  }

  /**
   * See if the instruction is a call and try to resolve the symbol name for the call target. Note that this is x86
   * specific at the moment and uses instruction mnemonics to find calls.
   *
   * @return The instruction string modified to contain the symbol name of the call/jump target
   */
  public String getRichInstruction (Binary symbolsProvider) {
    String instructionString = getInstruction();
    String mnemonic = getMnemonic();
    String operands = getOperands(false);
    if (mnemonic.matches("(?i)call|ja|jnbe|jae|jnb|jb|jnae|jbe|jna|jc|jcxz|jecxz|je|jz|jg|jnle"
      + "jge|jnl|jl|jnge|jle|jng|jmp|jnc|jne|jnz|jno|jns|jnp|jpo|jo|jp|jpe|js")) { // call or jump
      if (operands.matches("(?i).*(ip|eip|rip).*")) { // relative
        long targetRelativeAddress = 0;
        String[] params = operands.split("[\\+\\-]");
        if (params.length <= 1)
          return instructionString;
        String offset = params[params.length - 1].trim();
        while (offset.startsWith("("))
          offset = offset.substring(1);
        while (offset.endsWith(")"))
          offset = offset.substring(0, offset.length() - 1);
        offset = offset.trim();
        try {
          targetRelativeAddress = Long.parseLong(offset, 10);
          if (operands.contains("-"))
            targetRelativeAddress = -1 * targetRelativeAddress;
        } catch (NumberFormatException e) {
          return instructionString;
        }
        RReilAddr address = insn.address();
        long targetAbsoluteAddress = targetRelativeAddress + address.base() + insn.length();
        String addressString = RReilAddr.valueOf(targetAbsoluteAddress).toShortString();
        Option<Symbol> symbol = symbolsProvider.getSymbol(targetAbsoluteAddress);
        instructionString = instructionString + " # 0x" + addressString;
        if (symbol.isSome())
          instructionString = instructionString + " <" + symbol.get() + ">";
        return instructionString;
      } else { // absolute
        long targetAbsoluteAddress = 0;
        String hexOperands = getOperands(true);
        String[] params = hexOperands.split("0x");
        if (params.length <= 1)
          return instructionString;
        try {
          String absolute = params[params.length - 1];
          absolute = absolute.trim();
          targetAbsoluteAddress = Long.parseLong(absolute, 16);
        } catch (NumberFormatException e) {
          return instructionString;
        }
        Option<Symbol> symbol = symbolsProvider.getSymbol(targetAbsoluteAddress);
        if (symbol.isNone())
          return instructionString;
        return instructionString + " <" + symbol.get() + ">";
      }
    }
    return instructionString;
  }

  public String getAddress () {
    return Long.toHexString(insn.baseAddress());
  }

  public String getOpcode () {
    StringBuilder builder = new StringBuilder();
    insn.opcode(builder);
    return builder.toString();
  }

  public String getMnemonic () {
    return insn.mnemonic();
  }

  public String getOperands (boolean numbersInHex) {
    // workaround for GDSL as the instruction tree approach (operand.toString())
    // returns a different syntax for the operands
    if (insn instanceof GdslInstruction) {
      return ((GdslInstruction) insn).getOperandsString();
    }
    StringBuilder builder = new StringBuilder();
    Iterator<OperandTree> it = insn.operands().iterator();
    while (it.hasNext()) {
      boolean applyLeaHack = insn.mnemonic().toLowerCase().equals("lea");
      builder.append(renderOperand(it.next(), numbersInHex, applyLeaHack));
      if (it.hasNext())
        builder.append(", ");
    }
    return builder.toString();
  }

  private static String renderOperand (OperandTree operand, boolean numbersInHex, boolean isLea) {
    StringBuilder builder = new StringBuilder();
    OperandInfixIterator it = new OperandInfixIterator(operand.getRoot());
    boolean pointer = false;
    int size = 64;
    while (it.next()) {
      Node node = it.current();
      switch (node.getType()) {
      case Immf:
      case Immi:
        builder.append(formatNumber((Number) node.getData(), numbersInHex, size, pointer));
        break;
      case Immr:
        builder.append(((Interval) node.getData()).toString());
        break;
      case Sym:
        builder.append(node.getData().toString().toLowerCase()); // Hack for GDSL
        break;
      case Size:
        size = ((Number) node.getData()).intValue();
        break;
      case Op:
        builder.append(node.getData().toString().toLowerCase().replaceAll("dword ptr", "DWORD PTR")); // Hack for GDSL
        break;
      case Mem:
        if (!isLea)
          builder.append(pointerSizeTranslation((Number) node.getData()));
        builder.append("[");
        pointer = true;
        break;
      }
    }
    if (pointer)
      builder.append("]");
    // this is a hack to avoid "+-" if an offset it negative but added to a register; should be done with a lookahead
    String string = builder.toString().replaceAll("\\+-", "-");
    return string;
  }

  private static String formatNumber (Number number, boolean hex, int size, boolean showSign) {
    String sign = "";
    long longValue = number.longValue();
    if (showSign && longValue < 0) {
      sign = "-";
      longValue = -longValue;
    }
    String numString;
    if (hex) {
      String hexString = Long.toHexString(longValue);
      // cut off negative hex strings (two complement representation) to the right size
      if (hexString.length() > size / 4)
        hexString = hexString.substring(hexString.length() - size / 4);
      numString = sign + "0x" + hexString;
    } else {
      numString = number.toString();
    }
    return numString;
  }

  private static String pointerSizeTranslation (Number size) {
    switch (size.intValue()) {
    case 8:
      return "BYTE PTR ";
    case 16:
      return "WORD PTR ";
    case 32:
      return "DWORD PTR ";
    case 64:
      return "QWORD PTR ";
    default:
      throw new IllegalArgumentException("The size of an x86 instruction cannot be " + size);
    }
  }

}
