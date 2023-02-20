package rreil.interpreter;

import java.util.HashMap;
import java.util.Map;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rlit;
import rreil.lang.Rhs.Rvar;

/**
 *
 * @author mb0
 */
public class RegisterModel {
  protected final Map<MemVar, BigInt> registers = new HashMap<MemVar, BigInt>();
  protected final int baseSize;

  public RegisterModel (int baseSize) {
    this.baseSize = baseSize;
  }

  public BigInt get (Rvar variable) {
    return fetch(variable.getRegionId(), variable.getSize(), variable.getOffset());
  }

  public BigInt getSigned (Rvar variable) {
    return fetchSigned(variable.getRegionId(), variable.getSize());
  }

  /** Return the unwrapped literal value of {@code literal} */
  public BigInt get (Rlit literal) {
    return getUnsigned(literal);
  }

  public BigInt getUnsigned (Rlit literal) {
    return wrap(literal.getValue(), literal.getSize());
  }

  public BigInt getSigned (Rlit literal) {
    return fetchSigned(literal.getValue(), literal.getSize());
  }

  public BigInt get (RangeRhs range) {
    throw new RReilMachineException("Range values not allowed");
  }

  public BigInt getSigned (RangeRhs range) {
    throw new RReilMachineException("Range values not allowed");
  }

  public void set (Rvar lhs, BigInt unmaskedValue) {
    set(lhs.asLhs(), unmaskedValue);
  }

  public void set (Lhs lhs, BigInt unmaskedValue) {
    final int offset = lhs.getOffset();
    assert baseSize >= lhs.getSize() + offset : "Invalid effective register access size (size + offset > baseSize)";

    final BigInt baseValue = fetchFromPossiblyUndefined(lhs.getRegionId(), baseSize, Bound.ZERO);
    final BigInt value = wrap(unmaskedValue, lhs.getSize());
    final int lowerSz = lhs.getSize() + offset;
    final int upperSz = baseSize - lowerSz;
    final BigInt lower = baseValue.and(sizeMask(offset));
    final BigInt upper = baseValue.and(sizeMask(upperSz).shl(lowerSz));
    final BigInt effectiveValue = upper.or(lower.or(value.shl(offset)));
    registers.put(lhs.getRegionId(), effectiveValue);
  }

  protected BigInt fetchSigned (final MemVar name, int size) {
    return fetchSigned(registers.get(name), size);
  }

  protected BigInt fetchSigned (BigInt unmaskedValue, int size) {
    final BigInt value = wrap(unmaskedValue, size);
    final BigInt msbMask = BigInt.powerOfTwo(size - 1);
    final BigInt msb = value.and(msbMask);
    if (msb.isEqualTo(msbMask))
      return value.not().and(msbMask.sub(Bound.ONE)).add(Bound.ONE).negate();
    else
      return value;
  }

  protected BigInt fetch (final MemVar name, int size, int offset) {
    //assert (baseSize >= size) : "Invalid register access size";
    return wrap(registers.get(name).shr(offset), size);
  }

  protected BigInt fetchFromPossiblyUndefined (final MemVar name, int size, final BigInt undefinedValue) {
    //assert (baseSize >= size) : "Invalid register access size";
    if (!registers.containsKey(name))
      return wrap(undefinedValue, size);
    return wrap(registers.get(name), size);
  }

  protected BigInt wrap (BigInt value, int size) {
    return value.and(sizeMask(size));
  }

  protected BigInt sizeMask (final int size) {
    return BigInt.powerOfTwo(size).sub(Bound.ONE);
  }
}
