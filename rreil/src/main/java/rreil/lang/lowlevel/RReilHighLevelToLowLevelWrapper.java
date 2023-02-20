package rreil.lang.lowlevel;

import rreil.lang.RReil;

/**
 * An adapter class to make an high level RREIL instruction look like a low level RREIL instruction.
 * This is not perfect though as the operands are not carried over. However, we are only interested
 * to use this wrapper to later call {@link #toRReil()} to retrieve the high level instruction anyway.
 *
 * @author Bogdan Mihaila
 */
public class RReilHighLevelToLowLevelWrapper extends LowLevelRReil {
  private final RReil instruction;

  public RReilHighLevelToLowLevelWrapper (RReil instruction) {
    super(instruction.getRReilAddress(), instruction.mnemonic(), new LowLevelRReilOpnd[0]);
    this.instruction = instruction;
  }

  @Override public RReil toRReil () {
    return instruction;
  }

  @Override public String toString () {
    return instruction.toString();
  }
}
