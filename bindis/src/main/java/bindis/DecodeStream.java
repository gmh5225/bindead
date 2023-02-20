package bindis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author mb0
 */
public final class DecodeStream {
  private final ByteBuffer data;
  private int idx;
  private int mark;

  public DecodeStream (byte[] data, int idx) {
    this.data = ByteBuffer.allocateDirect(data.length);
    this.data.mark();
    this.data.put(data);

    this.idx = idx;
    mark = idx;
    this.data.order(ByteOrder.LITTLE_ENDIAN);
  }

  public DecodeStream (byte[] data) {
    this(data, 0);
  }

  public void mark () {
    mark = idx;
  }

  public int previous8 () {
    return get(idx - 1) & 0xff;
  }

  public int consumed () {
    assert (idx >= mark) : "inconsistent mark and index";
    return idx - mark;
  }

  public int available () {
    return data.limit() - idx;
  }

  public int getIdx () {
    return idx;
  }

  public int getMark () {
    return mark;
  }

  public void resetToMark () {
    assert (mark >= 0) : "invalid mark";
    idx = mark;
  }

  public int read8 () {
    int value = peek8();
    idx++;
    return value;
  }

  public byte[] read(int length) {
    byte[] bytes = new byte[length];
    data.position(idx);
    data.get(bytes, 0, length);
    idx += length;
    return bytes;
  }

  public int peek8 () {
    return get(idx) & 0xff;
  }

  public byte raw8 () {
    byte value = get(idx);
    idx++;
    return value;
  }

  public short raw16 () {
    short value = getShort(idx);
    idx += 2;
    return value;
  }

  public int raw32 () {
    int value = getInt(idx);
    idx += 4;
    return value;
  }

  public long raw64 () {
    long value = getLong(idx);
    idx += 8;
    return value;
  }

  public long read63 () {
    return raw64();
  }

  public int read16 () {
    return raw16() & 0xffff;
  }

  public int peek16 () {
    return getShort(idx) & 0xffff;
  }

  public int read32 () {
    int value = getInt(idx);
    idx += 4;
    return value;
  }

  public int peek32 () {
    return getInt(idx);
  }

  public void order (ByteOrder ord) {
    data.order(ord);
  }

  public ByteBuffer getBuffer() {
    return data;
  }

  public byte[] slice () {
    int length = idx - mark;
    assert length > 0 : "Invalid slice length";
    byte[] slice = new byte[length];
    for (int i = 0; i < slice.length; i++)
      slice[i] = data.get(mark + i);
    return slice;
  }

  /**
   * Returns a string summarizing the state of this stream.
   *
   * @return A summary string
   */
  @Override public String toString () {
    StringBuilder sb = new StringBuilder();
    sb.append("[pos=");
    sb.append(getIdx());
    sb.append(" lim=");
    sb.append(data.limit());
    sb.append("]");
    return sb.toString();
  }

  /**
   * Wrapper method to access the underlying data but with a better exception handling.
   */
  private byte get (int index) {
    try {
      return data.get(index);
    } catch (java.lang.IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException(index);
    }
  }

  /**
   * Wrapper method to access the underlying data but with a better exception handling.
   */
  private short getShort (int index) {
    try {
      return data.getShort(index);
    } catch (java.lang.IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException(index);
    }
  }

  /**
   * Wrapper method to access the underlying data but with a better exception handling.
   */
  private int getInt (int index) {
    try {
      return data.getInt(index);
    } catch (java.lang.IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException(index);
    }
  }

  /**
   * Wrapper method to access the underlying data but with a better exception handling.
   */
  private long getLong (int index) {
    try {
      return data.getLong(index);
    } catch (java.lang.IndexOutOfBoundsException e) {
      throw new IndexOutOfBoundsException(index);
    }
  }
}
