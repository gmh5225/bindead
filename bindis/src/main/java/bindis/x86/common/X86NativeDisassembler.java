package bindis.x86.common;

import java.nio.ByteOrder;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.IndexOutOfBoundsException;
import bindis.NativeDisassembler;
import bindis.NativeInstruction;

/**
 * Abstract base class for X86 disassemblers.
 *
 * @author mb0
 */
public abstract class X86NativeDisassembler implements NativeDisassembler {
  private final X86DecodeTable decodeTable;

  /**
   * Build a disassembler using the given decode-table.
   *
   * @param decodeTable The decode-table.
   */
  protected X86NativeDisassembler (X86DecodeTable decodeTable) {
    this.decodeTable = decodeTable;
  }

  /**
   * Decode all prefixes for the underlying decode mode (32bit vs 64bit) from the given decode-stream.
   *
   * @param in The decode-stream.
   * @return The decoded prefixes.
   */
  public abstract X86Prefixes decodePrefixes (DecodeStream in);

  /**
   * {@inheritDoc}
   */
  @Override public NativeInstruction decode (DecodeStream in, long currentPc) {
    in.order(ByteOrder.LITTLE_ENDIAN);
    X86Prefixes prefixes = decodePrefixes(in);
    X86InstructionDecoder[] opcodeTable = findOpcodeTable(in, prefixes);
    assert (opcodeTable != null) : "Null opcodeTable";
    int opcode = in.read8();
    X86InstructionDecoder decoder = opcodeTable[opcode];
    X86DecodeCtx ctx = new X86DecodeCtx(in, prefixes, currentPc);
    if (decoder == null)
      throw DecodeException.unknownOpcode(ctx);
    NativeInstruction insn;
    try {
      insn = decoder.decode(ctx);
    } catch (IndexOutOfBoundsException e) {
      throw DecodeException.outOfBytes(e.getIndex(), ctx);
    } catch (Exception e) {
      throw DecodeException.generalException(e, ctx);
    }
    if (insn == null)
      throw DecodeException.unknownOpcode(ctx);
    return insn;
  }

  private X86InstructionDecoder[] findOpcodeTable (DecodeStream in, X86Prefixes prefixes) {
    int opcodeOrEscape = in.peek8();
    if (opcodeOrEscape == X86Consts.ESCAPE_OPCODE) {
      in.read8();
      if (prefixes.contains(X86Prefixes.Prefix.REPNZ))
        return decodeTable.twoByteOpcodePrefixF2Table;
      else if (prefixes.contains(X86Prefixes.Prefix.REPZ))
        return decodeTable.twoByteOpcodePrefixF3Table;
      else if (prefixes.contains(X86Prefixes.Prefix.OPNDSZ)) {
        int realOpcode = in.peek8();
        // HACK: OPNDSZ prefix might be present, but is not used to select a different instruction; if this happens
        // we try to decode from the default two-byte-opcode table.
        if (decodeTable.twoByteOpcodePrefix66Table[realOpcode] != null)
          return decodeTable.twoByteOpcodePrefix66Table;
        return decodeTable.twoByteOpcodeTable;
      } else
        return decodeTable.twoByteOpcodeTable;
    } else {
      if (prefixes.contains(X86Prefixes.Prefix.REPZ))
        return decodeTable.twoByteOpcodePrefixF3Table;
      return decodeTable.oneByteOpcodeTable;
    }

  }
}
