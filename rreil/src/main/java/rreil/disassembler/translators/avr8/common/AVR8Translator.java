package rreil.disassembler.translators.avr8.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.avr8.common.AVR8OperationTranslator.ReturnType;
import rreil.disassembler.translators.avr8.translators.AdcTranslator;
import rreil.disassembler.translators.avr8.translators.AddTranslator;
import rreil.disassembler.translators.avr8.translators.AdiwTranslator;
import rreil.disassembler.translators.avr8.translators.AndTranslator;
import rreil.disassembler.translators.avr8.translators.AsrTranslator;
import rreil.disassembler.translators.avr8.translators.BclssrTranslator;
import rreil.disassembler.translators.avr8.translators.BldTranslator;
import rreil.disassembler.translators.avr8.translators.BrbTranslator;
import rreil.disassembler.translators.avr8.translators.BstTranslator;
import rreil.disassembler.translators.avr8.translators.CallTranslator;
import rreil.disassembler.translators.avr8.translators.CbiTranslator;
import rreil.disassembler.translators.avr8.translators.CbrTranslator;
import rreil.disassembler.translators.avr8.translators.ComTranslator;
import rreil.disassembler.translators.avr8.translators.DecTranslator;
import rreil.disassembler.translators.avr8.translators.EorTranslator;
import rreil.disassembler.translators.avr8.translators.ICallTranslator;
import rreil.disassembler.translators.avr8.translators.IjmpTranslator;
import rreil.disassembler.translators.avr8.translators.IncTranslator;
import rreil.disassembler.translators.avr8.translators.JmpTranslator;
import rreil.disassembler.translators.avr8.translators.LacTranslator;
import rreil.disassembler.translators.avr8.translators.LasTranslator;
import rreil.disassembler.translators.avr8.translators.LatTranslator;
import rreil.disassembler.translators.avr8.translators.LpmTranslator;
import rreil.disassembler.translators.avr8.translators.LslTranslator;
import rreil.disassembler.translators.avr8.translators.MovMemTranslator;
import rreil.disassembler.translators.avr8.translators.MovTranslator;
import rreil.disassembler.translators.avr8.translators.MovwTranslator;
import rreil.disassembler.translators.avr8.translators.MulTranslator;
import rreil.disassembler.translators.avr8.translators.NegTranslator;
import rreil.disassembler.translators.avr8.translators.NopTranslator;
import rreil.disassembler.translators.avr8.translators.OrTranslator;
import rreil.disassembler.translators.avr8.translators.PopTranslator;
import rreil.disassembler.translators.avr8.translators.PushTranslator;
import rreil.disassembler.translators.avr8.translators.RcallTranslator;
import rreil.disassembler.translators.avr8.translators.RetTranslator;
import rreil.disassembler.translators.avr8.translators.RetiTranslator;
import rreil.disassembler.translators.avr8.translators.RjmpTranslator;
import rreil.disassembler.translators.avr8.translators.RolTranslator;
import rreil.disassembler.translators.avr8.translators.RorTranslator;
import rreil.disassembler.translators.avr8.translators.SbiTranslator;
import rreil.disassembler.translators.avr8.translators.SbicTranslator;
import rreil.disassembler.translators.avr8.translators.SbisTranslator;
import rreil.disassembler.translators.avr8.translators.SerTranslator;
import rreil.disassembler.translators.avr8.translators.SubTranslator;
import rreil.disassembler.translators.avr8.translators.SubcTranslator;
import rreil.disassembler.translators.avr8.translators.SubiwTranslator;
import rreil.disassembler.translators.avr8.translators.SwapTranslator;
import rreil.disassembler.translators.avr8.translators.XchTranslator;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationException;
import rreil.disassembler.translators.common.Translator;
import rreil.lang.lowlevel.LowLevelRReil;

/**
 *
 * @author mb0
 */
public class AVR8Translator implements Translator {
  public static final AVR8Translator $ = new AVR8Translator();
  private static final HashMap<String, InsnTranslator> translators = new HashMap<String, InsnTranslator>();

