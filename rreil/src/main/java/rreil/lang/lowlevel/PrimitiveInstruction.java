package rreil.lang.lowlevel;

import java.util.Collections;
import java.util.List;

import rreil.disassembler.OperandTree;
import rreil.lang.RReilAddr;

public class PrimitiveInstruction extends LowLevelRReil {
  private final List<LowLevelRReilOpnd> inArgs;
  private final List<LowLevelRReilOpnd> outArgs;


  public PrimitiveInstruction (RReilAddr address, String mnemonic, List<LowLevelRReilOpnd> inArgs, List<LowLevelRReilOpnd> outArgs) {
    super(address, mnemonic);
    this.inArgs = Collections.unmodifiableList(inArgs);
    this.outArgs = Collections.unmodifiableList(outArgs);
  }

  public List<LowLevelRReilOpnd> getInArgs() {
    return inArgs;
  }

  public List<LowLevelRReilOpnd> getOutArgs () {
    return outArgs;
  }


  /**
   * Render this instruction's mnemonic and operands to {@code buf}.
   *
   * @param buf The string-builder to pretty print into.
   * @return The updated string-builder.
   */
  @Override public StringBuilder asString (StringBuilder buf) {
    for (OperandTree t : outArgs) {
      t.getRoot().asString(buf).append(' ');
    }
    buf.append("= ").append(mnemonic()).append(' ');
    for (OperandTree t : inArgs) {
      t.getRoot().asString(buf).append(' ');
    }
    return buf;
  }
}
