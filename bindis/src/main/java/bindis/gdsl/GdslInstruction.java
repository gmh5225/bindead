package bindis.gdsl;

import gdsl.rreil.IRReilCollection;
import gdsl.rreil.statement.IStatement;
import gdsl.translator.Translator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

import rreil.disassembler.Instruction;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.TranslationException;
import rreil.gdsl.StatementCollection;
import rreil.gdsl.builder.Builder;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.RReilHighLevelToLowLevelWrapper;

public class GdslInstruction extends Instruction {
  private final gdsl.decoder.NativeInstruction gdslInsn;
  private Translator rreilTranslator;

  public GdslInstruction (long address, byte[] opcode, gdsl.decoder.NativeInstruction gdslInsn, List<OperandTree> opnds,
      Translator rreilTranslator) {
    super(address, opcode, gdslInsn.mnemonic().toLowerCase(), opnds);
    this.gdslInsn = gdslInsn;
    this.rreilTranslator = rreilTranslator;
  }

  @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
    List<LowLevelRReil> rreil = new ArrayList<LowLevelRReil>();
    IRReilCollection<IStatement> coll = rreilTranslator.translate(gdslInsn);
    @SuppressWarnings("unchecked")
    StatementCollection statements = ((Builder<StatementCollection>) coll).build().getResult();
    SortedMap<RReilAddr, RReil> instructions = statements.getInstructions();
    for (Iterator<RReil> it = instructions.values().iterator(); it.hasNext();) {
      RReil next = it.next();
      rreil.add(new RReilHighLevelToLowLevelWrapper(next));
    }
    return rreil;
  }

  /**
   * {@inheritDoc}
   */
  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("%-6s", mnemonic().trim()));
    builder.append(" ");
    builder.append(getOperandsString());
    return prettyPrintFixup(builder.toString());
  }

  public String getMnemonicString () {
    return mnemonic().trim();
  }

  public String getOperandsString () {
    String rawString = gdslInsn.toString().trim().toLowerCase();
    String mnemonicString = getMnemonicString();
    String operandsString = rawString.replaceFirst(mnemonicString, "").trim();
    operandsString = operandsString.replaceAll("dword ptr", "DWORD PTR");
    operandsString = operandsString.replaceAll("ptr\\(0\\) ", ""); // first String is a regex
    return operandsString;
  }

  // deprecated as the operands printing does result in RREIL like syntax.
  // TODO: See how complex it is to fix the operands printing instead of the workaround above
  public String oldToString () {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("%-6s", gdslInsn.mnemonic()));
    builder.append(" ");
    for (int i = 0; i < gdslInsn.operands(); i++) {
      builder.append(gdslInsn.operandToString(i));
      if (i < gdslInsn.operands() - 1)
        builder.append(", ");
    }
    return prettyPrintFixup(builder.toString());
  }

  private static String prettyPrintFixup (String instruction) {
    return instruction.toLowerCase().replaceAll("dword ptr", "DWORD PTR");
  }
}
