package bindis.x86.common;

import bindis.DecodeCtx;
import bindis.DecodeStream;
import rreil.disassembler.OpcodeFormatter;

/**
 * X86 decode contexts.
 *
 * @author mb0
 */
public final class X86DecodeCtx extends DecodeCtx {
  private final X86Prefixes prefixes;
  private X86ModRM ModRM = null;

  public X86DecodeCtx (DecodeStream decodeStream, X86Prefixes prefixes, long startPc) {
    super(decodeStream, startPc);
    this.prefixes = prefixes;
  }

  public X86Prefixes getPrefixes () {
    return prefixes;
  }

  public X86ModRM getModRM () {
    if (ModRM == null)
      ModRM = X86ModRM.decode(getDecodeStream());
    return ModRM;
  }

  /**
   * {@inheritDoc}
   */
  @Override public String toErrorString () {
    StringBuilder builder = new StringBuilder();
    builder.append("{pc: 0x").append(Long.toHexString(getStartPc()));
    builder.append(", consumed-bytes: <");
    OpcodeFormatter.format(slice(), builder);
    builder.append('>');
    builder.append(", prefixes: ").append(prefixes).append('}');
    return builder.toString();
  }
}
