package bindead.domains;

import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;
import static bindead.debug.DebugHelper.logln;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import javalx.data.Option;
import javalx.numeric.BigInt;
import javalx.numeric.Interval;

import org.junit.Test;

import rreil.lang.util.Type;
import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.analyses.DomainFactory;
import bindead.data.NumVar;
import bindead.domainnetwork.combinators.ZenoChildOp;
import bindead.domainnetwork.combinators.ZenoChildOp.Sequence;
import bindead.domainnetwork.interfaces.ZenoDomain;

/**
 * Tests for the Wrapping Domain.
 *
 * @author Bogdan Mihaila
 */
public class Wrapping {

  @Test public void test001 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, [0, 255]",
        "cmples LE, x, 0",
        "brc LE, exit_le:",
        "mov x, x",
        "assert x = [1, 127]",
        "br exit:",
        "exit_le: ",
        "mov x, x",
        "assert x = [-128, 0]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void test002 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, [0, 255]",
        "cmplts LE, x, 0",
        "brc LE, exit_le:",
        "mov x, x",
        "assert x = [0, 127]",
        "br exit:",
        "exit_le: ",
        "mov x, x",
        "assert x = [-128, -1]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void test003 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, [-1, 1]",
        "cmpleu LT, x, 0",
        "brc LT, exit_lt:",
        "mov x, x",
        "assert x = [1, 255]",
        "br exit:",
        "exit_lt: ",
        "mov x, x",
        "assert x = 0",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void test004 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 64",
        "mov x, ?",
        "cmpltu LT, x, -4096",
        "brc LT, exit_lt:",
        "mov x, x",
        "assert x = [18446744073709547520, 18446744073709551615]",
        "br exit:",
        "exit_lt: ",
        "mov x, x",
        "assert x = [0, 18446744073709547519]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Found in GCC initialization code.
   */
  @Test public void mulWrapEqualityTest () {
    String assembly = lines(
        "option DEFAULT_SIZE = 64",
        "mov r1, -6148914691236517205", // 0xaaaaaaaaaaaaaaab in two's complement
        "mov r2, 36",
        "mul r1, r1, r2",
        "cmpeq f, r1, 12", // r1 should be 12 when wrapped
        "brc f, true:",
        "false:",
        "mov r0, 0",
        "brc 1, exit:",
        "true:",
        "mov r0, 1",
        "brc 1, exit:",
        "nop",
        "exit:",
        "assert r0 = 1",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Found in GCC initialization code.
   */
  @Test public void mulWrapInequalityTest () {
    String assembly = lines(
        "option DEFAULT_SIZE = 64",
        "mov r1, -6148914691236517205", // 0xaaaaaaaaaaaaaaab in two's complement
        "mov r2, 36",
        "mul r1, r1, r2",
        "cmpeq f, r1, 13", // r1 should be 12 when wrapped
        "brc f, true:",
        "false:",
        "mov r0, 0",
        "brc 1, exit:",
        "true:",
        "mov r0, 1",
        "brc 1, exit:",
        "exit:",
        "assert r0 = 0",
        "halt");
    evaluateAssertions(assembly);
  }

  @Test public void topEqualityTest () {
    String domainHierarchy = "Predicates(F) Wrapping Affine Intervals";
    RichFiniteDomain d = FiniteDomainHelper.for32bitVars(DomainFactory.parseFiniteDomain(domainHierarchy));
    NumVar x1 = NumVar.fresh("x1");
    NumVar x2 = NumVar.fresh("x2");
    d = d.introduce(x1);
    d = d.introduce(x2);
    d = d.assign(x2, Interval.of(0, 5));
    logln(d);
    logln("x1 == x2");
    d = d.equalTo(x1, x2);
    logln(d);
    d.assertValueIs(x1, Interval.of(0, 5));
    d.assertValueIs(x2, Interval.of(0, 5));
  }

  /**
   * Check that anding with a mask 0011...1100 resets the lower bits.
   */
  @Test public void andTest001 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, [2,62]",
        "shr r2, r1, 2",
        "mul r2, r2, 4",
        "assert r2 = [0,60]",
        "and r1, r1, 124", // 01111100, 0x7C
        "assert r1 = [0,60]",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Check that anding with a mask 0011...1111 resets the upper bits.
   */
  @Test public void andTest002 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, [57,62]",
        "and r1, r1, 15", // 00001111, 0x0F
        "assert r1 = [9,14]",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Check that anding with a mask 0011...1100 resets upper and lower bits.
   */
  @Test public void andTest003 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, [57,62]",
        "and r1, r1, 12", // 00001100, 0x0C
        "assert r1 = [8,12]",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Check that anding two constants is exact.
   */
  @Test public void andTest004 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, 14883",
        "and r1, r1, 10860",
        "assert r1 = 10784",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Check that anding remove the lowest bit works.
   */
  @Test public void andTest005 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 16",
        "mov r1, [14882,14883]",
        "and r1, r1, 10860",
        "assert r1 = 14880",
        "halt");
    evaluateAssertions(assembly);
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) @Test public void wrapToBool1 () {
    ZenoDomain d = DomainFactory.parseZenoDomain("Affine Congruences Intervals");
    NumVar x = NumVar.freshFlag("x");
    d = d.introduce(x, Type.Zeno, Option.some(BigInt.of(9)));
    logln(d);
    logln("wrap in [0, 1]");
    Interval oldWrap = Interval.of(9).wrap(Interval.of(0, 1));
    logln("old way:");
    logln(oldWrap);
    assertThat(oldWrap, is(Interval.of(1)));
    logln("new way:");
    Sequence childOp = new ZenoChildOp.Sequence();
    childOp.addWrap(x, Interval.of(0, 1), false);
    d = childOp.apply(d);
    logln(d);
    assertThat(d.queryRange(x).convexHull(), is(Interval.of(1)));
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) @Test public void wrapToBool2 () {
    ZenoDomain d = DomainFactory.parseZenoDomain("ThresholdsWidening Affine Congruences Intervals");
    NumVar x = NumVar.freshFlag("x");
    ZenoFactory zeno = ZenoFactory.getInstance();
    d = d.introduce(x, Type.Zeno, Option.<BigInt>none());
    d = d.eval(zeno.assign(zeno.variable(x), zeno.range(Interval.of(9, 10))));
    logln(d);
    logln("wrap in [0, 1]");
    Interval oldWrap = Interval.of(9, 10).wrap(Interval.of(0, 1));
    logln("old way:");
    logln(oldWrap);
    assertThat(oldWrap, is(Interval.of(0, 1)));
    logln("new way:");
    Sequence childOp = new ZenoChildOp.Sequence();
    childOp.addWrap(x, Interval.of(0, 1), false);
    d = childOp.apply(d);
    logln(d);
    assertThat(d.queryRange(x).convexHull(), is(Interval.of(0, 1)));
  }

