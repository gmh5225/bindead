package bindis.avr8;

import bindis.DecodeCtx;
import bindis.DecodeStream;
import rreil.disassembler.OpcodeFormatter;

/**
 * AVR-8 decode context.
 *
 * @author mb0
 */
public final class AVRDecodeCtx extends DecodeCtx {
  private int insnWord = -1;
  private int insnParameterWord = -1;

  public AVRDecodeCtx (DecodeStream stream, int startPc) {
    super(stream, startPc);
  }

  public int getInsnWord () {
    if (insnWord == -1)
      insnWord = getDecodeStream().read16();
    return insnWord;
  }

  public int getInsnParameterWord () {
    if (insnParameterWord == -1)
      insnParameterWord = getDecodeStream().read16();
    return insnParameterWord;
  }

  /**
   * {@inheritDoc}
   */
  @Override public String toErrorString () {
    StringBuilder builder = new StringBuilder();
    builder.append("{pc: 0x").append(Long.toHexString(getStartPc()));
    builder.append(", consumed-bytes: <");
    OpcodeFormatter.format(getDecodeStream().slice(), builder);
    builder.append('>');
    builder.append('}');
    return builder.toString();
  }
}
