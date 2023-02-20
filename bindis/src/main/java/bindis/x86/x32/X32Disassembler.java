package bindis.x86.x32;

import java.nio.ByteOrder;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.Disassembler;
import bindis.NativeDisassembler;
import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.TranslationException;
import rreil.disassembler.translators.x86.x32.X32RegisterTranslator;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 *
 */
public class X32Disassembler extends Disassembler {
  public static final X32Disassembler INSTANCE = new X32Disassembler();
  private static final String $ArchitectureName = "X86-32";
  private static final int $ArchitectureSize = 32;
  private static final ByteOrder $ByteOrder = ByteOrder.LITTLE_ENDIAN;
  private static final NativeDisassembler $Disassembler = X32NativeDisassembler.$;

  private X32Disassembler () {
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
    return X32RegisterTranslator.$.translateRegister(name);
  }
}
