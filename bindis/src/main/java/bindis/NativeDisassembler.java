package bindis;

import rreil.disassembler.BlockOfInstructions;

/**
 * Minimal interface for disassemblers.
 */
public interface NativeDisassembler {

  /**
   * Decode one instruction from the given decode-stream.
   *
   * @param in The current input byte-stream.
   * @param pc The value of the program counter to be filled when decoding relative addresses.
   * @return The decoded instruction
   * @throws DecodeException if instruction decoding failed.
   */
  public INativeInstruction decode (DecodeStream in, long pc) throws DecodeException;

  /**
   * Decode a block of instructions from the given decode-stream. Decoding a sequence of instructions
   * allows the disassembler frontend to perform optimizations on the decoded sequence (e.g. liveness analysis).
   *
   * @param in The current input byte-stream.
   * @param pc The value of the program counter to be used when decoding relative addresses.
   * @return The decoded instruction.
   * @throws DecodeException if instruction decoding failed.
   */
  public BlockOfInstructions decodeBlock(DecodeStream in, long pc);
}
