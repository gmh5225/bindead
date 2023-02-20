package rreil.interpreter.avr8;

import javalx.numeric.BigInt;
import rreil.interpreter.RegisterModel;
import rreil.lang.Lhs;

/**
 *
 * @author mb0
 */
public class Avr8RegisterModel extends RegisterModel {
  private final int registerRamSize;

  public Avr8RegisterModel (int baseSize, int registerRamSize) {
    super(baseSize);
    this.registerRamSize = registerRamSize;
  }

  @Override public void set (Lhs lhs, BigInt unmaskedValue) {
    int offset = lhs.getOffset();
    assert (registerRamSize >= lhs.getSize() + offset) : "Invalid effective register access size (size + offset > baseSize)";
    BigInt baseValue = fetchFromPossiblyUndefined(lhs.getRegionId(), registerRamSize, BigInt.ZERO);
    BigInt value = wrap(unmaskedValue, lhs.getSize());
    int lowerSz = lhs.getSize() + offset;
    int upperSz = registerRamSize - lowerSz;
    BigInt lower = baseValue.and(sizeMask(offset));
    BigInt upper = baseValue.and(sizeMask(upperSz).shl(lowerSz));
    BigInt effectiveValue = upper.or(lower.or(value.shl(offset)));
    registers.put(lhs.getRegionId(), effectiveValue);
  }
}
