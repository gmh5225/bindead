package rreil.disassembler.translators.x86.x64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rreil.disassembler.Instruction;
import rreil.disassembler.translators.common.InsnTranslator;
import rreil.disassembler.translators.common.TranslationCtx;
import rreil.disassembler.translators.common.TranslationException;
import rreil.disassembler.translators.common.Translator;
import rreil.disassembler.translators.x86.common.CallTranslator;
import rreil.disassembler.translators.x86.common.CdqTranslator;
import rreil.disassembler.translators.x86.common.ClcTranslator;
import rreil.disassembler.translators.x86.common.CldTranslator;
import rreil.disassembler.translators.x86.common.CliTranslator;
import rreil.disassembler.translators.x86.common.CmcTranslator;
import rreil.disassembler.translators.x86.common.CmovccTranslator;
import rreil.disassembler.translators.x86.common.CmpsTranslator;
import rreil.disassembler.translators.x86.common.CwdTranslator;
import rreil.disassembler.translators.x86.common.CwdeTranslator;
import rreil.disassembler.translators.x86.common.DecTranslator;
import rreil.disassembler.translators.x86.common.DivTranslator;
import rreil.disassembler.translators.x86.common.IdivTranslator;
import rreil.disassembler.translators.x86.common.ImulTranslator;
import rreil.disassembler.translators.x86.common.IncTranslator;
import rreil.disassembler.translators.x86.common.JccTranslator;
import rreil.disassembler.translators.x86.common.LahfTranslator;
import rreil.disassembler.translators.x86.common.LeaTranslator;
import rreil.disassembler.translators.x86.common.LodsTranslator;
import rreil.disassembler.translators.x86.common.LoopTranslator;
import rreil.disassembler.translators.x86.common.MovTranslator;
import rreil.disassembler.translators.x86.common.MovsTranslator;
import rreil.disassembler.translators.x86.common.MulTranslator;
import rreil.disassembler.translators.x86.common.NopTranslator;
import rreil.disassembler.translators.x86.common.PopTranslator;
import rreil.disassembler.translators.x86.common.PopfTranslator;
import rreil.disassembler.translators.x86.common.PopfwTranslator;
import rreil.disassembler.translators.x86.common.PushTranslator;
import rreil.disassembler.translators.x86.common.PushfTranslator;
import rreil.disassembler.translators.x86.common.PushfwTranslator;
import rreil.disassembler.translators.x86.common.RepTranslator;
import rreil.disassembler.translators.x86.common.RepeTranslator;
import rreil.disassembler.translators.x86.common.RepneTranslator;
import rreil.disassembler.translators.x86.common.RetnTranslator;
import rreil.disassembler.translators.x86.common.SahfTranslator;
import rreil.disassembler.translators.x86.common.ScasTranslator;
import rreil.disassembler.translators.x86.common.SetalcTranslator;
import rreil.disassembler.translators.x86.common.SetccTranslator;
import rreil.disassembler.translators.x86.common.StcTranslator;
import rreil.disassembler.translators.x86.common.StdTranslator;
import rreil.disassembler.translators.x86.common.StiTranslator;
import rreil.disassembler.translators.x86.common.StosTranslator;
import rreil.disassembler.translators.x86.common.X86Conditions;
import rreil.disassembler.translators.x86.common.X86RegRmTranslator;
import rreil.disassembler.translators.x86.common.X86RegTranslator;
import rreil.disassembler.translators.x86.common.X86RmRegBitmodTranslator;
import rreil.disassembler.translators.x86.common.X86RmRegRegTranslator;
import rreil.disassembler.translators.x86.common.X86RmRmNoWritebackTranslator;
import rreil.disassembler.translators.x86.common.X86RmRmTranslator;
import rreil.disassembler.translators.x86.common.X86RmTranslator;
import rreil.disassembler.translators.x86.common.XlatTranslator;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.OperandSize;

/**
 *
 * @author mb0
 */
