package bindis.gdsl;

import gdsl.rreil.IRReilCollection;
import gdsl.rreil.statement.IStatement;
import gdsl.translator.TranslatedBlock;
import gdsl.translator.Translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import rreil.disassembler.BlockOfInstructions;
import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.TranslationException;
import rreil.gdsl.StatementCollection;
import rreil.gdsl.builder.StatementCollectionBuilder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.RReilHighLevelToLowLevelWrapper;

public class GdslBlockOfInstructions implements BlockOfInstructions {
  private final TranslatedBlock insnBlock;
  private final byte[] opcode;
  private final long pc;
  private final Translator rreilTranslator;

  public GdslBlockOfInstructions (TranslatedBlock block, byte[] opcode, long pc, Translator rreilTranslator) {
    this.insnBlock = block;
    this.opcode = opcode;
    this.pc = pc;
    this.rreilTranslator = rreilTranslator;
  }

  @Override public List<Instruction> getInstructions () {
    gdsl.decoder.NativeInstruction[] gInsns = insnBlock.getInstructions();
    Instruction[] insns = new GdslInstruction[gInsns.length];

    int blockSize = 0;
    for (int i = 0; i < insns.length; i++) {
      gdsl.decoder.NativeInstruction gInsn = gInsns[i];

      byte[] opcode = new byte[(int) gInsn.getSize()];
      for (int j = 0; j < opcode.length; j++)
        opcode[j] = this.opcode[blockSize + j];

      GdslNativeInstruction nativeInstruction = new GdslNativeInstruction(gInsn, opcode, pc + blockSize, rreilTranslator);
      insns[i] = nativeInstruction.toTreeInstruction();
      blockSize += opcode.length;
    }

    return Arrays.asList(insns);
  }

  @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
    List<LowLevelRReil> rreil = new ArrayList<LowLevelRReil>();
    IRReilCollection<IStatement> coll = insnBlock.getRreil();
    StatementCollection statements = ((StatementCollectionBuilder) coll).build().getResult();
    SortedMap<RReilAddr, RReil> instructions = statements.getInstructions();
    for (Iterator<RReil> it = instructions.values().iterator(); it.hasNext();) {
      RReil insn = it.next();
      rreil.add(new RReilHighLevelToLowLevelWrapper(insn));
    }
    return rreil;
  }

  @Override public int byteLength () {
    return opcode.length;
  }

}
