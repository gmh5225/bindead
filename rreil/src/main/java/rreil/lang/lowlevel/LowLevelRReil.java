package rreil.lang.lowlevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.util.LowLevelToRReilTranslator;

/**
 * RREIL instruction representation as expression trees. Similar to IDA-Pro and BinNavi instruction-trees.
 */
public class LowLevelRReil extends Instruction {
  /**
   * The opcode is used to know the length of an instruction and thus where to look for the next instruction
   * on a fall-through during the disassembly (i.e. by how much to advance the program counter).
   * As RREIL instructions are by default 1 address apart we implement this using a one-byte opcode array
   */
  public static final byte[] oneSizeOpcode = new byte[] {(byte) 0xff};

  public LowLevelRReil (RReilAddr address, String mnemonic, LowLevelRReilOpnd... ops) {
    super(address, oneSizeOpcode, mnemonic, ops);
  }

  public RReil toRReil () {
    return LowLevelToRReilTranslator.translate(this);
  }

  public LowLevelRReilOpnd rreilOperand (int i) {
    return (LowLevelRReilOpnd) operand(i);
  }

  @Override public List<LowLevelRReil> toRReilInstructions () {
    return new ArrayList<LowLevelRReil>();
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    if (mnemonic().equals(LowLevelRReilFactory.$Brc)) {
      LowLevelRReilOpnd cond = rreilOperand(0);
      LowLevelRReilOpnd target = rreilOperand(1);
      builder.append("if ");
      builder.append(cond.toString());
      if (target.getOffsetOrZero() != 0)
        builder.append(" goto rreil ");
      else
        builder.append(" goto native ");
      builder.append(target);
    } else {
      builder.append(mnemonic());
      Iterator<OperandTree> it = operands().iterator();
      if (it.hasNext())
        builder.append(' ');
      while (it.hasNext()) {
        builder.append(it.next());
        if (it.hasNext())
          builder.append(", ");
      }
    }
    return builder.toString();
  }
}
