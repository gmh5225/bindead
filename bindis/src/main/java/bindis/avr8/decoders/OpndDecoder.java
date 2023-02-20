package bindis.avr8.decoders;

import bindis.DecodeException;
import bindis.avr8.AVRDecodeCtx;
import bindis.avr8.AVRImmOpnd;
import bindis.avr8.AVRMemOpnd;
import bindis.avr8.AVROpnd;
import bindis.avr8.AVRRegOpnd;
import bindis.avr8.AVRRegisterAlterationType;

/**
 * Decoders for AVR instruction operands.
 *
 * @author mb0
 */
public abstract class OpndDecoder {
  public static final OpndDecoder Rd5 = new Rd(5, false, false, false);
  public static final OpndDecoder Rr5 = new Rr(5, false, false, false);
  public static final OpndDecoder RdDv4 = new Rd(4, true, false, false);
  public static final OpndDecoder RrDv4 = new Rr(4, true, false, false);
  public static final OpndDecoder RdUpper4 = new Rd(4, false, true, false);
  public static final OpndDecoder RrUpper4 = new Rr(4, false, true, false);
  public static final OpndDecoder RdUpperHalf3 = new Rd(3, false, false, true);
  public static final OpndDecoder RrUpperHalf3 = new Rr(3, false, false, true);
  // public static final OpndDecoder Rd3 = new Rd(3, false, false, false);
  // public static final OpndDecoder Rr3 = new Rr(3, false, false, false);
  public static final OpndDecoder RdDvUpper2 = new Rd(2, true, true, false);
  public static final OpndDecoder RrDvUpper2 = new Rr(2, true, true, false);
  public static final OpndDecoder K = new ImmediateDecoder(0x00cf0000);
  public static final OpndDecoder KLong = new ImmediateDecoder(0x01f1ffff);
  public static final ImmediateDecoder A = new ImmediateDecoder(0x00f80000);
  public static final OpndDecoder B = new ImmediateDecoder(0x00070000);
  public static final OpndDecoder S = new ImmediateDecoder(0x00700000);
  public static final OpndDecoder Imm0x00f00000 = new ImmediateDecoder(0x00f00000);
  public static final ImmediateDecoder Imm0x0000ffff = new ImmediateDecoder(0x0000ffff);
  public static final OpndDecoder Imm0x0f0f0000 = new ImmediateDecoder(0x0f0f0000);
  public static final OpndDecoder ImmNegRel0x03f80000 = new ImmediateDecoder(0x03f80000, true, true);
  public static final OpndDecoder ImmNegRelAdd0x0fff0000 = new ImmediateDecoder(0x0fff0000, true, true);
  public static final ImmediateDecoder Imm0x2c070000 = new ImmediateDecoder(0x2c070000);
  public static final ImmediateDecoder Imm0x060f0000 = new ImmediateDecoder(0x060f0000);
  public static final OpndDecoder X = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.X));
  public static final OpndDecoder XPlus = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.X), AVRRegisterAlterationType.PostIncrement);
  public static final OpndDecoder MinusX = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.X), AVRRegisterAlterationType.PreDecrement);
  public static final OpndDecoder YPlus = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.Y), AVRRegisterAlterationType.PostIncrement);
  public static final OpndDecoder MinusY = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.Y), AVRRegisterAlterationType.PreDecrement);
  public static final OpndDecoder YDispImm0x2c070000 = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.Y), Imm0x2c070000);
  public static final OpndDecoder Z = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.Z));
  public static final OpndDecoder ZPlus = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.Z), AVRRegisterAlterationType.PostIncrement);
  public static final OpndDecoder MinusZ = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.Z), AVRRegisterAlterationType.PreDecrement);
  public static final OpndDecoder ZDispImm0x2c070000 = new MemoryDecoder(new AVRRegOpnd(AVRRegOpnd.AVRReg.Z), Imm0x2c070000);
  public static final OpndDecoder MemImm0x0000ffff = new MemoryDecoder(Imm0x0000ffff);
  public static final OpndDecoder MemImm0x060f0000 = new MemoryDecoder(Imm0x060f0000);
  public static final OpndDecoder MemA = new MemoryDecoder(A);
  public static final OpndDecoder Invalid = new Invalid();

  public abstract AVROpnd decode (AVRDecodeCtx ctx);

  public abstract boolean isValid ();

  private static final class Rr extends RegisterDecoder {
    private Rr (int size, boolean doubleValue, boolean upper, boolean upperHalf) {
      super(((size == 5 ? 0x0200 : 0x0000) | (((1 << size) - 1) & 0x000f)) << 16, doubleValue, upper, upperHalf);
      if (size > 5)
        throw new RuntimeException("Operand Rr has a maximum size of 5.");
    }
  }

  private static final class Rd extends RegisterDecoder {
    private Rd (int size, boolean doubleValue, boolean upper, boolean upperHalf) {
      super(((1 << size) - 1) << 20, doubleValue, upper, upperHalf);
      if (size > 5)
        throw new RuntimeException("Operand Rd has a maximum size of 5.");
    }
  }

  private static class Unmasker {
    private final int mask;
    private int size = -1;
    private final boolean negative;

    private Unmasker (int mask) {
      this.mask = mask;

      this.negative = false;
    }

    private Unmasker (int mask, boolean negative) {
      this.mask = mask;

      this.negative = negative;
    }

    public int composeValue (AVRDecodeCtx ctx) {
      int insnWord;
      if ((mask & 0x0000ffff) == 0)
        insnWord = ctx.getInsnWord() << 16;
      else
        insnWord = ctx.getInsnWord() << 16 | ctx.getInsnParameterWord();
      int value = 0;
      long currentMask = mask;
      size = 0;
      while (currentMask != 0) {
        int partSize = 0;
        int tailMask = 0;
        for (; (currentMask & 0x00000001) != 0; currentMask >>= 1) {
          partSize++;
          tailMask = (tailMask << 1) | 1;
        }
        value |= (tailMask & insnWord) << size;
        insnWord >>= partSize;
        size += partSize;

        currentMask >>= 1;
        insnWord >>= 1;
      }

      if (negative)
        value |= (value >> (size - 1)) * (~((1 << size) - 1));

      return value;
    }

    public int getSize () {
      if (size < 0)
        throw new RuntimeException("Size not yet calculated.");
      return size;
    }
  }

  private static final class ImmediateDecoder extends OpndDecoder {
    private Unmasker unmasker;
    private boolean relativeAddress;

    private ImmediateDecoder (int mask) {
      this(mask, false);
    }

    private ImmediateDecoder (int mask, boolean negative) {
      this(mask, negative, false);
    }

    private ImmediateDecoder (int mask, boolean negative, boolean relativeAddress) {
      unmasker = new Unmasker(mask, negative);

      this.relativeAddress = relativeAddress;
    }

    @Override public AVRImmOpnd decode (AVRDecodeCtx ctx) {
      int value = unmasker.composeValue(ctx);
      if (relativeAddress)
        value = 2 * value + (int) ctx.getStartPc();
      /*
       * Todo: Unhack
       */
      return new AVRImmOpnd(relativeAddress ? 16 : unmasker.getSize(), value);
    }

    @Override public boolean isValid () {
      return true;
    }
  }

  private static class RegisterDecoder extends OpndDecoder {
    private Unmasker unmasker;
    private boolean doubleValue;
    private boolean upper;
    private boolean upperHalf;

    private RegisterDecoder (int mask, boolean doubleValue, boolean upper, boolean upperHalf) {
      unmasker = new Unmasker(mask);

      this.doubleValue = doubleValue;
      this.upper = upper;
      this.upperHalf = upperHalf;
    }

    @Override public AVRRegOpnd decode (AVRDecodeCtx ctx) {
      int register = unmasker.composeValue(ctx);
      if (doubleValue)
        register *= 2;
      if (upper) {
        int max = 1 << unmasker.getSize();
        if (doubleValue)
          max *= 2;
        register += 32 - max;
      }
      if (upperHalf)
        register += 16;
      return new AVRRegOpnd(8, register);
    }

    @Override public boolean isValid () {
      return true;
    }
  }

  private static class MemoryDecoder extends OpndDecoder {
    AVRRegOpnd memReg;
    AVRRegisterAlterationType altType;
    ImmediateDecoder displacement;

    private MemoryDecoder (AVRRegOpnd memReg) {
      this(memReg, AVRRegisterAlterationType.None);
    }

    private MemoryDecoder (AVRRegOpnd memReg, AVRRegisterAlterationType altType) {
      this(memReg, altType, null);
    }

    private MemoryDecoder (AVRRegOpnd memReg, ImmediateDecoder displacement) {
      this(memReg, AVRRegisterAlterationType.None, displacement);
    }

    private MemoryDecoder (ImmediateDecoder displacement) {
      this(null, AVRRegisterAlterationType.None, displacement);
    }

    private MemoryDecoder (AVRRegOpnd memReg, AVRRegisterAlterationType altType, ImmediateDecoder displacement) {
      this.memReg = memReg;
      this.altType = altType;
      this.displacement = displacement;
    }

    @Override public AVROpnd decode (AVRDecodeCtx ctx) {
      if (displacement != null)
        return new AVRMemOpnd(memReg, altType, displacement.decode(ctx));
      else
        return new AVRMemOpnd(memReg, altType);
    }

    @Override public boolean isValid () {
      return true;
    }
  }

  private static final class Invalid extends OpndDecoder {
    @Override public AVROpnd decode (AVRDecodeCtx ctx) {
      throw DecodeException.inconsistentDecoder(ctx);
    }

    @Override public boolean isValid () {
      return false;
    }
  }
}
