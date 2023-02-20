package bindis.x86.x32;

import bindis.DecodeStream;
import bindis.x86.common.X86NativeDisassembler;
import bindis.x86.common.X86Prefixes;
import javalx.exceptions.UnimplementedException;
import rreil.disassembler.BlockOfInstructions;

/**
 * X86 32bit disassembler.
 *
 * @author mb0
 */
public class X32NativeDisassembler extends X86NativeDisassembler {
  public static final X32NativeDisassembler $ = new X32NativeDisassembler();

  private X32NativeDisassembler () {
    super(new X32DecodeTable());
  }

  /**
   * {@inheritDoc}
   */
  @Override public X86Prefixes decodePrefixes (DecodeStream in) {
    return X86Prefixes.decode32(in);
  }

  @Override public BlockOfInstructions decodeBlock (DecodeStream in, long pc) {
    throw new UnimplementedException();
  }
}
