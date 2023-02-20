package rreil.disassembler.translators.x86.common;

import java.util.List;

import rreil.disassembler.translators.common.CondEmitter;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.lang.lowlevel.OperandSize;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;

public class X86Conditions {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  /* == Base conditions == */
  public static final CondEmitter BELOW = new BelowEmitter();
  public static final CondEmitter BELOW_OR_EQUAL = new BelowEqualEmitter();
  public static final CondEmitter LESS = new LessEmitter();
  public static final CondEmitter LESS_OR_EUQAL = new LessEqualEmitter();
  public static final CondEmitter ZERO = new ZeroEmitter();
  public static final CondEmitter SIGN = new SignEmitter();
  public static final CondEmitter OVERFLOW = new OverflowEmitter();
  public static final CondEmitter PARITY = new ParityEmitter();
  public static final CondEmitter TRUE = new AlwaysTrueEmitter();
  public static final CondEmitter ECX_ZERO = new EcxZeroEmitter();
  public static final CondEmitter CX_ZERO = new CxZeroEmitter();

  /* == Derived conditions == */
  public static final CondEmitter NOT_OVERFLOW = new NotEmitter(OVERFLOW);
  public static final CondEmitter NOT_SIGN = new NotEmitter(SIGN);
  public static final CondEmitter NOT_ZERO = new NotEmitter(ZERO);
  public static final CondEmitter NOT_PARITY = new NotEmitter(PARITY);
  public static final CondEmitter ABOVE = new NotEmitter(BELOW_OR_EQUAL);
  public static final CondEmitter ABOVE_OR_EQUAL = new NotEmitter(BELOW);
  public static final CondEmitter GREATER = new NotEmitter(LESS_OR_EUQAL);
  public static final CondEmitter GREATER_OR_EQUAL = new NotEmitter(LESS);

  public static class AlwaysTrueEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return factory.immediate(OperandSize.BIT, 1);
    }
  }

  public static class ParityEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.PARITY_FLAG_OPERAND;
    }
  }

  public static class ZeroEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.EQUAL_FLAG_OPERAND;
    }
  }

  public static class SignEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.SIGN_FLAG_OPERAND;
    }
  }

  public static class OverflowEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.OVERFLOW_FLAG_OPERAND;
    }
  }

  public static class BelowEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.BELOW_FLAG_OPERAND;
    }
  }

  public static class BelowEqualEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.BELOW_OR_EQUAL_FLAG_OPERAND;
    }
  }

  public static class LessEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.LESS_FLAG_OPERAND;
    }
  }

  public static class LessEqualEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      return X86Helpers.LESS_OR_EQUAL_FLAG_OPERAND;
    }
  }

  public static class NotEmitter implements CondEmitter {
    private final CondEmitter conditionEmitter;

    public NotEmitter (final CondEmitter conditionEmitter) {
      this.conditionEmitter = conditionEmitter;
    }

    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd cond = conditionEmitter.emit(env, insns);
      final LowLevelRReilOpnd notCond = registerTranslator.temporaryRegister(env, cond.size());

      insns.add(factory.NOT(env.getNextReilAddress(), notCond, cond));

      return notCond;
    }
  }

  public static class EcxZeroEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, final List<LowLevelRReil> insns) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd ecx = registerTranslator.translateRegister("ecx");
      final LowLevelRReilOpnd zero = factory.immediate(ecx.size(), 0);
      final LowLevelRReilOpnd cond = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      insns.add(factory.CMPEQ(env.getNextReilAddress(), cond, ecx, zero));
      return cond;
    }
  }

  public static class CxZeroEmitter implements CondEmitter {
    @Override
    public LowLevelRReilOpnd emit (TranslationCtx env, List<LowLevelRReil> insns) {
      final RegisterTranslator registerTranslator = env.getRegisterTranslator();
      final LowLevelRReilOpnd cx = registerTranslator.translateRegister("cx");
      final LowLevelRReilOpnd zero = factory.immediate(cx.size(), 0);
      final LowLevelRReilOpnd cond = registerTranslator.temporaryRegister(env, OperandSize.BIT);

      insns.add(factory.CMPEQ(env.getNextReilAddress(), cond, cx, zero));
      return cond;
    }
  }
}