  static {
    translators.put("adc", new AdcTranslator());
    translators.put("add", new AddTranslator());
    translators.put("adiw", new AdiwTranslator());
    translators.put("and", new AndTranslator());
    translators.put("andi", new AndTranslator());
    translators.put("asr", new AsrTranslator());
    translators.put("bclr", new BclssrTranslator(0));
    translators.put("bld", new BldTranslator());
    translators.put("brbc", new BrbTranslator(false));
    translators.put("brbs", new BrbTranslator(true));
    translators.put("bset", new BclssrTranslator(1));
    translators.put("bst", new BstTranslator());
    translators.put("call", new CallTranslator());
    translators.put("cbi", new CbiTranslator());
    translators.put("cbr", new CbrTranslator());
    translators.put("com", new ComTranslator());
    translators.put("cp", new SubTranslator(ReturnType.None));
    translators.put("cpc", new SubcTranslator(ReturnType.None));
    translators.put("cpi", new SubTranslator(ReturnType.None));
//    translators.put("cpse", new CpseTranslator());
    translators.put("dec", new DecTranslator());
    translators.put("eor", new EorTranslator());
    translators.put("fmul", new MulTranslator(true, false, false, AVR8RegisterTranslator.$.translateRegister("r1"), AVR8RegisterTranslator.$.translateRegister("r0")));
    translators.put("fmuls", new MulTranslator(true, true, true, AVR8RegisterTranslator.$.translateRegister("r1"), AVR8RegisterTranslator.$.translateRegister("r0")));
    translators.put("fmulsu", new MulTranslator(true, true, false, AVR8RegisterTranslator.$.translateRegister("r1"), AVR8RegisterTranslator.$.translateRegister("r0")));
    translators.put("icall", new ICallTranslator());
    translators.put("ijmp", new IjmpTranslator());
    translators.put("in", new MovTranslator(true));
    translators.put("inc", new IncTranslator());
    translators.put("jmp", new JmpTranslator());
    translators.put("lac", new LacTranslator());
    translators.put("las", new LasTranslator());
    translators.put("lat", new LatTranslator());
    translators.put("ld", new MovTranslator());
    translators.put("ldi", new MovTranslator());
    translators.put("lds", new MovTranslator());
    translators.put("lpm", new LpmTranslator());
    translators.put("lsl", new LslTranslator());
    translators.put("mov", new MovTranslator());
    translators.put("movw", new MovwTranslator());
    translators.put("mul", new MulTranslator(false, false, false, AVR8RegisterTranslator.$.translateRegister("r1"), AVR8RegisterTranslator.$.translateRegister("r0")));
    translators.put("muls", new MulTranslator(false, true, true, AVR8RegisterTranslator.$.translateRegister("r1"), AVR8RegisterTranslator.$.translateRegister("r0")));
    translators.put("mulsu", new MulTranslator(false, true, false, AVR8RegisterTranslator.$.translateRegister("r1"), AVR8RegisterTranslator.$.translateRegister("r0")));
    translators.put("neg", new NegTranslator());
    translators.put("nop", new NopTranslator());
    translators.put("or", new OrTranslator());
    translators.put("ori", new OrTranslator());
    translators.put("out", new MovMemTranslator(true));
    translators.put("pop", new PopTranslator());
    translators.put("push", new PushTranslator());
    translators.put("rcall", new RcallTranslator());
    translators.put("ret", new RetTranslator());
    translators.put("reti", new RetiTranslator());
    translators.put("rjmp", new RjmpTranslator());
    translators.put("rol", new RolTranslator());
    translators.put("ror", new RorTranslator());
    translators.put("sbc", new SubcTranslator(ReturnType.Register));
    translators.put("sbci", new SubcTranslator(ReturnType.Register));
    translators.put("sbi", new SbiTranslator());
    translators.put("sbic", new SbicTranslator());
    translators.put("sbis", new SbisTranslator());
    translators.put("sbiw", new SubiwTranslator());
    translators.put("sbr", new OrTranslator());
//    translators.put("sbrc", new SbrcTranslator());
//    translators.put("sbrs", new SbrsTranslator());
    translators.put("ser", new SerTranslator());
    translators.put("sleep", new NopTranslator());
//    translators.put("spm", new SpmTranslator());
    translators.put("st", new MovMemTranslator());
    translators.put("sts", new MovMemTranslator());
    translators.put("sub", new SubTranslator(ReturnType.Register));
    translators.put("subi", new SubTranslator(ReturnType.Register));
    translators.put("swap", new SwapTranslator());
    translators.put("tst", new AndTranslator());
//    translators.put("wdr", new NopTranslator());
    translators.put("xch", new XchTranslator());
  }

  private AVR8Translator () {
  }

  /**
   * Translates an AVR8 instruction to RREIL code
   *
   * @param ctx
   *          A valid translation environment
   * @param insn
   *          The AVR8 instruction to translate
   * @return The list of REIL instruction the AVR8 instruction was translated to
   */
  public List<LowLevelRReil> translateRReil (TranslationCtx ctx, Instruction insn) {
    String mnemonic = insn.mnemonic();
    if (translators.containsKey(mnemonic)) {
      InsnTranslator translator = translators.get(mnemonic);
      ArrayList<LowLevelRReil> instructions = new ArrayList<LowLevelRReil>();
      translator.translate(ctx, insn, instructions);
      return instructions;
    } else if (mnemonic == null)
      return new ArrayList<LowLevelRReil>();
    else {
      ctx.setCurrentInstruction(insn);
      throw new TranslationException(ctx);
    }
  }

  @Override public List<LowLevelRReil> translate (TranslationCtx ctx, Instruction insn) {
    assert ctx != null : "null ctx";
    assert insn != null : "null instruction";
    return translateRReil(ctx, insn);
  }
}
