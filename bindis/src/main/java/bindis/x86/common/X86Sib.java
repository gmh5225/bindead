package bindis.x86.common;

import bindis.DecodeStream;

/**
 * Scale-index-base operand format decode helper.
 *
 * @see {@link bindis.x86.common.ModRM}
 * @author mb0
 */
public final class X86Sib {
  public static final int Sib_INDEX_OFFSET = 3;
  public static final int Sib_INDEX_MASK = 7;
  public static final int Sib_BASE_MASK = 7;
  public static final int Sib_SCALE_OFFSET = 6;
  public static final int Sib_SCALE_MASK = 3;
  private final int sib;

  public X86Sib (int sib) {
    this.sib = sib;
  }

  public int getIndex () {
    return (sib >> Sib_INDEX_OFFSET) & Sib_INDEX_MASK;
  }

  public int getBase () {
    return sib & Sib_BASE_MASK;
  }

  public int getScale () {
    return 1 << ((sib >> Sib_SCALE_OFFSET) & Sib_SCALE_MASK);
  }

  public static X86Sib decode (final DecodeStream in) {
    return new X86Sib(in.read8());
  }
}
