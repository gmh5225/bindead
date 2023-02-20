package bindis;

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

import rreil.disassembler.BlockOfInstructions;
import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

/**
 * Disassembler platforms contain the disassembler for a given architecture as well as additional information
 * about the architectures hardware characteristics such as endianess, bit-size etc.
 */
public abstract class Disassembler {
  private final String architectureName;
  private final int registerBaseSize;
  private final ByteOrder byteOrder;
  private final NativeDisassembler nativeDisassembler;

  public Disassembler (String name, int baseSize, NativeDisassembler dis, ByteOrder byteOrder) {
    this.architectureName = name;
    this.nativeDisassembler = dis;
    this.registerBaseSize = baseSize;
    this.byteOrder = byteOrder;
  }

  /**
   * Return the architectures name. This name is unique among all exported platforms.
   */
  public String getArchitectureName () {
    return architectureName;
  }

  /**
   * Decode one instruction from the given decode-stream.
   *
   * @param in The current input byte-stream.
   * @param pc The value of the program counter to be used when decoding relative addresses.
   * @return The decoded instruction.
   * @throws DecodeException if instruction decoding failed.
   */
  public abstract Instruction decodeOne (DecodeStream in, long pc) throws DecodeException;

  /**
   * Decode one instruction from the given byte array at {@code offset}.
   *
   * @param code Byte array carrying the opcodes.
   * @param offset The offset at which to start to disassemble.
   * @param pc The value of the program counter to be used when decoding relative addresses.
   * @return The decoded instruction.
   * @throws DecodeException if instruction decoding failed.
   */
  public Instruction decodeOne (byte[] code, int offset, long pc) throws DecodeException {
    DecodeStream in = new DecodeStream(code, offset);
    in.order(ByteOrder.LITTLE_ENDIAN);
    return decodeOne(in, pc);
  }

  /**
   * Decode a block of instructions from the given decode-stream. Decoding a sequence of instructions
   * allows the disassembler frontend to perform optimizations on the decoded sequence (e.g. liveness analysis).
   *
   * @param in The current input byte-stream.
   * @param pc The value of the program counter to be used when decoding relative addresses.
   * @return The decoded instruction.
   * @throws DecodeException if instruction decoding failed.
   */
  public BlockOfInstructions decodeBlock (DecodeStream in, long pc) throws DecodeException {
    return new BlockOfOneInstruction(decodeOne(in, pc));
  }

  /**
   * Decode a block of instructions from the given byte array at {@code offset}. Decoding a sequence of instructions
   * allows the disassembler frontend to perform optimizations on the decoded sequence (e.g. liveness analysis).
   *
   * @param code Byte array carrying the opcodes.
   * @param offset The offset at which to start to disassemble.
   * @param pc The value of the program counter to be used when decoding relative addresses.
   * @return The decoded instruction.
   * @throws DecodeException if instruction decoding failed.
   */
  public BlockOfInstructions decodeBlock (byte[] code, int offset, long pc) throws DecodeException {
    DecodeStream in = new DecodeStream(code, offset);
    in.order(ByteOrder.LITTLE_ENDIAN);
    return decodeBlock(in, pc);
  }

  /**
   * Translates the register/identifier with {@code name} to an RREIL (right-hand-side) operand.
   *
   * @param name The name of the register to be translated.
   * @return The RREIL operand corresponding to the given name.
   * @throws TranslationException if the given name can't be translated to an RREIL operand.
   */
  public abstract LowLevelRReilOpnd translateIdentifier (String name) throws TranslationException;

  /**
   * Return the {@link bindis.NativeInstruction} disassembler for this platform.
   */
  public NativeDisassembler getNativeDisassembler () {
    return nativeDisassembler;
  }

  /**
   * The default bit-size of this platform.
   */
  public int defaultArchitectureSize () {
    return registerBaseSize;
  }

  /**
   * The default byte-order of this platform.
   */
  public ByteOrder getByteOrder () {
    return byteOrder;
  }

  private static class BlockOfOneInstruction implements BlockOfInstructions {
    private final Instruction insn;

    public BlockOfOneInstruction (Instruction insn) {
      this.insn = insn;
    }

    @Override public List<Instruction> getInstructions () {
      return Collections.singletonList(insn);
    }

    @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
      return insn.toRReilInstructions();
    }

    @Override public int byteLength () {
      return insn.length();
    }

  }
}