  /**
   * Tests the wrapping for a variable being outside the values range of its type.
   * Found as bug in a binary.
   */
  @Test public void bugFromBinary001 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [2147483648, 4294967295]",
        "cmplts f, x, 0",
        "brc f, lower:",
          "assert UNREACHABLE",
        "br exit:",
        "lower:",
          "assert x = [-2147483648, -1]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Same as {@link #bugFromBinary001()} but with a smaller values range to be easier to understand.
   */
  @Test public void bugFromBinary002 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 4", // signed values range is [-8, 7]
        "mov x, [8, 15]",
        "cmplts f, x, 0",
        "brc f, lower:",
          "assert UNREACHABLE",
        "br exit:",
        "lower:",
          "assert x = [-8, -1]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Variation from {@link #bugFromBinary002()} with a different value and thus different outcome.
   */
  @Test public void bugFromBinary003 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 4", // signed values range is [-8, 7]
        "mov x, [8, 17]",          // with value 16, 17 there are bit patterns that satisfy the higher branch, too
        "cmplts f, x, 0",
        "brc f, lower:",
          "assert x = [0, 1]",
        "br exit:",
        "lower:",
          "assert x = [-8, -1]",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Difference between (unsigned) tests because of wrapping.
   * For x = [0, 1] testing x > 2 is unreachable but testing
   * x - 1 > 1 is reachable due to wrapping
   * ("0 - 1" wrapped gives a high positive number that is bigger than 1).
   */
  @Test public void wrappingOnTransformedTests () {
    String assembly = lines(
        "option DEFAULT_SIZE = 8",
        "mov x, [0, 1]",
        "cmpltu LT, 2, x",
        "brc LT, lower2:",
        "  higher2:",
        "  assert x = [0, 1]",
        "  br next_test:",
        "lower2:",
        "  assert UNREACHABLE",
        "next_test:",
        "sub x, x, 1",
        "cmpltu LT, 1, x",
        "brc LT, lower1:",
        "  higher1:",
        "  assert x = 0", // compared to the above here the [-1, 0] is reduced to 0 due to wrapping
        "  br exit:",
        "lower1:",
        "  assert x = 255", // -1 wrapped around in 8 bits wide numbers
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Applying an unsigned test to a TOP value should not
   * constrain the value range to unsigned and thus make
   * subsequent signed tests possibly unsatisfiable.
   * This was only an issue because of wrong syntactic entailment
   * in Finite predicates. Now should not occur anymore.
   */
  @Test public void wrappingWithPredicatesEntailment () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-oo, +oo]",
        // compare x with 0 and set the flags
        "cmpltu CF, x, 0",
        "cmpleu BE, x, 0",
        "cmplts LT, x, 0",
        "cmples LE, x, 0",
        "cmpeq  ZF, x, 0",
        "cmplts SF, x, 0",
        "xor.1 OF, LT, SF",
        "brc ZF, zero:",
        "  assert.1 ZF = 0",
        "  assert.1 CF = [0, 1]",
        "  assert.1 BE = [0, 1]",
        "  assert.1 LE = [0, 1]",
        "  assert.1 LT = [0, 1]",
        "xor.1 NLE, LE, 1",
        "brc NLE, greater:",
        "  assert REACHABLE",
        "  assert.1 NLE = 0",
        "br exit:",
        "greater:",
        "  assert REACHABLE",
        "  assert.1 NLE = 1",
        "br exit:",
        "zero:",
        "assert x = 0",
        "assert.1 ZF = 1",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Applying a disequality test {@code x != 0} to a value should not change
   * the value with convex numeric domains. Wrapping though does change it when it is unbounded.
   */
  @Test public void wrappingWithDisequality () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [-1, 1]",
        "cmpeq  ZF, x, 0",
        "brc ZF, zero:",
        "  assert.1 ZF = 0",
        "  assert x = [-1, 1]",
        "br exit:",
        "zero:",
        "assert.1 ZF = 1",
        "assert x = 0",
        // now with TOP
        "mov x, [-1, +oo]",
        "cmpeq  ZF, x, 0",
        "brc ZF, zero2:",
        "  assert.1 ZF = 0",
        "  assert x = [1, 4294967295]",
        "br exit:",
        "zero2:",
        "assert.1 ZF = 1",
        "assert x = 0",
        "exit:",
        "halt");
    evaluateAssertions(assembly);
  }

}
