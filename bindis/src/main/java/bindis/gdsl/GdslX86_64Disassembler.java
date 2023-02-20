package bindis.gdsl;

import gdsl.arch.ArchId;
import gdsl.arch.IConfigFlag;

import java.nio.ByteOrder;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.Disassembler;
import javalx.exceptions.UnimplementedException;
import rreil.disassembler.BlockOfInstructions;
import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class GdslX86_64Disassembler extends Disassembler {
  private static final ArchId $Architecture = ArchId.X86.setName("x86-64");
  private static final IConfigFlag[] $ArchConfig = {};
  private static final int $ArchitectureSize = 64;
  private static final ByteOrder $ByteOrder = ByteOrder.LITTLE_ENDIAN;
  public static final GdslNativeDisassembler $Disassembler = new GdslNativeDisassembler($Architecture, $ArchConfig);
  public static final GdslX86_64Disassembler instance = new GdslX86_64Disassembler();

  private GdslX86_64Disassembler () {
    // FIXME: need to make two classes for x86-32 and x86-64 and chose the right platform for it
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
