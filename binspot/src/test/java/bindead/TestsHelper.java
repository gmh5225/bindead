package bindead;

import static javalx.data.Option.some;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javalx.data.Option;
import javalx.data.products.P2;
import javalx.exceptions.UnimplementedException;
import javalx.numeric.Bound;
import javalx.numeric.Congruence;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import rreil.assembler.CompiledAssembler;
import rreil.lang.AssertionOp;
import rreil.lang.ComparisonOp;
import rreil.lang.Lhs;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReil.Assertion;
import rreil.lang.RReil.Assertion.AssertionCompare;
import rreil.lang.RReil.Assertion.AssertionReachable;
import rreil.lang.RReil.Assertion.AssertionUnreachable;
import rreil.lang.RReil.Assertion.AssertionWarnings;
import rreil.lang.RReil.Assign;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Cmp;
import rreil.lang.Rhs.RangeRhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RReilFactory;
import rreil.lang.util.RhsFactory;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.analyses.algorithms.data.CallString;
import bindead.analyses.algorithms.data.ProgramCtx;
import bindead.data.Linear;
import bindead.data.NumVar;
import bindead.data.VarSet;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.exceptions.Unreachable;
import binparse.Binary;
import binparse.BinaryFileFormat;

/**
 * Collection of tools to make writing tests easier.
 */
public class TestsHelper {
  public final static String binariesPath32bit = "/bindead/binaries/x86_32/";
  public final static String binariesPath64bit = "/bindead/binaries/x86_64/";

  /**
   * Instantiate a binary from a resource file path. Automatically chooses the right binary format (LINUX, Windows,
   * RREIL, etc.).
   *
   * @param resourcePath A file path that is relative to this projects binary examples path.
   * @return The binary
   * @throws IOException if there was an error while trying to access the file
   */
  public static Binary getExamplesBinary (String resourcePath) throws IOException {
    String file = TestsHelper.class.getResource(resourcePath).getPath();
    return BinaryFileFormat.getBinary(file);
  }

  /**
   * Instantiate a binary from a resource file path. Automatically chooses the right binary format (LINUX, Windows,
   * RREIL, etc.).
   *
   * @param resourcePath A file path that is relative to this projects 32 bit binary examples path.
   * @return The binary
   * @throws IOException if there was an error while trying to access the file
   * @see #binariesPath32bit
   */
  public static Binary get32bitExamplesBinary (String resourcePath) throws IOException {
    return getExamplesBinary(binariesPath32bit + resourcePath);
  }

  /**
   * Instantiate a binary from a resource file path. Automatically chooses the right binary format (LINUX, Windows,
   * RREIL, etc.).
   *
   * @param resourcePath A file path that is relative to this projects 64 bit binary examples path.
   * @return The binary
   * @throws IOException if there was an error while trying to access the file
   * @see #binariesPath64bit
   */
  public static Binary get64bitExamplesBinary (String resourcePath) throws IOException {
    return getExamplesBinary(binariesPath64bit + resourcePath);
  }

  /**
   * Evaluate the assertion given as RREIL instruction on the given domain state as a JUnit assertion.
   *
   * @param state A state to test the assertion on
   * @param assertion A RREIL assertion instruction
   */
  public static void evaluateAssertion (RootDomain<?> state, String assertion) {
    RReil.Assertion rreilAssertion = (Assertion) RReil.from(assertion);
    String assertionInfo = rreilAssertion.toString();
    evaluateAssertion(null, state, rreilAssertion, assertionInfo);
  }

