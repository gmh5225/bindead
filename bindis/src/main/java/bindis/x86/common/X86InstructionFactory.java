package bindis.x86.common;

import bindis.x86.common.X86Prefixes.Prefix;

/**
 * Abstract base class for X86 instruction factories providing convenience methods for creating instructions
 * with one, two or three operands.
 *
 * @author mb0
 */
public abstract class X86InstructionFactory {
  public abstract X86Instruction make (String name, X86DecodeCtx ctx);

  public abstract X86Instruction make (String name, X86Operand op1, X86DecodeCtx ctx);

  public abstract X86Instruction make (String name, X86Operand op1, X86Operand op2, X86DecodeCtx ctx);

  public abstract X86Instruction make (String name, X86Operand op1, X86Operand op2, X86Operand op3, X86DecodeCtx ctx);

  protected String applyMnemonicMacros (final String pattern, final X86Prefixes prefixes) {
    assert pattern != null : "null pattern";
    if (pattern.indexOf('|') != -1)
      return applyOrPattern(pattern, prefixes);
    String mnemonic = pattern;
    mnemonic = applySizeMacro(prefixes, mnemonic);
    mnemonic = applyJccMacro(prefixes, mnemonic);
    mnemonic = applyFwaitMacro(prefixes, mnemonic);
    mnemonic = applyRepLockMacro(prefixes, mnemonic);
    if (prefixes.contains(Prefix.LOCK))
      mnemonic = "lock " + mnemonic;
    return mnemonic;
  }

  private static String applySizeMacro (X86Prefixes prefixes, String mnemonic) {
    if (mnemonic.lastIndexOf('S') == -1)
      return mnemonic;
    char replacement = prefixes.contains(Prefix.REXW) ? 'q' : prefixes.contains(Prefix.OPNDSZ) ? 'w' : 'd';
    return mnemonic.replace('S', replacement);
  }

  private static String applyJccMacro (X86Prefixes prefixes, String mnemonic) {
    String replacement32 = prefixes.contains(Prefix.ADDRSZ) ? "" : "e";
    String replacement64 = prefixes.contains(Prefix.ADDRSZ) ? "" : "r";
    mnemonic = mnemonic.replace("C32", replacement32);
    mnemonic = mnemonic.replace("C64", replacement64);
    return mnemonic;
  }

  private static String applyRepLockMacro (X86Prefixes prefixes, String mnemonic) {
    String replacement = "";
    if (prefixes.contains(Prefix.REPZ))
      replacement = "repz ";
    else if (prefixes.contains(Prefix.REPNZ))
      replacement = "repnz ";
    return mnemonic.replace("R", replacement);
  }

  private static String applyFwaitMacro (X86Prefixes prefixes, String mnemonic) {
    String replacement = "n"; //prefixes.contains(Prefix.FWAIT) ? "n" : "";
    return mnemonic.replace("N", replacement);
  }

  private static String applyOrPattern (String pattern, X86Prefixes prefixes) {
    String[] names = pattern.split("\\|");
    assert names.length == 2 || names.length == 3 : "Invalid pattern for mnemonic fixup";
    if (names.length == 2) {
      if (prefixes.contains(Prefix.OPNDSZ))
        return names[1];
      else
        return names[0];
    } else if (names.length == 3) {
      if (prefixes.contains(Prefix.REXW))
        return names[2];
      else if (prefixes.contains(Prefix.OPNDSZ))
        return names[1];
      else
        return names[0];
    } else
      throw new UnsupportedOperationException("Invalid or-pattern as mnemonic");
  }
}
