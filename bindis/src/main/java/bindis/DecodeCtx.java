package bindis;

/**
 * Abstract base class used during decoding instructions defining a decode context.
 *
 * @author mb0
 */
public abstract class DecodeCtx {
  private final DecodeStream decodeStream;
  private final long startPc;

  /**
   * Construct a fresh decode context.
   *
   * @param decodeStream The decode-stream.
   * @param startPc The value of the (virtual) program counter (pc).
   */
  public DecodeCtx (DecodeStream decodeStream, long startPc) {
    this.decodeStream = decodeStream;
    this.startPc = startPc;
  }

  public DecodeStream getDecodeStream () {
    return decodeStream;
  }

  /**
   * Return the consumed bytes constituting the opcode discovered so far.
   *
   * @return
   */
  public byte[] slice () {
    return decodeStream.slice();
  }

  public long getStartPc () {
    return startPc;
  }

  /**
   * Renders this context-state for the case that an error happened during instruction decoding.
   *
   * @return An error message.
   */
  public abstract String toErrorString ();
}
