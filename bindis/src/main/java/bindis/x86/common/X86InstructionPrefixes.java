package bindis.x86.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import bindis.DecodeStream;

/**
 * X86 instruction prefix decoder for both 32bit and 64bit modes.
 *
 * @author mb0
 */
public class X86InstructionPrefixes {
  private static final Map<Integer, Prefix> ords = new HashMap<Integer, Prefix>();
  private static final int $OPNDSZ = 0x66;
  private static final int $REPNZ = 0xf2;
  private static final int $REPZ = 0xf3;
  private static final int $LOCK = 0xf0;
  private static final int $ADDRSZ = 0x67;
  private static final int $FWAIT = 0x9b;
  private static final int $CS = 0x2e;
  private static final int $SS = 0x36;
  private static final int $DS = 0x3e;
  private static final int $ES = 0x26;
  private static final int $FS = 0x64;
  private static final int $GS = 0x65;
  private static final int $REXW = 0x48;
  private static final int $REXR = 0x44;
  private static final int $REXX = 0x42;
  private static final int $REXB = 0x41;
  private static final int $REXPrefixLow = 0x41;
  private static final int $REXPrefixHigh = 0x4f;
  public static final Prefix OPNDSZ = new SingleByteExactPrefix($OPNDSZ);
  public static final Prefix REPNZ = new SingleByteExactPrefix($REPNZ);
  public static final Prefix REPZ = new SingleByteExactPrefix($REPZ);
  public static final Prefix LOCK = new SingleByteExactPrefix($LOCK);
  public static final Prefix ADDRSZ = new SingleByteExactPrefix($ADDRSZ);
  public static final Prefix FWAIT = new SingleByteExactPrefix($FWAIT);
  public static final Prefix CS = new SingleByteExactPrefix($CS);
  public static final Prefix SS = new SingleByteExactPrefix($SS);
  public static final Prefix DS = new SingleByteExactPrefix($DS);
  public static final Prefix ES = new SingleByteExactPrefix($ES);
  public static final Prefix FS = new SingleByteExactPrefix($FS);
  public static final Prefix GS = new SingleByteExactPrefix($GS);
  public static final Prefix REXW = new REXPrefix($REXW);
  public static final Prefix REXR = new REXPrefix($REXR);
  public static final Prefix REXX = new REXPrefix($REXX);
  public static final Prefix REXB = new REXPrefix($REXB);
  private final Set<Prefix> prefixes = new HashSet<Prefix>();

  private X86InstructionPrefixes () {
  }

  public boolean contains (Prefix p) {
    return prefixes.contains(p);
  }

  public static X86InstructionPrefixes decode64 (final DecodeStream in) {
    /*
    final X86InstructionPrefixes actual = new X86InstructionPrefixes();
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
     */
    throw new UnsupportedOperationException();
  }

  private static boolean isRexPrefix (int value, int ord) {
    return (value & ord) == ord;
  }

  private void addPrefix (Prefix prefix) {
    prefixes.add(prefix);
  }

  public static X86InstructionPrefixes decode32 (final DecodeStream in) {
    /*
    final X86InstructionPrefixes actual = new X86InstructionPrefixes();
    for (;;) {
    int value = in.peek8();
    Prefix prefix = ords.get(value);
    if (prefix == null || prefix.isRexPrefix())
    break;
    in.read8();
    actual.prefixes.add(ords.get(value));
    }
    return actual;
     */
    throw new UnsupportedOperationException();
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

  public static abstract class Prefix {
    /**
     * Try decoding an instance of this prefix from the given decode-stream. If successful an instance of this prefix
     * gets added to the given prefixes.
     *
     * @param in The decode-stream.
     * @param prefixes The decoded prefixes so far.
     * @return Returns {@code true} if a prefix was decoded successfully.
     */
    public abstract boolean decode (DecodeStream in, X86InstructionPrefixes prefixes);

    /**
     * {@inheritDoc}
     */
    @Override public abstract boolean equals (Object obj);

    /**
     * {@inheritDoc}
     */
    @Override public abstract int hashCode ();
  }

  public static class SingleByteExactPrefix extends Prefix {
    private final int ord;

    public SingleByteExactPrefix (int ord) {
      this.ord = ord;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean decode (DecodeStream in, X86InstructionPrefixes prefixes) {
      int value = in.peek8();
      if (value == ord) { // prefix matches
        prefixes.addPrefix(this);
        return true;
      }
      // prefix doesn't match
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals (Object obj) {
      if (obj == null)
        return false;
      if (!(obj instanceof SingleByteExactPrefix))
        return false;
      final SingleByteExactPrefix other = (SingleByteExactPrefix) obj;
      if (this.ord != other.ord)
        return false;
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode () {
      return ord;
    }
  }

  public static class REXPrefix extends Prefix {
    private final int ord;

    public REXPrefix (int ord) {
      this.ord = ord;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean decode (DecodeStream in, X86InstructionPrefixes prefixes) {
      int value = in.peek8();
      if (inRange(value) && (value & ord) == ord) { // prefix matches
        prefixes.addPrefix(this);
        return true;
      }
      // prefix doesn't match
      return false;
    }

    private static boolean inRange (int value) {
      return value >= $REXPrefixLow && value <= $REXPrefixHigh;
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals (Object obj) {
      if (obj == null)
        return false;
      if (!(obj instanceof REXPrefix))
        return false;
      final REXPrefix other = (REXPrefix) obj;
      if (this.ord != other.ord)
        return false;
      return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int hashCode () {
      return ord;
    }
  }
}
