package bindead.debug;

import static bindead.debug.StringHelpers.indentMultiline;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javalx.data.products.P3;
import javalx.numeric.Interval;
import javalx.numeric.Range;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RvarExtractor;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory.AnalysisDebugHooks;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.data.NumVar;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domains.affine.AffineProperties;
import bindead.domains.congruences.CongruenceProperties;
import bindead.domains.gauge.GaugeProperties;
import bindead.domains.intervals.IntervalProperties;
import bindead.domains.widening.delayed.DelayedWideningProperties;
import bindead.domains.widening.thresholds.ThresholdsWideningProperties;
import bindis.gdsl.GdslNativeDisassembler;

/**
 * A collection of tools to help with debugging a fixpoint analysis. Add the debug hooks from here to an analysis to
 * print values and other properties of variables and memory locations. It is necessary that you also enable the debug
 * output of the
 * fixpoint analysis (at least "debugAssignments") to see what instructions are evaluated.
 */
public class DebugHelper {
  /**
   * Skeleton for classes implementing the interface.
   */
  private static abstract class DebugHooksSkeleton implements AnalysisDebugHooks {
    @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint point, D domainState,
        Analysis<D> analysis) {
      // empty
    }

    @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
        D domainState, Analysis<D> analysis) {
      // empty
    }
  }

  /**
   * Dummy function called in the breakpoint hooks. Set a breakpoint in its body to stop the program
   * at a certain condition and debug it.
   *
   * @see DebugBreakpoints
   */
  public static void noopBreakpointFunction () {
    assert true;
  }

  /**
   * A collection of debug hooks code that injects a no-operation on which Java breakpoints can be set
   * for debugging.
   */
  public static class DebugBreakpoints {

    /**
     * Build a debug hook that executes a dummy function #DebugHelper.noopBreakpointFunction(). Set a breakpoint into
     * the functions body to break before the instruction at the given address is executed.
     *
     * @param address The address of the instruction before which to execute the hook
     */
    public AnalysisDebugHooks triggerBeforeInsn (String address) {
      final RReilAddr addressValue = RReilAddr.valueOf(address);
      return new DebugHooksSkeleton() {
        @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint point, D domainState,
            Analysis<D> analysis) {
          if (insn.getRReilAddress().equals(addressValue))
            noopBreakpointFunction();
        }
      };
    }

    /**
     * Build a debug hook that executes a dummy function #DebugHelper.noopBreakpointFunction(). Set a breakpoint into
     * the functions body to break after the instruction at the given address is executed.
     *
     * @param address The address of the instruction after which to execute the hook
     */
    public AnalysisDebugHooks triggerAfterInsn (String address) {
      final RReilAddr addressValue = RReilAddr.valueOf(address);
      return new DebugHooksSkeleton() {
        @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
            D domainState, Analysis<D> analysis) {
          if (insn.getRReilAddress().equals(addressValue))
            noopBreakpointFunction();
        }
      };

    }
  }

  private static boolean printLogMessages = false;
  private static PrintStream printer = System.out;
  public static final DebugKnobs analysisKnobs = new DebugKnobs();
  public static final DebugPrinters printers = new DebugPrinters();
  public static final DebugBreakpoints breakpoints = new DebugBreakpoints();
  private static PrintStream originalOutPrintStream;

  private static final PrintStream nullPrintStream = new PrintStream(new OutputStream() {
    @Override public void write (int b) throws IOException {
      // nada
    }
  });

  /**
   * Silence all the message that are printed using {@code System.out.print} methods.
   *
   * @see #unmuteSystemOut()
   */
  public static void muteSystemOut () {
    originalOutPrintStream = System.out;
    System.setOut(nullPrintStream);
  }

  /**
   * Show again all the message that are printed using {@code System.out.print} methods
   * after it has been muted through {@link #muteSystemOut()}.
   */
  public static void unmuteSystemOut () {
    if (originalOutPrintStream != null)
      System.setOut(originalOutPrintStream);
  }

  /**
   * Print a message if the logging is enabled.
   * Use this method in tests thus the output of tests can be enabled/disabled in a central place.
   *
   * @param obj A message that will be printed using is {@code toString()} method.
   * @see DebugKnobs
   */
  public static void log (Object... obj) {
    if (printLogMessages) {
      for (Object o : obj) {
        printer.print(o);
      }
    }
  }

  /**
   * Print a message and a newline if the logging is enabled.
   * Use this method in tests thus the output of tests can be enabled/disabled in a central place.
   *
   * @param obj A message that will be printed using is {@code toString()} method.
   * @see DebugKnobs
   */
  public static void logln (Object... obj) {
    if (printLogMessages) {
      log(obj);
      printer.println();
    }
  }


  /**
   * Combine one or more debug hooks into one.
   */
  public static AnalysisDebugHooks combine (final AnalysisDebugHooks... hooks) {
    return new AnalysisDebugHooks() {
      @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint ctx, D domainState,
          Analysis<D> analysis) {
        for (AnalysisDebugHooks hook : hooks) {
          if (hook != null)
            hook.beforeEval(insn, ctx, domainState, analysis);
        }
      }

      @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint ctx, RReilAddr target,
          D domainState, Analysis<D> analysis) {
        for (AnalysisDebugHooks hook : hooks) {
          if (hook != null)
            hook.afterEval(insn, ctx, target, domainState, analysis);
        }
      }
    };
  }

  /**
   * A collection of the common debug settings that can be enabled for the analysis. Having thins in one place and with
   * better names should simplify the discovery and manipulation of debug flags.
   */
  public static class DebugKnobs {

    /**
     * Print a compact representation of the domain hierarchy.
     *
     * @see #printFullDomain()
     */
    public void printCompactDomain () {
      DomainPrintProperties.INSTANCE.printCompact.setValue(true);
    }

    /**
     * Print the canonical, full domain hierarchy representation.
     *
     * @see #printCompactDomain()
     */
    public void printFullDomain () {
      DomainPrintProperties.INSTANCE.printCompact.setValue(false);
    }

    /**
     * Print the instructions and location (context) at each evaluation step of the analysis.
     */
    public void printInstructions () {
      AnalysisProperties.INSTANCE.debugAssignments.setValue(true);
    }

    /**
     * Print a summary of the analysis.
     */
    public void printSummary () {
      AnalysisProperties.INSTANCE.debugSummary.setValue(true);
    }

    /**
     * Print the warnings that occurred during the analysis.
     */
    public void printWarnings () {
      AnalysisProperties.INSTANCE.debugWarnings.setValue(true);
    }

    /**
     * Enable the printing of debug messages through {@link DebugHelper#log(Object)}.
     */
    public void printLogging () {
      DebugHelper.printLogMessages = true;
    }

    /**
     * Print the code listing after the analysis.
     */
    public void printCodeListing () {
      printRReilCodeListing();
      printNativeCodeListing();
    }

    /**
     * Print the RREIL code listing after the analysis.
     */
    public void printRReilCodeListing () {
      AnalysisProperties.INSTANCE.debugRReilCode.setValue(true);
    }

    /**
     * Print the native code listing after the analysis.
     */
    public void printNativeCodeListing () {
      AnalysisProperties.INSTANCE.debugNativeCode.setValue(true);
    }

    public void printSubsetOrEqual () {
      AnalysisProperties.INSTANCE.debugSubsetOrEqual.setValue(true);
    }

    public void printWidening () {
      AnalysisProperties.INSTANCE.debugWidening.setValue(true);
      ThresholdsWideningProperties.INSTANCE.debugWidening.setValue(true);
      DelayedWideningProperties.INSTANCE.debugWidening.setValue(true);
      IntervalProperties.INSTANCE.debugWidening.setValue(true);
    }

    /**
     * Do not show the numeric id and type of variables but only the memory region name. Registers and
     * especially memory regions can contain more than one variable, one for each part of bits addressed separately.
     * Showing the numeric id for the variables helps to separate e.g. {@code ah} and {@code ah} as they are both
     * part of {@code eax} but comprise different bits and thus different numeric variables.<br>
     * Nevertheless for some programs memory regions and numeric variables are mostly the same and thus
     * this setting can be used to remove clutter from the output. Note though that issues related to substitutions
     * can only be seen when knowing the numeric id of a variable.
     *
     * @see #printMemVarAndNumVar()
     */
    public void printMemVarOnly () {
      NumVar.printMemVarOnly(true);
    }

    /**
     * Print only the numeric id of a variable and not the memory region it belongs to.
     *
     * @see #printMemVarAndNumVar()
     */
    public void printNumVarOnly () {
      NumVar.printNumVarOnly(true);
    }

    /**
     * Show the numeric id and type of variables and not only the region name.
     *
     * @see #printMemVarOnly()
     * @see #printNumVarOnly()
     */
    public void printMemVarAndNumVar () {
      NumVar.printMemVarOnly(false);
      NumVar.printNumVarOnly(false);
    }

    public void useGDSLasFrontend () {
      AnalysisProperties.INSTANCE.useGDSLDisassembler.setValue(true);
      AnalysisProperties.INSTANCE.disassembleBlockWise.setValue(false);
      GdslNativeDisassembler.disableOptimizations();
    }

    public void useGDSLasFrontendAndOptimize () {
      AnalysisProperties.INSTANCE.useGDSLDisassembler.setValue(true);
      AnalysisProperties.INSTANCE.disassembleBlockWise.setValue(true);
      GdslNativeDisassembler.enableOptimizations();
    }

    public void useLegacyFrontend () {
      AnalysisProperties.INSTANCE.useGDSLDisassembler.setValue(false);
      AnalysisProperties.INSTANCE.disassembleBlockWise.setValue(false);
      GdslNativeDisassembler.disableOptimizations();
    }

    /**
     * Use this to enable the most common debug output flags.
     */
    public void enableCommon () {
      printLogging();
      printCodeListing();
      printInstructions();
      printWarnings();
      printSummary();
    }

    /**
     * Use this to clear any debug settings that might have been set elsewhere in the code.
     */
    public void disableAll () {
      AnalysisProperties.INSTANCE.disableAllDebugSwitches();
      AnalysisProperties.INSTANCE.debugNativeCode.setValue(false);
      AnalysisProperties.INSTANCE.debugRReilCode.setValue(false);
      AnalysisProperties.INSTANCE.debugWarnings.setValue(false);
      AffineProperties.INSTANCE.disableAllDebugSwitches();
      IntervalProperties.INSTANCE.disableAllDebugSwitches();
      GaugeProperties.INSTANCE.disableAllDebugSwitches();
      CongruenceProperties.INSTANCE.disableAllDebugSwitches();
      ThresholdsWideningProperties.INSTANCE.disableAllDebugSwitches();
      DelayedWideningProperties.INSTANCE.disableAllDebugSwitches();
      printMemVarAndNumVar();
      printFullDomain();
      DebugHelper.printLogMessages = false;
    }
  }

  /**
   * A collection of debug hooks code that prints domains or variables during the analysis.
   */
  public static class DebugPrinters {
    private static String indentForDomains = StringHelpers.repeatString(" ", 15);
    private static String indentForValues = StringHelpers.repeatString(" ", 30);

    private final static WatcherAction<Interval> printValueAction (final boolean onChangeOnly) {
      return new WatcherAction<Interval>() {
        @Override public <D extends RootDomain<D>> void valueUnchanged (String variableName, Interval value,
            D domainState) {
          if (!onChangeOnly) {
            printer.printf("  %s = %s", variableName, value);
          }
        }

        @Override public <D extends RootDomain<D>> void valueChanged (String variableName, Interval oldValue,
            Interval newValue, D domainState) {
          printer.printf("  %s = %s", variableName, newValue);
        }
      };
    }

    private final static WatcherAction<SetOfEquations> printEqualitiesAction (final boolean onChangeOnly) {
      return new WatcherAction<SetOfEquations>() {

        @Override public <D extends RootDomain<D>> void valueUnchanged (String variableName, SetOfEquations value,
            D domainState) {
          if (!onChangeOnly) {
            printer.printf(" eqs: %s", DomainQueryHelpers.formatLinearEqualities(value));
          }
        }

        @Override public <D extends RootDomain<D>> void valueChanged (String variableName, SetOfEquations oldValue,
            SetOfEquations newValue, D domainState) {
          printer.printf(" eqs: %s", DomainQueryHelpers.formatLinearEqualities(newValue));
        }
      };
    }

    private static AnalysisDebugHooks dumpDomainBeforeEval () {
      return new DebugHooksSkeleton() {
        @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint point, D domainState,
            Analysis<D> analysis) {
          printer.println();
          printer.println(indentMultiline(indentForDomains + "@ " + point.getAddress() + ": ", domainState.toString()));
          printer.println();
        }
      };
    }

    private static AnalysisDebugHooks dumpDomainAfterEval () {
      return new DebugHooksSkeleton() {
        @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
            D domainState, Analysis<D> analysis) {
          printer.println();
          printer.println(indentMultiline(indentForDomains + "@ " + point.getAddress() + " -> " + target + ": ",
              domainState.toString()));
          printer.println();
        }

      };
    }

    private static AnalysisDebugHooks dumpDomainFilteredBeforeEval (final String... domainsToShow) {
      return new DebugHooksSkeleton() {
        @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint point, D domainState,
            Analysis<D> analysis) {
          PrintStream outStream = printer;
          String indentationString = indentForDomains + "@ " + point.getAddress() + ": ";
          printOut(domainState, outStream, indentationString, domainsToShow);
        }

        private <D extends RootDomain<D>> void printOut (D domainState, PrintStream outStream, String indentationString,
            final String... domainsToShow) {
          DomainStringBuilder builder = new DomainStringBuilder();
          domainState.toString(builder);
          outStream.println();
          outStream.println(indentMultiline(indentationString,
              builder.toFilteredString(domainsToShow).toString()));
          outStream.println();
        }

      };
    }

    /**
     * Build a debug hook that prints the values of each variable in the instruction before and after each analysis step.
     */
    public AnalysisDebugHooks instructionEffect () {
      return instructionEffect(null, null);
    }

    /**
     * Build a debug hook that prints the values of each variable in the instruction before and after each analysis step.
     *
     * @param enableAddress The address after which the printer is enabled. If {@code null} the printer is enabled from
     *          start.
     * @param disableAddress The address after which the printer will be disabled. If {@code null} the printer is never
     *          disabled.
     */
    public AnalysisDebugHooks instructionEffect (RReilAddr enableAddress, RReilAddr disableAddress) {
      List<AnalysisDebugHooks> watchers = new ArrayList<AnalysisDebugHooks>();
      watchers.add(new InstructionVariablesPrinter());
      return new AddressRangeDebugger(watchers, enableAddress, disableAddress, false);
    }

    /**
     * Build a printer hook that displays the values of the given register locations after each instruction.
     *
     * @param enableAddress The address after which the printer is enabled. If {@code null} the printer is enabled from
     *          start.
     * @param disableAddress The address after which the printer will be disabled. If {@code null} the printer is never
     *          disabled.
     * @param variableNames The names of the register whose values should be printed
     */
    public AnalysisDebugHooks variablesDump (RReilAddr enableAddress, RReilAddr disableAddress,
        String... variableNames) {
      List<AnalysisDebugHooks> watchers = new ArrayList<AnalysisDebugHooks>();
      for (String variable : variableNames) {
        watchers.add(new VariableWatcher(variable, false));
      }
      return new AddressRangeDebugger(watchers, enableAddress, disableAddress, false);
    }

    /**
     * Build a watcher hook that displays the values of the given register locations whenever they are changed.
     *
     * @param analysis The fixpoint analysis
     * @param enableAddress The address after which the watcher is enabled. If {@code null} the watcher is enabled from
     *          start.
     * @param disableAddress The address after which the watcher will be disabled. If {@code null} the watcher is never
     *          disabled.
     * @param variableNames The names of the register whose values should be printed on change
     */
    public AnalysisDebugHooks variablesWatcher (final RReilAddr enableAddress, final RReilAddr disableAddress,
        String... variableNames) {
      List<AnalysisDebugHooks> watchers = new ArrayList<AnalysisDebugHooks>();
      for (String variable : variableNames) {
        watchers.add(new VariableWatcher(variable, true));
      }
      return new AddressRangeDebugger(watchers, enableAddress, disableAddress, false);
    }

    /**
     * Build a watcher hook that displays the values of the given memory locations whenever they are changed.
     *
     * @param enableAddress The address after which the watcher is enabled. If {@code null} the watcher is enabled from
     *          start.
     * @param disableAddress The address after which the watcher will be disabled. If {@code null} the watcher is never
     *          disabled.
     * @param memoryLocations A list of triples (name of a pointer register, an offset to that pointer and an access
     *          size)
     *          to identify a memory location, whose value should be printed on change
     */
    public AnalysisDebugHooks memoryWatcher (final RReilAddr enableAddress,
        final RReilAddr disableAddress, P3<String, Integer, Integer>... memoryLocations) {
      List<AnalysisDebugHooks> watchers = new ArrayList<AnalysisDebugHooks>();
      for (P3<String, Integer, Integer> tripel : memoryLocations) {
        String pointerName = tripel._1();
        Integer offset = tripel._2();
        Integer accessSize = tripel._3();
        watchers.add(new MemoryVariableValueWatcher(pointerName, offset, accessSize, printValueAction(true)));
      }
      return new AddressRangeDebugger(watchers, enableAddress, disableAddress, false);
    }

    /**
     * Build a debug hook that prints the domain state before each analysis step.
     *
     * @see #domainDumpBoth()
     * @see #buildDomainPrinter(RReilAddr, RReilAddr, String...)
     */
    public AnalysisDebugHooks domainDump () {
      return domainDump(null, null);
    }

    /**
     * Build a debug hook that prints the domain state before and after each analysis step.
     */
    public AnalysisDebugHooks domainDumpBoth () {
      return new AddressRangeDebugger(Arrays.asList(dumpDomainBeforeEval(), dumpDomainAfterEval()), null, null, false);
    }

    /**
     * Build a debug hook that prints the domain state before each analysis step.
     *
     * @param enableAddress The address after which the hook is enabled. If {@code null} the watcher is enabled from
     *          start.
     * @param disableAddress The address after which the hook will be disabled. If {@code null} the watcher is never
     *          disabled.
     */
    public AnalysisDebugHooks domainDump (RReilAddr enableAddress, RReilAddr disableAddress) {
      return new AddressRangeDebugger(Arrays.asList(dumpDomainBeforeEval()), enableAddress, disableAddress, false);
    }

    /**
     * Build a debug hook that prints the domain state before each analysis step.
     *
     * @param domainsToShow A list of domain names to be printed
     */
    public AnalysisDebugHooks domainDumpFiltered (String... domainsToShow) {
      return domainDumpFiltered(null, null, domainsToShow);
    }

    /**
     * Build a debug hook that prints some of the domain state before each analysis step.
     *
     * @param enableAddress The address after which the hook is enabled. If {@code null} the watcher is enabled from
     *          start.
     * @param disableAddress The address after which the hook will be disabled. If {@code null} the watcher is never
     *          disabled.
     * @param domainsToShow A list of domain names to be printed
     */
    public AnalysisDebugHooks domainDumpFiltered (RReilAddr enableAddress, RReilAddr disableAddress,
        String... domainsToShow) {
      return new AddressRangeDebugger(Arrays.asList(dumpDomainFilteredBeforeEval(domainsToShow)), enableAddress,
        disableAddress, false);
    }

    private interface WatcherAction<T> {
      public <D extends RootDomain<D>> void valueUnchanged (String variableName, T value, D domainState);

      public <D extends RootDomain<D>> void valueChanged (String variableName, T oldValue, T newValue, D domainState);
    }

    /**
     * Prints the values of all the variables in the instruction. It prints the rhs before the evaluation and the lhs
     * after the instruction evaluation.
     */
    private static class InstructionVariablesPrinter extends DebugHooksSkeleton {

      @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint point, D domainState,
          Analysis<D> analysis) {
        List<Rvar> variables = RvarExtractor.getRhs(insn);
        printValues(domainState, variables);
      }

      @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
          D domainState, Analysis<D> analysis) {
        List<Rvar> variables = RvarExtractor.getLhs(insn);
        printValues(domainState, variables);
      }

      private static <D extends RootDomain<D>> void printValues (D domainState, List<Rvar> variables) {
        if (!variables.isEmpty())
          printer.print(indentForValues);
        for (Rvar variable : variables) {
          Range value = DomainQueryHelpers.queryRange(variable, domainState);
          SetOfEquations equalities = DomainQueryHelpers.queryEqualities(variable, domainState);
          Object pointsTo = DomainQueryHelpers.queryPointsToSet(variable, domainState);
          if (value != null) {
            printer.printf("%s = %s", variable.getRegionId(), value);
            if (equalities != null && !equalities.isEmpty()) {
              printer.print("\t");
              printer.printf("eqs: %s", DomainQueryHelpers.formatLinearEqualities(equalities));
            }
            if (pointsTo != null) {
              printer.print("\t");
              printer.printf("pts: %s", pointsTo);
            }
            printer.print("\t");
          }
        }
        if (!variables.isEmpty())
          printer.println();
      }

    }

    /**
     * A watcher for a variable that will print the equalities for the variable whenever they change.
     */
    private static class VariableWatcher extends DebugHooksSkeleton {
      private SetOfEquations oldEqualities = null;
      private Interval oldValue = null;
      private final String variableName;
      private final WatcherAction<SetOfEquations> equalitiesAction;
      private final WatcherAction<Interval> valueAction;

      public VariableWatcher (String variableName, boolean onChangeOnly) {
        this.variableName = variableName;
        valueAction = printValueAction(onChangeOnly);
        equalitiesAction = printEqualitiesAction(onChangeOnly);
      }

      @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint ctx, D domainState,
          Analysis<D> analysis) {
        watchValue(domainState, analysis);
        watchEqualities(domainState, analysis);
        printer.println();
      }

      @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
          D domainState, Analysis<D> analysis) {
        watchValue(domainState, analysis);
        watchEqualities(domainState, analysis);
        printer.println();
      }

      private <D extends RootDomain<D>> void watchValue (D domainState, Analysis<D> analysis) {
        Range range = DomainQueryHelpers.queryRange(variableName, domainState, analysis.getPlatform());
        if (range == null)
          return;
        Interval newValue = range.convexHull();
        if (!newValue.equals(oldValue)) {
          valueAction.valueChanged(variableName, oldValue, newValue, domainState);
          oldValue = newValue;
        } else {
          valueAction.valueUnchanged(variableName, newValue, domainState);
        }
      }

      private <D extends RootDomain<D>> void watchEqualities (D domainState, Analysis<D> analysis) {
        Rvar variable = DomainQueryHelpers.resolveVariable(variableName, domainState, analysis.getPlatform());
        if (variable == null)
          return;
        SetOfEquations newValue = DomainQueryHelpers.queryEqualities(variable, domainState);
        if (newValue == null)
          return;
        if (oldEqualities == null) {
          equalitiesAction.valueChanged(variableName, oldEqualities, newValue, domainState);
          oldEqualities = newValue;
        } else {
          if (!newValue.difference(oldEqualities).isEmpty()) {
            equalitiesAction.valueChanged(variableName, oldEqualities, newValue, domainState);
            oldEqualities = newValue;
          } else {
            equalitiesAction.valueUnchanged(variableName, newValue, domainState);
          }
        }
      }

    }

    private static class MemoryVariableValueWatcher extends DebugHooksSkeleton {
      private final String pointerName;
      private final WatcherAction<Interval> actions;
      private final int offset;
      private final int sizeInBits;
      private Interval oldValue = null;

      public MemoryVariableValueWatcher (String pointerName, int offset, int sizeInBits,
          WatcherAction<Interval> actions) {
        this.pointerName = pointerName;
        this.offset = offset;
        this.sizeInBits = sizeInBits;
        this.actions = actions;
      }

      @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint ctx, D domainState,
          Analysis<D> analysis) {
        watchMemory(domainState, analysis);
      }

      @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
          D domainState, Analysis<D> analysis) {
        watchMemory(domainState, analysis);
      }

      private <D extends RootDomain<D>> void watchMemory (D domainState, Analysis<D> analysis) {
        Range range =
          DomainQueryHelpers.queryRange(pointerName, offset, sizeInBits, domainState, analysis.getPlatform());
        if (range == null)
          return;
        Interval newValue = range.convexHull();
        String operator = "";
        if (offset > 0)
          operator = "+";
        String locationName = "[" + pointerName + operator + offset + "]";
        if (!newValue.equals(oldValue)) {
          actions.valueChanged(locationName, oldValue, newValue, domainState);
          oldValue = newValue;
        } else {
          actions.valueUnchanged(locationName, newValue, domainState);
        }
      }
    }

    /**
     * Executes a callback from a given start address till a given end address (both included). The debugger can trigger
     * only once or can be re-enabled when entering the given address range a second or later time.
     */
    private static class AddressRangeDebugger implements AnalysisDebugHooks {
      boolean enabled = false;
      boolean disabled = false;
      private final List<AnalysisDebugHooks> callbacks;
      private final RReilAddr enableAddress;
      private final RReilAddr disableAddress;
      private final boolean onceOnly;

      public AddressRangeDebugger (List<AnalysisDebugHooks> callbacks, RReilAddr enableAddress,
          RReilAddr disableAddress,
          boolean onceOnly) {
        this.callbacks = callbacks;
        this.enableAddress = enableAddress;
        this.disableAddress = disableAddress;
        this.onceOnly = onceOnly;
      }

      @Override public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint ctx, D domainState,
          Analysis<D> analysis) {
        if (onceOnly && disabled)
          return;
        RReilAddr currentAddress = insn.getRReilAddress();
        if (!enabled)
          tryTransitionToEnabledState(currentAddress);
        if (enabled) {
          executeBeforeCallbacks(insn, ctx, domainState, analysis);
        }
      }

      @Override public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target,
          D domainState, Analysis<D> analysis) {
        if (onceOnly && disabled)
          return;
        RReilAddr currentAddress = insn.getRReilAddress();
        if (enabled) {
          executeAfterCallbacks(insn, point, target, domainState, analysis);
          tryTransitionToDisabledState(currentAddress);
        }
      }

      private void tryTransitionToEnabledState (RReilAddr currentAddress) {
        enabled = isEnableTrigger(currentAddress, enableAddress);
      }

      private void tryTransitionToDisabledState (RReilAddr currentAddress) {
        disabled = isDisableTrigger(currentAddress, disableAddress);
        if (disabled)
          enabled = false;
      }

      private <D extends RootDomain<D>> void executeBeforeCallbacks (
          RReil insn, ProgramPoint ctx, D domainState, Analysis<D> analysis) {
        for (AnalysisDebugHooks callback : callbacks) {
          callback.beforeEval(insn, ctx, domainState, analysis);
        }
      }

      private <D extends RootDomain<D>> void executeAfterCallbacks (
          RReil insn, ProgramPoint ctx, RReilAddr target, D domainState, Analysis<D> analysis) {
        for (AnalysisDebugHooks callback : callbacks) {
          callback.afterEval(insn, ctx, target, domainState, analysis);
        }
      }

      private static boolean isDisableTrigger (RReilAddr currentAddress, RReilAddr disableAddress) {
        if (disableAddress == null) // never disable if no address specified
          return false;
        else
          return currentAddress.equals(disableAddress);
      }

      private static boolean isEnableTrigger (RReilAddr currentAddress, RReilAddr enableAddress) {
        if (enableAddress == null) // always enable if no address specified
          return true;
        else
          return currentAddress.equals(enableAddress);
      }
    }

  }
}
