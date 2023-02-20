package bindis.x86.x64;

import java.nio.ByteOrder;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.Disassembler;
import bindis.NativeDisassembler;
import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.TranslationException;
import rreil.disassembler.translators.x86.x64.X64RegisterTranslator;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 */
public class X64Disassembler extends Disassembler {
  public static final X64Disassembler INSTANCE = new X64Disassembler();
  private static final String $ArchitectureName = "X86-64";
  private static final int $ArchitectureSize = 64;
  private static final ByteOrder $ByteOrder = ByteOrder.LITTLE_ENDIAN;
  private static final NativeDisassembler $Disassembler = X64NativeDisassembler.$;

  public X64Disassembler () {
    super($ArchitectureName, $ArchitectureSize, $Disassembler, $ByteOrder);
  }

  /**
   * {@inheritDoc}
   */
  @Override public Instruction decodeOne (DecodeStream in, long pc) throws DecodeException {
    return $Disassembler.decode(in, pc).toTreeInstruction();
  }

  /**
   * {@inheritDoc}
   */
  @Override public LowLevelRReilOpnd translateIdentifier (String name) throws TranslationException {
    return X64RegisterTranslator.$.translateRegister(name);
  }
}
