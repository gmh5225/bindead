package rreil.disassembler;

import java.util.List;

import rreil.disassembler.translators.common.TranslationException;
import rreil.lang.lowlevel.LowLevelRReil;

/**
 * A block or sequence of instructions. Used by disassembler frontends that want to decode
 * more than one instruction from a given address to be able to perform optimizations on the code
 * (e.g. liveness analysis).
 */
public interface BlockOfInstructions {

  /**
   * The sequence of decoded native instructions.
   */
  public abstract List<Instruction> getInstructions();

  /**
   * The optimized sequence of RREIL instructions corresponding to the decoded native instructions
   * as returned by {@link #getInstructions()}. Note that because of optimizations performed by
   * the disassembler these RREIL instructions are not the same as the ones obtained by calling
   * {@link #getInstructions()} and then the instruction.toRREIL() method.
   */
  public abstract List<LowLevelRReil> toRReilInstructions () throws TranslationException;

  /**
   * Return the length in bytes of all the native instructions in this block.
   */
  public int byteLength ();
}