  private static void evaluateAssertion (Analysis<?> analysis, RootDomain<?> state, RReil.Assertion rreilAssertion,
      String assertionInfo) {
    if (rreilAssertion instanceof AssertionReachable) {
      String assertionMessage = assertionInfo + " was not reachable with any state.";
      assertThat(assertionMessage, state, notNullValue());
    } else if (rreilAssertion instanceof AssertionUnreachable) {
      String assertionMessage = assertionInfo + " was reachable with a non-bottom state.";
      assertThat(assertionMessage, state, nullValue());
    } else if (rreilAssertion instanceof AssertionWarnings) {
      AssertionWarnings assertion = (AssertionWarnings) rreilAssertion;
      // XXX: because warnings are not propagated to the next program point we need to retrieve the warnings
      // from the previous program point. This assumes that the assertion is immediately following the program
      // point that generated the warnings
      RReilAddr address = assertion.getRReilAddress().nextBase(-1);
      WarningsContainer warnings = analysis.getWarnings().get(new ProgramCtx(CallString.root(), address));
      assertThat(assertionInfo, warnings.size(), is(assertion.getNumberOfExpectedWarnings()));
    } else if (rreilAssertion instanceof AssertionCompare) {
      AssertionCompare assertion = (AssertionCompare) rreilAssertion;
      if (state == null)
        throw new IllegalArgumentException(assertionInfo + " (no domain state to apply assertion on)");
      switch (assertion.getOperator()) {
      case Equal:
        // TODO: not yet sure what to use where. Think of what one wants when comparing with an interval.
        evaluateAssertionQuery(state, assertionInfo, assertion);
//        evaluateAssertionTest(state, assertionInfo, assertion);
        break;
      case AffineEquality:
        evaluateAssertionAffineEquality(state, assertionInfo, assertion);
        break;
      case NotEqual:
        // NOTE: using the test method below is more precise as the query method would not yield false for 2 != [1, 2]
//        evaluateAssertionQuery(state, assertionInfo, assertion);
      case SignedLessThan:
      case SignedLessThanOrEqual:
      case UnsignedLessThan:
      case UnsignedLessThanOrEqual:
        evaluateAssertionTest(state, assertionInfo, assertion);
        break;
      default:
        break;
      }
    } else {
      fail("Unknown assertion type.");
    }
  }

  @SuppressWarnings("rawtypes") private static P2<Rval, RootDomain> toVariable (RootDomain<?> state, Rhs varOrValue) {
    RReilFactory rreilFactory = RReilFactory.instance;
    if (varOrValue instanceof RangeRhs) {
      Lhs tempVariable = new Lhs(((RangeRhs) varOrValue).getSize(), 0, MemVar.freshTemporary());
      Assign assign = rreilFactory.assign(null, tempVariable, varOrValue);
      state = (RootDomain<?>) state.eval(assign);
      return P2.tuple2((Rval) tempVariable.asRvar(), (RootDomain) state);
    } else {
      return P2.tuple2((Rval) varOrValue, (RootDomain) state);
    }
  }

  /**
   * Evaluates the assertion by applying the comparison on the state.
   * This is unfortunately to imprecise mostly and does not yield the results we want.
   */
  @SuppressWarnings("rawtypes") private static void evaluateAssertionTest (RootDomain<?> state, String assertionInfo,
      AssertionCompare assertion) {
    RReilFactory rreilFactory = RReilFactory.instance;
    RhsFactory rhsFactory = RhsFactory.getInstance();
    Rhs lhs = assertion.getLhs();
    ComparisonOp operator = assertion.getOperator().toComparison();
    Rhs rhs = assertion.getRhs();
    P2<Rval, RootDomain> realLhs = toVariable(state, lhs);
    state = realLhs._2();
    P2<Rval, RootDomain> realRhs = toVariable(state, rhs);
    state = realRhs._2();
    Cmp comparison = rhsFactory.comparison(realLhs._1(), operator, realRhs._1());
    Lhs flag = new Lhs(1, 0, MemVar.freshTemporary());
    state = (RootDomain<?>) state.eval(rreilFactory.assign(null, flag, comparison));
    try {
      state = (RootDomain<?>) state.eval(rreilFactory.testIfZero(flag.asRvar()));
      // test the negated condition and if it is not bottom then the original test is not satisfiable
      // Hence, the code following now is the error code.
      StringBuilder builder = new StringBuilder();
      builder.append(assertionInfo);
      builder.append("\n");
      builder.append("But was:");
      if (lhs instanceof Rvar) {
        builder.append("\n");
        builder.append(lhs);
        builder.append(" = ");
        builder.append(state.queryRange((Rvar) lhs));
      }
      if (rhs instanceof Rvar) {
        builder.append("\n");
        builder.append(rhs);
        builder.append(" = ");
        builder.append(state.queryRange((Rvar) rhs));
      }
      fail(builder.toString());
    } catch (Unreachable _) {
    }
  }

