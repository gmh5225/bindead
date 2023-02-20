package rreil.interpreter;

import javalx.numeric.BigInt;
import rreil.disassembler.translators.common.RegisterTranslator;
import rreil.lang.Rhs.Rvar;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import rreil.lang.util.LowLevelToRReilTranslator;

/**
 *
 * @author mb0
 */
public class InterpCtx {
  private static LowLevelRReilFactory factory = LowLevelRReilFactory.getInstance();

  private final MemoryModel memory;
  private final RegisterModel registers;
  private final RegisterTranslator registerTranslator;

  public InterpCtx (int baseSize, RegisterTranslator registerTranslator, MemoryModel memory) {
    this(baseSize, registerTranslator, new RegisterModel(baseSize), memory);
  }

  public InterpCtx (int baseSize, RegisterTranslator registerTranslator, RegisterModel registerModel, MemoryModel memory) {
    this.registerTranslator = registerTranslator;
    this.memory = memory;
    this.registers = registerModel;
  }

  public MemoryModel getMemory () {
    return memory;
  }

  public RegisterModel getRegisters () {
    return registers;
  }

  public RegisterTranslator getRegisterTranslator () {
    return registerTranslator;
  }

  public void set (final String name, final BigInt value) {
    final LowLevelRReilOpnd register = registerTranslator.translateRegister(name);
    final Rvar rreilVariable = LowLevelToRReilTranslator.translateRvar(register);
    registers.set(rreilVariable.asLhs(), value);
  }

  public BigInt get (final String name) {
    final LowLevelRReilOpnd register = registerTranslator.translateRegister(name);
    final Rvar rreilVariable = LowLevelToRReilTranslator.translateRvar(register);
    return registers.get(rreilVariable);
  }

  public BigInt getBool (final String name) {
    final Rvar rreilVariable = LowLevelToRReilTranslator.translateRvar(factory.flag(name));
    return registers.get(rreilVariable);
  }

  public void setBool (final String name, final BigInt value) {
    final Rvar rreilVariable = LowLevelToRReilTranslator.translateRvar(factory.flag(name));
    registers.set(rreilVariable, value);
  }

  public BigInt getSigned (final String name) {
    final LowLevelRReilOpnd register = registerTranslator.translateRegister(name);
    final Rvar rreilVariable = LowLevelToRReilTranslator.translateRvar(register);
    return registers.getSigned(rreilVariable);
  }

  public BigInt load (final int size, final long address) {
    return memory.load(size, address);
  }

  public void store (int size, long address, BigInt value) {
    memory.store(size, address, value);
  }
}