public class X64Translator implements Translator {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  public static final X64Translator $ = new X64Translator();
  private static final HashMap<String, InsnTranslator> translators = new HashMap<String, InsnTranslator>();

  static {
    translators.put("adc", new X86RmRmTranslator(new X86RmRmTranslator.AdcEmitter()));
    translators.put("add", new X86RmRmTranslator(new X86RmRmTranslator.AddEmitter()));
    translators.put("and", new X86RmRmTranslator(new X86RmRmTranslator.AndEmitter()));
    translators.put("bswap", new X86RegTranslator(new X86RegTranslator.BswapEmitter()));
    translators.put("bt", new X86RmRmNoWritebackTranslator(new X86RmRmNoWritebackTranslator.BtEmitter()));
    translators.put("btc", new X86RmRegBitmodTranslator(new X86RmRegBitmodTranslator.BtcEmitter()));
    translators.put("btr", new X86RmRegBitmodTranslator(new X86RmRegBitmodTranslator.BtrEmitter()));
    translators.put("bts", new X86RmRegBitmodTranslator(new X86RmRegBitmodTranslator.BtsEmitter()));
    translators.put("bsf", new X86RegRmTranslator(new X86RegRmTranslator.BsfEmitter()));
    translators.put("bsr", new X86RegRmTranslator(new X86RegRmTranslator.BsrEmitter()));
    translators.put("call", new CallTranslator());
    translators.put("cdq", new CdqTranslator());
    translators.put("clc", new ClcTranslator());
    translators.put("cld", new CldTranslator());
    translators.put("cli", new CliTranslator());
    translators.put("cmc", new CmcTranslator());
    translators.put("cmova", new CmovccTranslator(X86Conditions.ABOVE));
    translators.put("cmovnb", new CmovccTranslator(X86Conditions.ABOVE_OR_EQUAL));
    translators.put("cmovb", new CmovccTranslator(X86Conditions.BELOW));
    translators.put("cmovbe", new CmovccTranslator(X86Conditions.BELOW_OR_EQUAL));
    translators.put("cmovz", new CmovccTranslator(X86Conditions.ZERO));
    translators.put("cmovg", new CmovccTranslator(X86Conditions.GREATER));
    translators.put("cmovge", new CmovccTranslator(X86Conditions.GREATER_OR_EQUAL));
    translators.put("cmovl", new CmovccTranslator(X86Conditions.LESS));
    translators.put("cmovle", new CmovccTranslator(X86Conditions.LESS_OR_EUQAL));
    translators.put("cmovnz", new CmovccTranslator(X86Conditions.NOT_ZERO));
    translators.put("cmovno", new CmovccTranslator(X86Conditions.NOT_OVERFLOW));
    translators.put("cmovnp", new CmovccTranslator(X86Conditions.NOT_PARITY));
    translators.put("cmovns", new CmovccTranslator(X86Conditions.NOT_SIGN));
    translators.put("cmovo", new CmovccTranslator(X86Conditions.OVERFLOW));
    translators.put("cmovp", new CmovccTranslator(X86Conditions.PARITY));
    translators.put("cmovs", new CmovccTranslator(X86Conditions.SIGN));
    translators.put("cmpsb", new CmpsTranslator(OperandSize.BYTE));
    translators.put("cmpsd", new CmpsTranslator(OperandSize.DWORD));
    translators.put("cmpsw", new CmpsTranslator(OperandSize.WORD));
    translators.put("cmp", new X86RmRmNoWritebackTranslator(new X86RmRmNoWritebackTranslator.CmpEmitter()));
    translators.put("cwd", new CwdTranslator());
    translators.put("cwde", new CwdeTranslator());
    translators.put("cdqe", new CdqeTranslator());
    translators.put("dec", new DecTranslator());
    translators.put("div", new DivTranslator());
    translators.put("imul", new ImulTranslator());
    translators.put("idiv", new IdivTranslator());
    translators.put("inc", new IncTranslator());
    translators.put("ja", new JccTranslator(X86Conditions.ABOVE));
    translators.put("jb", new JccTranslator(X86Conditions.BELOW));
    translators.put("jbe", new JccTranslator(X86Conditions.BELOW_OR_EQUAL));
    translators.put("jcxz", new JccTranslator(X86Conditions.CX_ZERO));
    translators.put("jecxz", new JccTranslator(X86Conditions.ECX_ZERO));
    translators.put("je", new JccTranslator(X86Conditions.ZERO));
    translators.put("jg", new JccTranslator(X86Conditions.GREATER));
    translators.put("jnle", new JccTranslator(X86Conditions.GREATER));
    translators.put("jge", new JccTranslator(X86Conditions.GREATER_OR_EQUAL));
    translators.put("jl", new JccTranslator(X86Conditions.LESS));
    translators.put("jle", new JccTranslator(X86Conditions.LESS_OR_EUQAL));
    translators.put("jmp", new JccTranslator(X86Conditions.TRUE));
    translators.put("jnb", new JccTranslator(X86Conditions.ABOVE_OR_EQUAL));
    translators.put("jne", new JccTranslator(X86Conditions.NOT_ZERO));
    translators.put("jno", new JccTranslator(X86Conditions.NOT_OVERFLOW));
    translators.put("jnp", new JccTranslator(X86Conditions.NOT_PARITY));
    translators.put("jns", new JccTranslator(X86Conditions.NOT_SIGN));
    translators.put("jnz", new JccTranslator(X86Conditions.NOT_ZERO));
    translators.put("jo", new JccTranslator(X86Conditions.OVERFLOW));
    translators.put("jp", new JccTranslator(X86Conditions.PARITY));
    translators.put("js", new JccTranslator(X86Conditions.SIGN));
    translators.put("jz", new JccTranslator(X86Conditions.ZERO));
    translators.put("lahf", new LahfTranslator());
    translators.put("lea", new LeaTranslator());
    translators.put("leave", new LeaveTranslator());
    translators.put("lodsb", new LodsTranslator(OperandSize.BYTE));
    translators.put("lodsw", new LodsTranslator(OperandSize.WORD));
    translators.put("lodsd", new LodsTranslator(OperandSize.DWORD));
    translators.put("loop", new LoopTranslator(LoopTranslator.LOOP));
    translators.put("loope", new LoopTranslator(LoopTranslator.LOOPE));
    translators.put("loopne", new LoopTranslator(LoopTranslator.LOOPNE));
    translators.put("mov", new MovTranslator());
    translators.put("movsb", new MovsTranslator(OperandSize.BYTE));
    translators.put("movsw", new MovsTranslator(OperandSize.WORD));
    translators.put("movsd", new MovsTranslator(OperandSize.DWORD));
    translators.put("movsx", new X86RegRmTranslator(new X86RegRmTranslator.MovsxEmitter()));
    translators.put("movsxd", new X86RegRmTranslator(new X86RegRmTranslator.MovsxEmitter()));
    translators.put("movzx", new X86RegRmTranslator(new X86RegRmTranslator.MovzxEmitter()));
    translators.put("mul", new MulTranslator());
    translators.put("neg", new X86RmTranslator(new X86RmTranslator.NegEmitter()));
    translators.put("nop", new NopTranslator());
    translators.put("not", new X86RmTranslator(new X86RmTranslator.NotEmitter()));
    translators.put("or", new X86RmRmTranslator(new X86RmRmTranslator.OrEmitter()));
    translators.put("pop", new PopTranslator(new PopTranslator.PopEmitter()));
    //translators.put("popa", new PopaTranslator());
    //translators.put("popaw", new PopawTranslator());
    translators.put("popf", new PopfTranslator());
    translators.put("popfw", new PopfwTranslator());
    translators.put("push", new PushTranslator());
    //translators.put("pusha", new PushaTranslator());
    //translators.put("pushaw", new PushawTranslator());
    translators.put("pushf", new PushfTranslator());
    translators.put("pushfw", new PushfwTranslator());
    translators.put("repz lodsb", new RepTranslator(new LodsTranslator(OperandSize.BYTE)));
    translators.put("repz lodsw", new RepTranslator(new LodsTranslator(OperandSize.WORD)));
    translators.put("repz lodsd", new RepTranslator(new LodsTranslator(OperandSize.DWORD)));
    translators.put("repz movsb", new RepTranslator(new MovsTranslator(OperandSize.BYTE)));
    translators.put("repz movsw", new RepTranslator(new MovsTranslator(OperandSize.WORD)));
    translators.put("repz movsd", new RepTranslator(new MovsTranslator(OperandSize.DWORD)));
    translators.put("repz stosb", new RepTranslator(new StosTranslator(OperandSize.BYTE)));
    translators.put("repz stosw", new RepTranslator(new StosTranslator(OperandSize.WORD)));
    translators.put("repz stosd", new RepTranslator(new StosTranslator(OperandSize.DWORD)));
    translators.put("repz cmpsb", new RepeTranslator(new CmpsTranslator(OperandSize.BYTE)));
    translators.put("repz cmpsw", new RepeTranslator(new CmpsTranslator(OperandSize.WORD)));
    translators.put("repz cmpsd", new RepeTranslator(new CmpsTranslator(OperandSize.DWORD)));
    translators.put("repz scasb", new RepeTranslator(new ScasTranslator(OperandSize.BYTE)));
    translators.put("repz scasw", new RepeTranslator(new ScasTranslator(OperandSize.WORD)));
    translators.put("repz scasd", new RepeTranslator(new ScasTranslator(OperandSize.DWORD)));
    translators.put("repnz cmpsb", new RepneTranslator(new CmpsTranslator(OperandSize.BYTE)));
    translators.put("repnz cmpsw", new RepneTranslator(new CmpsTranslator(OperandSize.WORD)));
    translators.put("repnz cmpsd", new RepneTranslator(new CmpsTranslator(OperandSize.DWORD)));
    translators.put("repnz scasb", new RepneTranslator(new ScasTranslator(OperandSize.BYTE)));
    translators.put("repnz scasw", new RepneTranslator(new ScasTranslator(OperandSize.WORD)));
    translators.put("repnz scasd", new RepneTranslator(new ScasTranslator(OperandSize.DWORD)));
    translators.put("ret", new RetnTranslator());
    translators.put("retn", new RetnTranslator());
    translators.put("rol", new X86RmRmTranslator(new X86RmRmTranslator.RolEmitter()));
    translators.put("ror", new X86RmRmTranslator(new X86RmRmTranslator.RorEmitter()));
    translators.put("rcl", new X86RmRmTranslator(new X86RmRmTranslator.RclEmitter()));
    translators.put("rcr", new X86RmRmTranslator(new X86RmRmTranslator.RcrEmitter()));
    translators.put("sahf", new SahfTranslator());
    translators.put("sal", new X86RmRmTranslator(new X86RmRmTranslator.ShlEmitter()));
    translators.put("sar", new X86RmRmTranslator(new X86RmRmTranslator.SarEmitter()));
    translators.put("sbb", new X86RmRmTranslator(new X86RmRmTranslator.SbbEmitter()));
    translators.put("scasb", new ScasTranslator(OperandSize.BYTE));
    translators.put("scasw", new ScasTranslator(OperandSize.WORD));
    translators.put("scasd", new ScasTranslator(OperandSize.DWORD));
    translators.put("setalc", new SetalcTranslator());
    translators.put("setb", new SetccTranslator(X86Conditions.BELOW));
    translators.put("setbe", new SetccTranslator(X86Conditions.BELOW_OR_EQUAL));
    translators.put("sete", new SetccTranslator(X86Conditions.ZERO));
    translators.put("setl", new SetccTranslator(X86Conditions.LESS));
    translators.put("setle", new SetccTranslator(X86Conditions.LESS_OR_EUQAL));
    translators.put("setnb", new SetccTranslator(X86Conditions.ABOVE_OR_EQUAL));
    translators.put("setnbe", new SetccTranslator(X86Conditions.ABOVE));
    translators.put("setne", new SetccTranslator(X86Conditions.NOT_ZERO));
    translators.put("setnl", new SetccTranslator(X86Conditions.GREATER_OR_EQUAL));
    translators.put("setnle", new SetccTranslator(X86Conditions.GREATER));
    translators.put("setno", new SetccTranslator(X86Conditions.NOT_OVERFLOW));
    translators.put("setnp", new SetccTranslator(X86Conditions.NOT_PARITY));
    translators.put("setns", new SetccTranslator(X86Conditions.NOT_SIGN));
    translators.put("setnz", new SetccTranslator(X86Conditions.NOT_ZERO));
    translators.put("seto", new SetccTranslator(X86Conditions.OVERFLOW));
    translators.put("setp", new SetccTranslator(X86Conditions.PARITY));
    translators.put("sets", new SetccTranslator(X86Conditions.SIGN));
    translators.put("setz", new SetccTranslator(X86Conditions.ZERO));
    translators.put("shl", new X86RmRmTranslator(new X86RmRmTranslator.ShlEmitter()));
    translators.put("shr", new X86RmRmTranslator(new X86RmRmTranslator.ShrEmitter()));
    translators.put("shld", new X86RmRegRegTranslator(new X86RmRegRegTranslator.ShldEmitter()));
    translators.put("shrd", new X86RmRegRegTranslator(new X86RmRegRegTranslator.ShrdEmitter()));
    translators.put("stc", new StcTranslator());
    translators.put("std", new StdTranslator());
    translators.put("sti", new StiTranslator());
    translators.put("stosb", new StosTranslator(OperandSize.BYTE));
    translators.put("stosw", new StosTranslator(OperandSize.WORD));
    translators.put("stosd", new StosTranslator(OperandSize.DWORD));
    translators.put("sub", new X86RmRmTranslator(new X86RmRmTranslator.SubEmitter()));
    translators.put("test", new X86RmRmNoWritebackTranslator(new X86RmRmNoWritebackTranslator.TestEmitter()));
    translators.put("xadd", new X86RmRmTranslator(new X86RmRmTranslator.XaddEmitter()));
    translators.put("xchg", new X86RmRmTranslator(new X86RmRmTranslator.XchgEmitter()));
    translators.put("xlat", new XlatTranslator());
    translators.put("xor", new X86RmRmTranslator(new X86RmRmTranslator.XorEmitter()));
  }

  private X64Translator () {
  }

  /**
   * Translates an x86 instruction to RREIL code
   *
   * @param ctx A valid translation environment
   * @param insn The x86 instruction to translate
   * @return The list of REIL instruction the x86 instruction was translated to
   */
  public List<LowLevelRReil> translateRReil (final TranslationCtx ctx, final Instruction insn) throws TranslationException {
    String mnemonic = insn.mnemonic();
    ArrayList<LowLevelRReil> instructions = new ArrayList<LowLevelRReil>();
    if (translators.containsKey(mnemonic)) {
      InsnTranslator translator = translators.get(mnemonic);
      translator.translate(ctx, insn, instructions);
    } else {
      // translate to a generic native instruction without semantics
      instructions.add(factory.NATIVE(RReilAddr.valueOf(insn.baseAddress()), insn));
    }
    return instructions;
  }

  @Override public List<LowLevelRReil> translate (final TranslationCtx ctx, final Instruction instruction) throws TranslationException {
    assert ctx != null : "null ctx";
    assert instruction != null : "null instruction";
    return translateRReil(ctx, instruction);
  }
}
