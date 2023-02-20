package rreil.interpreter;

import java.util.HashMap;
import java.util.Map;

import bindis.Disassembler;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;
import rreil.disassembler.Instruction;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.Rhs.Rvar;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.util.RhsFactory;

/**
 * A virtual machine for executing {@code RReil} instruction.
 */
public class RReilMachine {
  private static final int $DefaultPcBitWidth = 32;
  private static final RhsFactory exprs = RhsFactory.getInstance();
  private final Disassembler dis;
  private final Rvar pc;

  public RReilMachine (Disassembler dis, int pcBitWidth) {
    this.dis = dis;
    pc = exprs.variable(pcBitWidth, 0, MemVar.getVarOrFresh("$pc"));
  }

  public RReilMachine (Disassembler dis) {
    this(dis, $DefaultPcBitWidth);
  }

  public void evalOne (final MachineCtx ctx) {
    Instruction instruction = dis.decodeOne(ctx.getCode(), ctx.getOffset(), ctx.getStartPc());
    run(buildInsnMap(instruction), ctx);
  }

  public void initPc (final MachineCtx ctx) {
    // ctx.getCtx().getRegisters().set(pc, BigInt.valueOf(ctx.getStartPc()));
    // myPc = ctx.getStartPc();

    ctx.getCtx().getRegisters().set(pc, BigInt.of(ctx.getStartPc()));
  }

  public void step (final MachineCtx ctx) {
    BigInt currentPC = ctx.getCtx().getRegisters().get(pc);
    Instruction instruction = dis.decodeOne(ctx.getCode(), currentPC.intValue(), currentPC.longValue());
    currentPC = currentPC.add(BigInt.of(instruction.opcode().length));
    ctx.getCtx().getRegisters().set(pc, currentPC);
    System.out.println(instruction);
    run(buildInsnMap(instruction), ctx);
  }

  private void run (final Map<Integer, RReil> code, final MachineCtx ctx) {
    final RReilInterp interp = new RReilInterp(pc);
    // final RegisterModel registers = ctx.getCtx().getRegisters();
    // registers.set(pc.asLhs(), BigInt.ZERO);
    BigInt currentPc = Bound.ZERO;
    for (;;) {
      // Fetch instruction and (pre) update program counter
      // final BigInt currentPc = registers.get(pc);
      final RReil insn = code.get(currentPc.intValue());
      // registers.set(pc.asLhs(), currentPc.add(BigInt.ONE));
      currentPc = currentPc.add(Bound.ONE);
      if (insn == null)
        break;
      interp.run(insn, ctx.getCtx());
    }
  }

  private static Map<Integer, RReil> buildInsnMap (Instruction instruction) {
    Map<Integer, RReil> code = new HashMap<Integer, RReil>();
    for (LowLevelRReil insn : instruction.toRReilInstructions()) {
      final RReil rreil = insn.toRReil();
      code.put(insn.address().offset(), rreil);
    }
    return code;
  }
}
