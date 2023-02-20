package bindis.gdsl;

import gdsl.arch.ArchId;
import gdsl.arch.IConfigFlag;
import gdsl.arch.X86ConfigFlag;

import java.nio.ByteOrder;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.Disassembler;
import bindis.NativeDisassembler;
import javalx.exceptions.UnimplementedException;
import rreil.disassembler.BlockOfInstructions;
import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class GdslX86_32Disassembler extends Disassembler {
  private static final ArchId $Architecture = ArchId.X86.setName("x86-32");
  private static final IConfigFlag[] $ArchConfig = {X86ConfigFlag.MODE32};
  private static final int $ArchitectureSize = 32;
  private static final ByteOrder $ByteOrder = ByteOrder.LITTLE_ENDIAN;
  public static final GdslNativeDisassembler $Disassembler = new GdslNativeDisassembler($Architecture, $ArchConfig);
  public static final GdslX86_32Disassembler instance = new GdslX86_32Disassembler();

  private GdslX86_32Disassembler () {
    super($Architecture.getName(), $ArchitectureSize, $Disassembler, $ByteOrder);
  }

  @Override public Instruction decodeOne (DecodeStream in, long pc) throws DecodeException {
    return $Disassembler.decode(in, pc).toTreeInstruction();
  }

  @Override public BlockOfInstructions decodeBlock (DecodeStream in, long pc) throws DecodeException {
    return $Disassembler.decodeBlock(in, pc);
  }

  @Override public LowLevelRReilOpnd translateIdentifier (String name) throws TranslationException {
    throw new UnimplementedException();
  }
}
