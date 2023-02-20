package rreil.lang.lowlevel;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class TranslationHelpers {
  static final LowLevelRReilFactory rreil = LowLevelRReilFactory.getInstance();
  public static final Map<String, OperandSize> architectures = new HashMap<String, OperandSize>();

  static {
    architectures.put("X86-32", OperandSize.DWORD);
    architectures.put("X86-64", OperandSize.QWORD);
    architectures.put("ARM-32", OperandSize.DWORD);
    architectures.put("POWERPC-32", OperandSize.DWORD);
    architectures.put("REIL", OperandSize.DWORD);
    architectures.put("MIPS-32", OperandSize.DWORD);
    architectures.put("AVR", OperandSize.BYTE);
    architectures.put("AVR32", OperandSize.DWORD);
  }

  /**
   * Finds the next biggest operand size for a given operand size.
   *
   * @param size The smaller operand size
   * @return The next bigger operand siz
   */
  public static OperandSize getNextSize (final OperandSize size) {
    switch (size) {
      case BYTE:
        return OperandSize.WORD;
      case WORD:
        return OperandSize.DWORD;
      case DWORD:
        return OperandSize.QWORD;
      case QWORD:
        return OperandSize.OWORD;
    default:
      throw new IllegalArgumentException("Error: Invalid argument size");
    }
  }

  public static int getNextSize (final int size) {
    switch (size) {
      case 8:
        return 16;
      case 16:
        return 32;
      case 32:
        return 64;
      case 64:
        return 128;
    }

    throw new IllegalArgumentException("Error: Invalid argument size");
  }

  /**
   * Returns a mask that masks all bits of values of a given
   * operand size.
   *
   * @param size The given operand size
   * @return A mask where all bits are set
   */
  public static long getTruncateMask (final OperandSize size) {
    switch (size) {
      case BYTE:
        return 255l;
      case WORD:
        return 65535l;
      case DWORD:
        return 4294967295l;
      case QWORD:
        return 0xFFFFFFFFFFFFFFFFl;
    default:
      throw new IllegalArgumentException("Error: Invalid argument size");
    }
  }

  public static LowLevelRReilOpnd getMaxImmediate (final int size) {
    return rreil.immediate(size, BigInteger.valueOf(2).pow(size).subtract(BigInteger.ONE));
  }

  public static LowLevelRReilOpnd getMaxImmediateSigned (final int size) {
    return rreil.immediate(size, BigInteger.valueOf(2).pow(size - 1).subtract(BigInteger.ONE));
  }

  public static LowLevelRReilOpnd getMinImmediateSigned (final int size) {
    return rreil.immediate(size, BigInteger.valueOf(2).pow(size - 1).multiply(BigInteger.ZERO.subtract(BigInteger.ONE)));
  }

  public static OperandSize fromBitsOverapproximate (final int bits) {
    if (bits == 1)
      return TranslationHelpers.fromBits(1);
    else if (bits <= 8)
      return TranslationHelpers.fromBits(8);
    else if (bits <= 16)
      return TranslationHelpers.fromBits(16);
    else if (bits <= 32)
      return TranslationHelpers.fromBits(32);
    else if (bits <= 64)
      return TranslationHelpers.fromBits(64);
    else if (bits <= 128)
      return TranslationHelpers.fromBits(128);
    throw new IllegalStateException("Error: Invalid bitcount: '" + String.valueOf(bits) + "'");

  }

  public static OperandSize fromBits (final int bits) {
    switch (bits) {
      case 1:
        return OperandSize.BIT;
      case 8:
        return OperandSize.BYTE;
      case 16:
        return OperandSize.WORD;
      case 32:
        return OperandSize.DWORD;
      case 64:
        return OperandSize.QWORD;
      case 128:
        return OperandSize.OWORD;
      default:
        throw new IllegalStateException("Error: Invalid bitcount: '" + String.valueOf(bits) + "'");
    }
  }
}
