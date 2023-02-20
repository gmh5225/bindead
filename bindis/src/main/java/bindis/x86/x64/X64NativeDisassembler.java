package bindis.x86.x64;

import bindis.DecodeStream;
import bindis.x86.common.X86NativeDisassembler;
import bindis.x86.common.X86Prefixes;
import javalx.exceptions.UnimplementedException;
import rreil.disassembler.BlockOfInstructions;

/**
 * X86 64bit disassembler.
 *
 * @author mb0
 */
public class X64NativeDisassembler extends X86NativeDisassembler {
  public static final X64NativeDisassembler $ = new X64NativeDisassembler();

  private X64NativeDisassembler () {
    super(new X64DecodeTable());
  }

  /**
   * {@inheritDoc}
   */
  @Override public X86Prefixes decodePrefixes (DecodeStream in) {
    return X86Prefixes.decode64(in);
  }

  @Override public BlockOfInstructions decodeBlock (DecodeStream in, long pc) {
    throw new UnimplementedException();
  }
}
