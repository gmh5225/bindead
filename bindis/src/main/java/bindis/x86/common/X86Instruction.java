package bindis.x86.common;

import java.util.List;

import bindis.NativeInstruction;
import rreil.disassembler.Instruction;
import rreil.disassembler.Instruction.InstructionFactory;
import rreil.disassembler.OperandTree;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationException;
import rreil.disassembler.translators.x86.x32.X32RegisterTranslator;
import rreil.disassembler.translators.x86.x32.X32Translator;
import rreil.disassembler.translators.x86.x64.X64RegisterTranslator;
import rreil.disassembler.translators.x86.x64.X64Translator;
import rreil.lang.lowlevel.LowLevelRReil;

/**
 * Base class for X86 instructions.
 *
 * @author mb0
 */
public abstract class X86Instruction extends NativeInstruction {
  private final X86Prefixes prefixes;

  private X86Instruction (String mnemonic, X86Operand op1, X86Operand op2, X86Operand op3, X86DecodeCtx ctx) {
    super(ctx.getStartPc(), mnemonic, ctx.slice(), op1, op2, op3);
    this.prefixes = ctx.getPrefixes();
  }

  public X86Operand operand1 () {
    return (X86Operand) operand(0);
  }

  public X86Operand operand2 () {
    return (X86Operand) operand(1);
  }

  public X86Operand operand3 () {
    return (X86Operand) operand(2);
  }

  public X86Prefixes prefixes () {
    return prefixes;
  }

  /**
   * {@inheritDoc}
   */
  @Override public StringBuilder asString (StringBuilder pretty) {
    pretty = super.asString(pretty);
    if (!prefixes.isEmpty())
      pretty.append(" ; " + prefixes);
    return pretty;
  }

  public static class X32Instruction extends Instruction {
    private static final int $DefaultArchitectureSize = 32;

    public X32Instruction (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
      super(address, opcode, mnemonic, opnds);
    }

    /**
     * {@inheritDoc }
     */
    @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
      TranslationCtx ctx = new TranslationCtx(baseAddress(), X32RegisterTranslator.$, $DefaultArchitectureSize);
      return X32Translator.$.translate(ctx, this);
    }

    @Override public String toString () {
      return X86PrettyPrinter.print(this);
    }
  }

  /**
   * Native X86 32bit instructions.
   */
  public static class NativeX32Instruction extends X86Instruction {
    private static final String $Architecture = "X86-32";
    private static final InstructionFactory factory = new InstructionFactory() {
      @Override public Instruction build (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
        return new X32Instruction(address, opcode, mnemonic, opnds);
      }
    };
    private static final X86InstructionTranslator translator = new X86InstructionTranslator(factory);

    public NativeX32Instruction (String mnemonic, X86Operand op1, X86Operand op2, X86Operand op3, X86DecodeCtx ctx) {
      super(mnemonic, op1, op2, op3, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String architecture () {
      return $Architecture;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Instruction toTreeInstruction () {
      return translator.translate(this);
    }
  }

  public static class X64Instruction extends Instruction {
    private static final int $DefaultArchitectureSize = 64;

    public X64Instruction (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
      super(address, opcode, mnemonic, opnds);
    }

    /**
     * {@inheritDoc }
     */
    @Override public List<LowLevelRReil> toRReilInstructions () throws TranslationException {
      TranslationCtx ctx = new TranslationCtx(baseAddress(), X64RegisterTranslator.$, $DefaultArchitectureSize);
      return X64Translator.$.translate(ctx, this);
    }

    @Override public String toString () {
      return X86PrettyPrinter.print(this);
    }
  }

  /**
   * Native X86 64bit instructions.
   */
  public static class NativeX64Instruction extends X86Instruction {
    private static final String $Architecutre = "X86-64";
    private static final InstructionFactory factory = new InstructionFactory() {
      @Override public Instruction build (long address, byte[] opcode, String mnemonic, List<OperandTree> opnds) {
        return new X64Instruction(address, opcode, mnemonic, opnds);
      }
    };
    private static final X86InstructionTranslator translator = new X86InstructionTranslator(factory);

    public NativeX64Instruction (String mnemonic, X86Operand op1, X86Operand op2, X86Operand op3, X86DecodeCtx ctx) {
      super(mnemonic, op1, op2, op3, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public String architecture () {
      return $Architecutre;
    }

    /**
     * {@inheritDoc}
     */
    @Override public Instruction toTreeInstruction () {
      return translator.translate(this);
    }
  }
}
