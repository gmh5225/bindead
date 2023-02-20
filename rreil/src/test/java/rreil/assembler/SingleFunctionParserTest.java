package rreil.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static rreil.assembler.TestsHelper.lines;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Test;

import rreil.assembler.SingleFunctionParser.SizedVariable;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.RReilParser;
import rreil.lang.BinOp;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.util.RReilFactory;

public class SingleFunctionParserTest {
  private static final RReilFactory rreil = new RReilFactory();

  private static final int SIZE = 32;
  private static final List<SizedVariable> inputVars = list(new SizedVariable("ebx", SIZE), new SizedVariable("ecx",
    SIZE), new SizedVariable("edx", SIZE), new SizedVariable("esi", SIZE), new SizedVariable("edi", SIZE),
      new SizedVariable("ebp", SIZE));
  private static final List<SizedVariable> outputVars = list(new SizedVariable("eax", SIZE));


  @Test public void test_001 () throws Throwable {
    // Input
    String functionDefinition = lines(
        "def (out1.S) = doSth <S> (in1.S, in2.S) {",
        "mov.S t0, in1",
        "mul.S t1, in2, t0",
        "mov.S out1, t1",
        "}"
      );

    Map<String, Integer> templateParams = new HashMap<String, Integer>();
    templateParams.put("S", SIZE);

    // Expected
    SortedMap<RReilAddr, RReil> expected =
      instructions(
          rreil.assign(addr(0), local(SIZE, "t0").asLhs(), variable(SIZE, "ebx")),
          rreil.assign(addr(1), local(SIZE, "t1").asLhs(),
              rreil.binary(variable(SIZE, "ecx"), BinOp.Mul, local(SIZE, "t0"))),
          rreil.assign(addr(2), variable(SIZE, "eax").asLhs(), local(SIZE, "t1"))
      );

    parseAndCompare(functionDefinition, expected, templateParams);
  }

  @Test public void test_002 () throws Throwable {
    // Input
    String functionDefinition = lines(
        "def (out1) = doSth <S> (in1.S, in2.S) {",
        "mov.S t0, in1",
        "mul.S t1, in2, t0",
        "mov.S out1, t1",
        "}"
      );

    Map<String, Integer> templateParams = new HashMap<String, Integer>();
    templateParams.put("S", SIZE);

    // Expected
    SortedMap<RReilAddr, RReil> expected =
      instructions(
          rreil.assign(addr(0), local(SIZE, "t0").asLhs(), variable(SIZE, "ebx")),
          rreil.assign(addr(1), local(SIZE, "t1").asLhs(),
              rreil.binary(variable(SIZE, "ecx"), BinOp.Mul, local(SIZE, "t0"))),
          rreil.assign(addr(2), variable(SIZE, "eax").asLhs(), local(SIZE, "t1"))
      );
    try {
      parseAndCompare(functionDefinition, expected, templateParams);
      fail("Expected ParseException on missing size for out1!");
    } catch (ParseException err) {
      // Success!
    }
  }

  @Test public void test_003 () throws Throwable {
    // Input
    String functionDefinition = lines(
        "def (out1.32) = doSth (in1.32, in2.32) {",
        "mov.d t0, in1",
        "mul.32 t1, in2, t0",
        "mov.d out1, t1",
        "}"
      );

    // Expected
    SortedMap<RReilAddr, RReil> expected =
      instructions(
          rreil.assign(addr(0), local(SIZE, "t0").asLhs(), variable(SIZE, "ebx")),
          rreil.assign(addr(1), local(SIZE, "t1").asLhs(),
              rreil.binary(variable(SIZE, "ecx"), BinOp.Mul, local(SIZE, "t0"))),
          rreil.assign(addr(2), variable(SIZE, "eax").asLhs(), local(SIZE, "t1"))
      );

    parseAndCompare(functionDefinition, expected, null);
  }

  @Test public void test_004 () throws Throwable {
    // Input
    String functionDefinition = lines(
        "def (out1.int8) = doSth <int8> (in1.int8, in2.int8) {",
        "mov.int8 t0, in1",
        "mul.int8 t1, in2, t0",
        "mov.int8 out1, t1",
        "}"
      );

    Map<String, Integer> templateParams = new HashMap<String, Integer>();
    templateParams.put("int8", SIZE);

    // Expected
    SortedMap<RReilAddr, RReil> expected =
      instructions(
          rreil.assign(addr(0), local(SIZE, "t0").asLhs(), variable(SIZE, "ebx")),
          rreil.assign(addr(1), local(SIZE, "t1").asLhs(),
              rreil.binary(variable(SIZE, "ecx"), BinOp.Mul, local(SIZE, "t0"))),
          rreil.assign(addr(2), variable(SIZE, "eax").asLhs(), local(SIZE, "t1"))
      );

    parseAndCompare(functionDefinition, expected, templateParams);
  }


  private static void parseAndCompare (String functionDefinition, SortedMap<RReilAddr, RReil> expected,
      Map<String, Integer> templateParams) throws ParseException {
    parseAndCompare(functionDefinition, expected, inputVars, outputVars, templateParams);
  }

  private static void parseAndCompare (String functionDefinition, SortedMap<RReilAddr, RReil> expected,
      List<SizedVariable> inputVars, List<SizedVariable> outputVars, Map<String, Integer> templateParams)
      throws ParseException {
    // Parse
    RReilParser tree = new RReilParser(new StringReader(functionDefinition));
    ASTModule module = tree.Module();

    SingleFunctionParser parser = new SingleFunctionParser(inputVars, outputVars, templateParams);
    SortedMap<RReilAddr, RReil> actual = parser.instantiateFunctionTemplate(module);

    compare(expected, actual);
  }


  private static void compare (SortedMap<RReilAddr, RReil> expected, SortedMap<RReilAddr, RReil> actual) {
    assertEquals(expected.size(), actual.size());

    final RReilAddr lastAddr = expected.lastKey();
    RReilAddr addr = expected.firstKey();
    for (; !addr.equals(lastAddr); addr = addr.nextBase()) {
      final RReil actInst = actual.get(addr);
      final RReil expInst = expected.get(addr);
      assertEqInst(expInst, actInst);
    }
  }

  private static void assertEqInst (RReil expected, RReil actual) {
    assertEquals(expected.getRReilAddress(), actual.getRReilAddress());
    assertEquals(expected.isAlwaysTakenBranch(), actual.isAlwaysTakenBranch());
    assertEquals(expected.isBranch(), actual.isBranch());
    assertEquals(expected.isConditionalBranch(), actual.isConditionalBranch());
    assertEquals(expected.isDeadBranch(), actual.isDeadBranch());
    assertEquals(expected.isIndirectBranch(), actual.isIndirectBranch());
    assertEquals(expected.toString(), actual.toString());
  }


  private static RReilAddr addr (long base) {
    return RReilAddr.valueOf(base);
  }

  private static Rhs.Rvar variable (int size, String name) {
    return rreil.variable(size, 0, MemVar.getVarOrFresh(name));
  }

  private static Rhs.Rvar local (int size, String name) {
    return rreil.variable(size, 0, MemVar.fresh(SingleFunctionParser.FUNCTION_LOCALS_PREFIX + name));
  }

  private static SortedMap<RReilAddr, RReil> instructions (RReil... instructions) {
    final SortedMap<RReilAddr, RReil> result = new TreeMap<RReilAddr, RReil>();
    for (int i = 0; i < instructions.length; i++) {
      result.put(RReilAddr.valueOf(i), instructions[i]);
    }
    return result;
  }

  private static <T> List<T> list (T... elements) {
    return Arrays.asList(elements);
  }
}