  private static void evaluateAssertionAffineEquality (RootDomain<?> state, String assertionInfo,
      AssertionCompare assertion) {
    Rhs lhs = assertion.getLhs();
    Rhs rhs = assertion.getRhs();
    assert assertion.getOperator().equals(AssertionOp.AffineEquality);
    if (!(lhs instanceof Rvar))
      throw new IllegalArgumentException(assertionInfo + " " + lhs + " must be a variable.");
    if (!(rhs instanceof Rvar))
      throw new IllegalArgumentException(assertionInfo + " " + rhs + " must be a variable.");
    Rvar lhsVar = (Rvar) lhs;
    Rvar rhsVar = (Rvar) rhs;
    assertEqualityRelationExists(state, state.resolveVariable(lhsVar.getRegionId(), lhsVar.bitsRange()).get(),
        state.resolveVariable(rhsVar.getRegionId(), rhsVar.bitsRange()).get(), assertionInfo);
  }

  /**
   * Assert that a variable has a linear equalities relation to another variable.
   * Performs the transitive closure on the linear equalities reported.
   */
  private static void assertEqualityRelationExists (RootDomain<?> state, NumVar x1, NumVar x2, String assertionInfo) {
    VarSet varsInRelation = VarSet.of(x1);
    VarSet worklist = VarSet.of(x1);
    while (!worklist.isEmpty()) {
      NumVar variable = worklist.first();
      worklist = worklist.remove(variable);
      for (Linear equality : state.queryEqualities(variable)) {
        VarSet newEqualityVars = equality.getVars().difference(varsInRelation);
        varsInRelation = varsInRelation.union(newEqualityVars);
        worklist = worklist.union(newEqualityVars);
      }
    }
    assertThat(assertionInfo, varsInRelation.contains(x2), is(true));
  }

  private static void evaluateAssertionQuery (RootDomain<?> state, String assertionInfo, AssertionCompare assertion) {
    Rhs lhs = assertion.getLhs();
    Rhs rhs = assertion.getRhs();
    int bitSize = assertion.getSize();
    Interval lhsValue = getValue(state, assertionInfo, lhs);
    Interval rhsValue = getValue(state, assertionInfo, rhs);
    rhsValue = wrapTopToSize(rhsValue, bitSize);
    lhsValue = wrapTopToSize(lhsValue, bitSize);
    // FIXME: this only tests equality at the moment. Need to take the other operators into account.
    switch (assertion.getOperator()) {
    case Equal:
      assertThat(assertionInfo, lhsValue, is(rhsValue));
      break;
    case NotEqual:
      assertThat(assertionInfo, lhsValue, not(rhsValue));
      break;
    case AffineEquality:
    case SignedLessThan:
    case SignedLessThanOrEqual:
    case UnsignedLessThan:
    case UnsignedLessThanOrEqual:
    default:
      throw new UnimplementedException();
    }
  }

  private static Interval getValue (RootDomain<?> state, String assertionInfo, Rhs rhs) {
    if (rhs instanceof RangeRhs) {
      return ((RangeRhs) rhs).getRange();
    } else if (rhs instanceof Rval) {
      javalx.numeric.Range rhsRange = state.queryRange((Rval) rhs);
      if (rhsRange == null) {
        if (rhs instanceof Rvar)
          throw new IllegalArgumentException(assertionInfo + " (no value for variable: " + rhs + " in domain state");
        else
          throw new IllegalArgumentException(assertionInfo + " " + rhs + " must be a variable or range.");
      }
      return rhsRange.convexHull();
    }
    return null;
  }

  /**
   * Extract all the assertions in the RREIL code and evaluate them as JUnit assertions.
   *
   * @param analysis The finished fixpoint analysis
   */
  public static void evaluateAssertions (Analysis<?> analysis) {
    Collection<Assertion> assertions = getAssertionInstructions(analysis.getRReilCode().getInstructions().values());
    evaluateAssertions(analysis, assertions);
  }

  private static void evaluateAssertions (Analysis<?> analysis, Collection<Assertion> assertions) {
    for (Assertion assertion : assertions) {
      String assertionInfo = "0x" + assertion.getRReilAddress().toShortString() + ": " + assertion;
      RReilAddr address = assertion.getRReilAddress();
      RootDomain<?> state = analysis.getState(CallString.root(), address).getOrNull();
      evaluateAssertion(analysis, state, assertion, assertionInfo);
    }
  }

  /**
   * Use the analyzer with the default domains to analyze the RREIL assembly and evaluate all the assertions after
   * the analysis.
   */
  public static Analysis<?> evaluateAssertions (String assembly) {
    return evaluateAssertions(assembly, null);
  }

