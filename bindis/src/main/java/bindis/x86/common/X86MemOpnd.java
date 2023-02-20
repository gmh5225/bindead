package bindis.x86.common;

import bindis.OperandKind;

/**
 * X86 memory access operands.
 *
 * @author mb0
 */
public class X86MemOpnd extends X86Operand {
  protected final byte scale;
  protected final X86RegOpnd index;
  protected final X86RegOpnd base;
  protected final X86ImmOpnd disp;
  protected final X86ImmOpnd seg;
  protected final int ptrSize;

  private X86MemOpnd (int size, int ptrSize, byte scale, X86RegOpnd index, X86RegOpnd base, X86ImmOpnd disp, X86ImmOpnd seg) {
    super(size, OperandKind.MEM);
    this.scale = scale;
    this.index = index;
    this.base = base;
    this.disp = disp;
    this.seg = seg;
    this.ptrSize = ptrSize;
  }

  public X86MemOpnd (int size, int ptrSize, byte scale, X86RegOpnd index, X86RegOpnd base, X86ImmOpnd disp) {
    this(size, ptrSize, scale, index, base, disp, null);
  }

  public X86MemOpnd (int size, int ptrSize, X86ImmOpnd seg, X86ImmOpnd disp) {
    this(size, ptrSize, (byte) 0, null, null, disp, seg);
  }

  public X86RegOpnd base () {
    return base;
  }

  public X86ImmOpnd disp () {
    return disp;
  }

  public X86RegOpnd index () {
    return index;
  }

  public byte scale () {
    return scale;
  }

  public int ptrSize () {
    return ptrSize;
  }

  @Override public StringBuilder asString (StringBuilder buf) {
    buf.append(size);
    buf.append('[');
    if (base != null)
      base.asString(buf);
    if (index != null) {
      if (base != null)
        buf.append('+');
      index.asString(buf);
      if (scale > 1) {
        buf.append('*');
        buf.append(scale);
      }
    }
    if (disp != null)
      if (index != null || base != null) {
        if (disp.imm.intValue() != 0) {
          buf.append('+');
          disp.asString(buf);
        }
      } else {
        disp.asString(buf);
      }
    return buf.append("]:").append(ptrSize);
  }

  @Override public <R, T> R accept (X86OperandVisitor< R, T> visitor, T data) {
    return visitor.visit(this, data);
  }

  public static X86MemOpnd mk (int dataSize, X86RegOpnd base) {
    return new X86MemOpnd(dataSize, base.size(), (byte) -1, null, base, null);
  }

  public static X86MemOpnd mk (int dataSize, X86RegOpnd base, X86RegOpnd index, byte scale) {
    return new X86MemOpnd(dataSize, base.size(), scale, index, base, null);
  }

  public static X86MemOpnd mk (int dataSize, X86RegOpnd base, X86RegOpnd index, byte scale, X86ImmOpnd disp) {
    return new X86MemOpnd(dataSize, base.size(), scale, index, base, disp);
  }

  public static X86MemOpnd mk (int dataSize, X86RegOpnd base, X86ImmOpnd disp) {
    return new X86MemOpnd(dataSize, base.size(), (byte) -1, null, base, disp);
  }

  public static X86MemOpnd mk (int dataSize, X86ImmOpnd disp) {
    return new X86MemOpnd(dataSize, disp.size(), (byte) -1, null, null, disp);
  }

  public static X86MemOpnd mk (int dataSize, X86RegOpnd index, byte scale, X86ImmOpnd disp) {
    return new X86MemOpnd(dataSize, index.size(), scale, index, null, disp);
  }

  public static X86MemOpnd mk (int dataSize, X86ImmOpnd seg, X86ImmOpnd offs) {
    return new X86MemOpnd(dataSize, seg.size(), (byte) -1, null, null, offs, seg);
  }
}
