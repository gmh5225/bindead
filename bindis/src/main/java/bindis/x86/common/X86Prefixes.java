package bindis.x86.common;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bindis.DecodeStream;

/**
 * X86 instruction prefix decoder for both 32bit and 64bit modes.
 *
 * @author mb0
 */
public final class X86Prefixes {
  private static final Map<Integer, Prefix> ords = new HashMap<Integer, Prefix>();

  static {
    for (Prefix p : Prefix.values()) {
      ords.put(p.ord, p);
    }
  }
  private final EnumSet<Prefix> prefixes = EnumSet.noneOf(Prefix.class);

  private X86Prefixes () {
  }

  public boolean hasOpcodePrefix66 () {
    return prefixes.contains(Prefix.OPNDSZ);
  }

  public X86Prefixes discardOpcodePrefix66 () {
    prefixes.remove(Prefix.OPNDSZ);
    return this;
  }

  public boolean hasOpcodePrefixF2 () {
    return prefixes.contains(Prefix.REPNZ);
  }

  public X86Prefixes discardOpcodePrefixF2 () {
    prefixes.remove(Prefix.REPNZ);
    return this;
  }

  public boolean hasOpcodePrefixF3 () {
    return prefixes.contains(Prefix.REPZ);
  }

  public X86Prefixes discardOpcodePrefixF3 () {
    prefixes.remove(Prefix.REPZ);
    return this;
  }

  public boolean contains (final Prefix p) {
    return prefixes.contains(p);
  }

  public static X86Prefixes decode64 (final DecodeStream in) {
    final X86Prefixes actual = new X86Prefixes();
    for (;;) {
      int value = in.peek8();
      Prefix prefix = ords.get(value);
      if (prefix == null || prefix.isRexPrefix())
        break;
      in.read8();
      actual.prefixes.add(ords.get(value));
    }
    int value = in.peek8();
    boolean popByte = false;
    if (value > 0x4f || value < Prefix.REXB.ord())
      return actual;
    if (isRexPrefix(value, Prefix.REXW.ord())) {
      actual.prefixes.add(Prefix.REXW);
      popByte = true;
    }
    if (isRexPrefix(value, Prefix.REXX.ord())) {
      actual.prefixes.add(Prefix.REXX);
      popByte = true;
    }
    if (isRexPrefix(value, Prefix.REXB.ord())) {
      actual.prefixes.add(Prefix.REXB);
      popByte = true;
    }
    if (isRexPrefix(value, Prefix.REXR.ord())) {
      actual.prefixes.add(Prefix.REXR);
      popByte = true;
    }
    if (popByte)
      in.read8();
    return actual;
  }

  private static boolean isRexPrefix (int value, int ord) {
    return (value & ord) == ord;
  }

  public static X86Prefixes decode32 (final DecodeStream in) {
    final X86Prefixes actual = new X86Prefixes();
    for (;;) {
      int value = in.peek8();
      Prefix prefix = ords.get(value);
      if (prefix == null || prefix.isRexPrefix())
        break;
      in.read8();
      actual.prefixes.add(ords.get(value));
    }
    return actual;
  }

  public boolean isEmpty () {
    return prefixes.isEmpty();
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder("{");
    Iterator<Prefix> it = prefixes.iterator();
    while (it.hasNext()) {
      Prefix prefix = it.next();
      builder.append(prefix);
      if (it.hasNext())
        builder.append(", ");
    }
    builder.append('}');
    return builder.toString();
  }

  public static enum Prefix {
    OPNDSZ(0x66),
    REPNZ(0xf2),
    REPZ(0xf3),
    LOCK(0xf0),
    ADDRSZ(0x67),
    //FWAIT(0x9b),
    CS(0x2e),
    SS(0x36),
    DS(0x3e),
    ES(0x26),
    FS(0x64),
    GS(0x65),
    REXW(0x48),
    REXR(0x44),
    REXX(0x42),
    REXB(0x41);
    private final int ord;

    Prefix (final int ord) {
      this.ord = ord;
    }

    public int ord () {
      return ord;
    }

    public boolean isRexPrefix () {
      return this == REXW || this == REXR || this == REXX || this == REXB;
    }
  }
}