  /**
   * Use the analyzer with the default domains to analyze the RREIL assembly and evaluate all the assertions after
   * the analysis. Also inject debug hooks into the analysis.
   */
  public static Analysis<?> evaluateAssertions (String assembly, AnalysisDebugHooks debug) {
    Analysis<?> analysis = new AnalysisFactory().runAnalysis(assembly, debug);
    // we need to retrieve the assertions from the compiled code as the code "disassembled"
    // by the analysis does not contain dead branches. But we need dead branches to evaluate
    // the assertions they might contain (e.g. asserting that the branch is (un-)reachable)
    // missing the assertions in dead code would hide errors
    CompiledAssembler compilationUnit = CompiledAssembler.from(assembly);
    Collection<Assertion> assertions = getAssertionInstructions(compilationUnit.getInstructions().values());
    evaluateAssertions(analysis, assertions);
    return analysis;
  }

  /**
   * Collects and returns all the assertion instructions that exist in the RREIL code.
   */
  private static Collection<Assertion> getAssertionInstructions (Collection<RReil> code) {
    // using a map here to sort the assertions by their addresses
    Map<RReilAddr, RReil.Assertion> assertions = new TreeMap<RReilAddr, RReil.Assertion>();
    for (RReil instruction : code) {
      if (instruction instanceof RReil.Assertion)
        assertions.put(instruction.getRReilAddress(), (Assertion) instruction);
    }
    return assertions.values();
  }

  private static int getNumberOfWarnings (Analysis<?> analysis, Class<?> warningType) {
    int numberOfWarnings = 0;
    for (WarningsContainer entry : analysis.getWarnings().values()) {
      if (warningType == null) {
        numberOfWarnings += entry.size();
      } else {
        for (WarningMessage message : entry) {
          if (message.getClass().equals(warningType))
            numberOfWarnings++;
        }
      }
    }
    return numberOfWarnings;
  }

  /**
   * Assert that the analysis did not produce any warnings.
   *
   * @param analysis The analysis result.
   */
  public static void assertNoWarnings (Analysis<?> analysis) {
    assertHasWarnings(0, analysis);
  }

  /**
   * Assert that the analysis did not produce any warnings of a certain warning type.
   *
   * @param warningType The type of warnings.
   * @param analysis The analysis result.
   */
  public static void assertNoWarnings (Class<?> warningType, Analysis<?> analysis) {
    assertHasWarnings(warningType, 0, analysis);
  }

  /**
   * Assert that the analysis produced a given number of warnings of a certain warning type.
   *
   * @param warningType The type of warnings.
   * @param numberOfSuchWarnings The exact number of warnings of the given type produced by the analysis.
   * @param analysis The analysis result.
   */
  public static void assertHasWarnings (Class<?> warningType, int numberOfSuchWarnings, Analysis<?> analysis) {
    assertThat("Number of warnings of type: " + warningType + " that were produced by the analysis.",
        getNumberOfWarnings(analysis, warningType), is(numberOfSuchWarnings));
  }

  /**
   * Assert that the analysis produced a given number of warnings.
   *
   * @param numberOfWarnings The exact number of warnings produced by the analysis.
   * @param analysis The analysis results.
   */
  public static void assertHasWarnings (int numberOfWarnings, Analysis<?> analysis) {
    assertThat("Number of warnings that were produced by the analysis.", getNumberOfWarnings(analysis, null),
        is(numberOfWarnings));
  }

  /**
   * Assert that the variable at the given program point has a certain value.
   *
   * @param address The program point to test the value at.
   * @param variable The name of the variable to test.
   * @param variableSize The bit-size of the variable to test.
   * @param value The value that the variable should have.
   * @param analysis A finished analysis.
   */
  public static void assertValueAt (int address, String variable, int variableSize, Interval value, Analysis<?> analysis) {
    assert address >= 0;
    assert variable != null;
    assert variableSize > 0;
    assert analysis != null;
    assert value != null;
    assertThat(analysis.query(RReilAddr.valueOf(address), variable, variableSize, 0), is(some(value)));
  }

