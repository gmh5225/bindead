package bindis.x86.x64;

import bindis.x86.common.X86DecodeCtx;
import bindis.x86.common.X86InstructionFactory;
import bindis.x86.common.X86Operand;
import bindis.x86.common.X86Instruction.NativeX64Instruction;

/**
 * Factory implementation for X86 64bit instructions with support for mnemonic macros.
 *
 * @author mb0
 */
public class X64InstructionFactory extends X86InstructionFactory {
  @Override public NativeX64Instruction make (String name, X86DecodeCtx ctx) {
    return new NativeX64Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), null, null, null, ctx);
  }

  @Override public NativeX64Instruction make (String name, X86Operand op1, X86DecodeCtx ctx) {
    return new NativeX64Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), op1, null, null, ctx);
  }

  @Override public NativeX64Instruction make (String name, X86Operand op1, X86Operand op2, X86DecodeCtx ctx) {
    return new NativeX64Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), op1, op2, null, ctx);
  }

  @Override public NativeX64Instruction make (String name, X86Operand op1, X86Operand op2, X86Operand op3, X86DecodeCtx ctx) {
    return new NativeX64Instruction(applyMnemonicMacros(name, ctx.getPrefixes()), op1, op2, op3, ctx);
  }
}
