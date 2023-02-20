package rreil.interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javalx.numeric.BigInt;
import javalx.numeric.Bound;



/**
 *
 * @author mb0
 */
public final class MemoryModel {
  private static final BigInt MASK64 = BigInt.powerOfTwo(64).sub(Bound.ONE);
  public static final long DEFAULT_BASE_ADDRESS = 0xFFFF;
  public static final int DEFAULT_SIZE = 100;
  private final ByteBuffer data;
  private final long baseAddress;

  public MemoryModel (final byte[] segment, final long baseAddress, final ByteOrder ord) {
    data = ByteBuffer.wrap(segment);
    this.baseAddress = baseAddress;
    data.order(ord);
  }

  public MemoryModel (final ByteOrder ord) {
    this(new byte[DEFAULT_SIZE], DEFAULT_BASE_ADDRESS, ord);
  }

  public BigInt load (final int size, final long address) {
    switch (size) {
      case 8: {
        final int value = data.get((int) (address - baseAddress)) & 0xff;
        return BigInt.of(value);
      }
      case 16: {
        final int value = data.getShort((int) (address - baseAddress)) & 0xffff;
        return BigInt.of(value);
      }
      case 32: {
        final long value = data.getInt((int) (address - baseAddress)) & 0xffffffffl;
        return BigInt.of(value);
      }
      case 64: {
        final long value = data.getLong((int) (address - baseAddress));
        return BigInt.of(value).and(MASK64);
      }
      default:
        throw new RReilMachineException("Invalid memory access size :" + size);
    }
  }

  public void store (final int size, final long address, BigInt value) {
    switch (size) {
      case 8: {
        data.put((int) (address - baseAddress), value.getValue().byteValue());
        break;
      }
      case 16: {
        data.putShort((int) (address - baseAddress), value.getValue().shortValue());
        break;
      }
      case 32: {
        data.putInt((int) (address - baseAddress), value.intValue());
        break;
      }
      case 64: {
        data.putLong((int) (address - baseAddress), value.longValue());
        break;
      }
      default:
        throw new RReilMachineException("Invalid memory access size :" + size);
    }
  }
}