  /**
   * Assert that the variable at the given program point has a certain congruence.
   *
   * @param address The program point to test the value at.
   * @param variable The name of the variable to test.
   * @param variableSize The bit-size of the variable to test.
   * @param congruence The congruence that the variable should have.
   * @param analysis A finished analysis.
   */
  public static void assertCongruenceAt (int address, String variable, int variableSize, Congruence congruence, Analysis<?> analysis) {
    assert address >= 0;
    assert variable != null;
    assert variableSize > 0;
    assert analysis != null;
    assert congruence != null;
    Option<Range> range = analysis.queryRange(RReilAddr.valueOf(address), variable, variableSize, 0);
    assert range.isSome();
    assertThat(range.get().getCongruence(), is(congruence));
  }

  // implements a simple wrapping logic
  private static Interval wrapTopToSize (Interval value, int bits) {
    if (value.contains(Bound.MINUSONE)) { // assume signed
      Interval signedTop = Interval.signedTop(bits);
      if (!signedTop.contains(value))
        return signedTop;
    } else { // assume unsigned
      Interval unsignedTop = Interval.unsignedTop(bits);
      if (!unsignedTop.contains(value))
        return unsignedTop;
    }
    return value; // everything else is passed on as is
  }

  /**
   * A builder to ease writing assertions. To use statically import the {@code assertResultOf(Analysis)} method and you
   * will be able to write assertions in one of the following ways:<br>
   * <br>
   * {@code
   * assertResultOf(analysis).at(4).forVar("r1").is(Interval.valueOf(0, 18)).withSize(32);
   * assertResultOf(analysis).at(4).forVar("r1").is("[0, 18]").withSize(32);
   * assertResultOf(analysis).at(4).is("r1 = [0, 18]").withSize(32);
   * assertResultOf(analysis).at(4).is("r1 = [-oo, +oo]").withSize(64); // signed TOP
   * assertResultOf(analysis).at(4).is("r1 = [0, +oo]").withSize(64); // unsigned TOP
   * assertResultOf(analysis).at(4).is("r1 = 1234").withSize(8);
   * assertResultOf(analysis).at(4).is("r1 = 0xab").withSize(16);
   * }
   *
   * @see {@link Interval} for the possible ways to build an interval from a string.
   */
  public static class AssertionsBuilder {
    private final Analysis<?> analysis;
    private int address;
    private int variableSize;
    private String variableName;

    private AssertionsBuilder (Analysis<?> analysis) {
      this.analysis = analysis;
    }

    public static AssertionsBuilder assertResultOf (Analysis<?> analysis) {
      return new AssertionsBuilder(analysis);
    }

    public AssertionsBuilder at (int address) {
      this.address = address;
      return this;
    }

    public AssertionsBuilder forVar (String variableName) {
      this.variableName = variableName;
      return this;
    }

    public void is (Interval value) {
      // need to fix the size for a TOP interval
      value = wrapTopToSize(value, variableSize);
      assertValueAt(address, variableName, variableSize, value, analysis);
    }

    public void is (String expressionOrValue) {
      Interval value;
      if (expressionOrValue.contains("=")) { // the string is an expression
        String[] split = expressionOrValue.split("=");
        assert split.length == 2;
        this.variableName = split[0].trim();
        value = Interval.of(split[1].trim());
      } else {
        value = Interval.of(expressionOrValue);
      }
      // need to fix the size for a TOP interval
      value = wrapTopToSize(value, variableSize);
      assertValueAt(address, variableName, variableSize, value, analysis);
    }

    public void is (Congruence congruence) {
      assertCongruenceAt(address, variableName, variableSize, congruence, analysis);
    }

    public AssertionsBuilder withSize (int variableSize) {
      this.variableSize = variableSize;
      return this;
    }
  }

  /**
   * Append newlines after each argument.
   */
  public static String lines (String... lines) {
    StringBuilder builder = new StringBuilder();
    for (String line : lines) {
      builder.append(line + "\n");
    }
    return builder.toString();
  }

  /**
   * Remove any assertion from an assembler program and add newlines after each argument.
   *
   * @see #lines(String...)
   * @see #removeAssertions(String...)
   */
  public static String linesWithoutAssertions (String... lines) {
    return lines(removeAssertions(lines));
  }

  /**
   * Remove the assertions from an assembler program given as one statement per line.
   *
   * @param lines The assembler program
   * @return The same assembler program without the assertion statements
   */
  public static String[] removeAssertions (String... lines) {
    List<String> originalArray = Arrays.asList(lines);
    List<String> resultArray = new ArrayList<String>();
    for (String string : originalArray) {
      if (!string.startsWith("assert"))
        resultArray.add(string);
    }
    return resultArray.toArray(new String[] {});
  }
}
