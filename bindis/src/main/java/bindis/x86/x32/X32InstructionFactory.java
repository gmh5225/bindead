package bindis.x86.x32;

import bindis.x86.common.X86DecodeCtx;
import bindis.x86.common.X86InstructionFactory;
import bindis.x86.common.X86Operand;
import bindis.x86.common.X86Instruction.NativeX32Instruction;

/**
 * Factory implementation for X86 32bit instructions with support for mnemonic macros.
 *
 * @author mb0
 */
public class X32InstructionFactory extends X86InstructionFactory {
  @Override public NativeX32Instruction make (String name, X86DecodeCtx ctx) {
    return new NativeX32Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), null, null, null, ctx);
  }

  @Override public NativeX32Instruction make (String name, X86Operand op1, X86DecodeCtx ctx) {
    return new NativeX32Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), op1, null, null, ctx);
  }

  @Override public NativeX32Instruction make (String name, X86Operand op1, X86Operand op2, X86DecodeCtx ctx) {
    return new NativeX32Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), op1, op2, null, ctx);
  }

  @Override public NativeX32Instruction make (String name, X86Operand op1, X86Operand op2, X86Operand op3, X86DecodeCtx ctx) {
    return new NativeX32Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), op1, op2, op3, ctx);
  }
}
