package bindis;

/**
 * Base class for exceptions thrown when decoding an instruction fails.
 *
 * @author mb0
 */
public class DecodeException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final ErrCtx erroneousCtx;

  private DecodeException (Exception cause, ErrCtx obj) {
    super(obj.toString(), cause);
    this.erroneousCtx = obj;
  }

  private DecodeException (ErrCtx obj) {
    super(obj.toString());
    this.erroneousCtx = obj;
  }

  public ErrCtx getErrCtx () {
    return erroneousCtx;
  }

  public static DecodeException generalException(Exception cause, DecodeCtx ctx) {
    return new DecodeException(cause, new ErrCtx(ctx, "Error while decoding"));
  }

  public static DecodeException unknownOpcode (DecodeCtx ctx) {
    return new DecodeException(new ErrCtx(ctx, "Unknown opcode"));
  }

  public static DecodeException outOfBytes (int index, DecodeCtx ctx) {
    return new DecodeException(new ErrCtx(ctx, "Out of bytes access at index " + index));
  }

  public static DecodeException inconsistentDecoder (DecodeCtx ctx) {
    return new DecodeException(new ErrCtx(ctx, "Inconsistend decoder specification"));
  }

  public static class ErrCtx {
    private final DecodeCtx ctx;
    private final String reason;

    public ErrCtx (DecodeCtx ctx, String reason) {
      this.ctx = ctx;
      this.reason = reason;
    }

    public DecodeCtx getDecodeCtx () {
      return ctx;
    }

    @Override public String toString () {
      if (getDecodeCtx() == null)
        return "ErrCtx{ctx=null, reason=" + reason + '}';
      return "ErrCtx{ctx=" + ctx.toErrorString() + ", reason=" + reason + '}';
    }
  }
}
