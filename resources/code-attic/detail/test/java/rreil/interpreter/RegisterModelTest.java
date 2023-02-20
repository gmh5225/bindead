package rreil.interpreter;

import javalx.data.BigInt;
import org.junit.Test;
import rreil.abstractsyntax.Rhs.Rvar;
import rreil.abstractsyntax.util.RhsFactory;
import rreil.abstractsyntax.util.RhsFactory;

import static junit.framework.Assert.assertEquals;

/**
 *
 * @author mb0
 */
public class RegisterModelTest {
  private static final RhsFactory rreil = RhsFactory.getInstance();
  private final Rvar r1_32_0 = rreil.variable(32, 0, 1);
  private final Rvar r1_8_0 = rreil.variable(8, 0, 1);
  private final Rvar r1_8_8 = rreil.variable(8, 8, 1);
  private final Rvar r1_8_16 = rreil.variable(8, 16, 1);
  private final Rvar r1_16_0 = rreil.variable(16, 0, 1);
  private final Rvar r2_32_0 = rreil.variable(32, 0, 2);

  @Test public void store001 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_32_0, BigInt.ZERO);
    registers.set(r2_32_0, BigInt.ONE);
    assertEquals(BigInt.ZERO, registers.get(r1_32_0));
    assertEquals(BigInt.ONE, registers.get(r2_32_0));
  }

  @Test public void store002 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_32_0, BigInt.MINUSONE);
    assertEquals(BigInt.valueOf("FFFFFFFF", 16), registers.get(r1_32_0));
  }

  @Test public void store003 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_32_0, BigInt.MINUSONE);
    assertEquals(BigInt.MINUSONE, registers.getSigned(r1_32_0));
  }

  @Test public void store004 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_8_0, BigInt.valueOf("80", 16));
    assertEquals(BigInt.valueOf(-128), registers.getSigned(r1_8_0));
  }

  @Test public void store005 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_8_0, BigInt.valueOf("7F", 16));
    assertEquals(BigInt.valueOf(127), registers.getSigned(r1_8_0));
  }

  @Test public void store006 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_32_0, BigInt.MINUSONE);  // store 32bit value
    assertEquals(BigInt.MINUSONE, registers.getSigned(r1_8_0)); // retrieve 8bit value
  }

  @Test public void store007 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_32_0, BigInt.MINUSONE);  // store 32bit value
    assertEquals(BigInt.MINUSONE, registers.getSigned(r1_8_8)); // retrieve 8bit value with offset
  }

  @Test public void store008 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_8_0, BigInt.MINUSONE);
    assertEquals(BigInt.valueOf("FF", 16), registers.get(r1_8_0));
  }

  @Test public void store009 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_16_0, BigInt.valueOf("FFFF", 16));
    registers.set(r1_8_8, BigInt.valueOf("00"));
    assertEquals(BigInt.valueOf("00FF", 16), registers.get(r1_16_0));
  }

  @Test public void store010 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_16_0, BigInt.valueOf("FFFF", 16));
    registers.set(r1_8_0, BigInt.valueOf("00"));
    assertEquals(BigInt.valueOf("FF00", 16), registers.get(r1_16_0));
  }

  @Test public void store011 () {
    final RegisterModel registers = new RegisterModel(32);
    registers.set(r1_32_0, BigInt.valueOf("DEADBEAF", 16));
    registers.set(r1_8_16, BigInt.valueOf("AF", 16));
    assertEquals(BigInt.valueOf("DEAFBEAF", 16), registers.get(r1_32_0));
  }
}
