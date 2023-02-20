package rreil.interpreter;

/**
 *
 * @author mb0
 */
public final class MachineCtx {
  private final InterpCtx ctx;
  private final byte[] code;
  private final int offset;
  private final long startPc;

  public MachineCtx (final InterpCtx ctx, final byte[] code, final int offset, final long startPc) {
    this.ctx = ctx;
    this.code = code;
    this.offset = offset;
    this.startPc = startPc;
  }

  public byte[] getCode () {
    return code;
  }

  public InterpCtx getCtx () {
    return ctx;
  }

  public int getOffset () {
    return offset;
  }

  public long getStartPc () {
    return startPc;
  }
}
