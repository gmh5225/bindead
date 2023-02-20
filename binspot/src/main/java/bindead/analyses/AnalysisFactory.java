package bindead.analyses;

import javalx.data.Option;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import bindead.analyses.algorithms.CallStringAnalysis;
import bindead.analyses.algorithms.FixpointAnalysis;
import bindead.analyses.algorithms.RecursiveDisassembler;
import bindead.analyses.callback.Callbacks;
import bindead.analyses.systems.SystemModel;
import bindead.analyses.systems.SystemModelRegistry;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;
import bindead.environment.platform.Platform;
import binparse.Binary;
import binparse.Symbol;
import binparse.rreil.RReilBinary;

/**
 * Instantiates the analyzer with the right domain hierarchy and runs
 * an analysis either on binaries or a RREIL assembler string.
 *
 * @author Bogdan Mihaila
 */
public class AnalysisFactory {
  private String domainHierarchy;
  public static final String defaultDomainHierarchy =
    "SegMem Processor Stack -Null Data -Heap Fields "
    + "-Undef Predicates(F) -SupportSet PointsTo -Disjunction Wrapping "
    + "DelayedWidening -DelayedWidening(Thresholds) -Phased ThresholdsWidening "
      + "Predicates(Z) RedundantAffine -Affine Congruences Intervals -IntervalSets "
      + "-Apron(Polyhedra) -Apron(Octagons) -Apron(Intervals) ";

  /**
   * Instantiate the analysis with the default domain hierarchy.
   */
  public AnalysisFactory () {
    this(defaultDomainHierarchy);
  }

  /**
   * Instantiate the analysis with the domain hierarchy given by {@code domainHierarchy}.
   *
   * @param domainHierarchy A string to be parsed and instantiated to a domain hierarchy.
   * @see DomainFactory.parseFactory() for a description of the syntax.
   */
  public AnalysisFactory (String domainHierarchy) {
    this.domainHierarchy = domainHierarchy;
  }

  /**
   * Enable the usage of one or more domains. Note that the domain must be part of the domain hierarchy that
   * was used to instantiate this factory. It is not possible to add unknown domains with this method.
   * @see #disableDomains(String)
   */
  public AnalysisFactory enableDomains (String...domainNames) {
    // normalization necessary to be able to match names
    String newDomainHierarchy = this.domainHierarchy.toLowerCase();
    for (String domainName : domainNames) {
      domainName = domainName.toLowerCase();
      newDomainHierarchy = newDomainHierarchy.replace(" -" + domainName + " ", " " + domainName + " ");
    }
    this.domainHierarchy = newDomainHierarchy;
    return this;
  }

  /**
   * Disable the usage of a domain.
   * @see #enableDomains(String)
   */
  public AnalysisFactory disableDomains (String...domainNames) {
    // normalization necessary to be able to match names
    String newDomainHierarchy = this.domainHierarchy.toLowerCase();
    for (String domainName : domainNames) {
      domainName = domainName.toLowerCase();
      newDomainHierarchy = newDomainHierarchy.replace(" " + domainName + " ", " -" + domainName + " ");
    }
    this.domainHierarchy = newDomainHierarchy;
    return this;
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) public RootDomain initialDomain () {
    return DomainFactory.parseFactory(domainHierarchy).build();
  }

  @SuppressWarnings({"rawtypes", "unchecked"}) public static RootDomain buildInitialDomain (String domainHierarchy) {
    DomainHierarchyFactory domain = DomainFactory.parseFactory(domainHierarchy);
    return domain.build();
  }

  /**
   * Returns the address of the main function or the program entry address if
   * the binary has no symbols or no main function exists.
   *
   * @param binary A binary file
   * @return A start address for the CFG reconstruction analysis
   */
  public static RReilAddr getStartAddress (Binary binary) {
    long reconstructionStartAddress;
    Option<Symbol> mainFunction = binary.getMainFunction();
    if (mainFunction.isSome())
      reconstructionStartAddress = mainFunction.get().getAddress();
    else
      reconstructionStartAddress = binary.getEntryAddress();
    return RReilAddr.valueOf(reconstructionStartAddress);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Analysis<?> getFixpointAnalysis (Binary binary) {
    SystemModel systemModel = SystemModelRegistry.getLinuxModel(binary); // for now the only thing we have
    AnalysisEnvironment environment = new AnalysisEnvironment(systemModel, null);
    return new FixpointAnalysis(environment, binary, initialDomain());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Analysis<?> getCallstringAnalysis (Binary binary) {
    SystemModel systemModel = SystemModelRegistry.getLinuxModel(binary); // for now the only thing we have
    AnalysisEnvironment environment = new AnalysisEnvironment(systemModel, null);
    return new CallStringAnalysis(environment, binary, initialDomain());
  }

  @SuppressWarnings({"rawtypes"})
  public Analysis<?> getRecursiveDisassembler (Binary binary) {
    SystemModel systemModel = SystemModelRegistry.getLinuxModel(binary); // for now the only thing we have
    AnalysisEnvironment environment = new AnalysisEnvironment(systemModel, null);
    return new RecursiveDisassembler(environment, binary);
  }

  /**
   * Default uses the Fixpoint Analysis with procedure summaries (not the CallString Method).
   */
  public Analysis<?> runAnalysis (String rreilAssembly) {
    return runAnalysis(rreilAssembly, null);
  }

  /**
   * Default uses the Fixpoint Analysis with procedure summaries (not the CallString Method).
   */
  public Analysis<?> runAnalysis (String rreilAssembly, AnalysisDebugHooks debug) {
    RReilBinary binary = RReilBinary.fromString(rreilAssembly);
    return runAnalysis(binary, debug);
  }

  /**
   * Default uses the Fixpoint Analysis with procedure summaries (not the CallString Method).
   */
  public Analysis<?> runAnalysis (Binary binary) {
    return runAnalysis(binary, null);
  }

  /**
   * Default uses the Fixpoint Analysis with procedure summaries (not the CallString Method).
   */
  public Analysis<?> runAnalysis (Binary binary, AnalysisDebugHooks debug) {
    return runAnalysis(binary, debug, null);
  }

  /**
   * Default uses the Fixpoint Analysis with procedure summaries (not the CallString Method).
   */
  public Analysis<?> runAnalysis (Binary binary, AnalysisDebugHooks debug, Callbacks registry) {
    RReilAddr startAddress = getStartAddress(binary);
    SystemModel systemModel = SystemModelRegistry.getLinuxModel(binary); // for now the only thing we have
    return runAnalysis(binary, systemModel, startAddress, debug, registry);
  }

  /**
   * Default uses the Fixpoint Analysis with procedure summaries (not the CallString Method).
   */
  private Analysis<?> runAnalysis (Binary binary, SystemModel model,
      RReilAddr startAddress, AnalysisDebugHooks debug, Callbacks registry) {
    AnalysisEnvironment environment = new AnalysisEnvironment(model, registry);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Analysis<?> analysis = new FixpointAnalysis(environment, binary, initialDomain());
    analysis.setDebugHooks(debug);
    analysis.runFrom(startAddress);
    return analysis;
  }

  /**
   * Default uses the Fixpoint Analysis with procedure summaries (not the CallString Method).
   */
  public Analysis<?> runAnalysis (Binary binary, Platform platform,
      RReilAddr startAddress, AnalysisDebugHooks debug, Callbacks registry) {
    AnalysisEnvironment environment = new AnalysisEnvironment(platform, registry);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Analysis<?> analysis = new FixpointAnalysis(environment, binary, initialDomain());
    analysis.setDebugHooks(debug);
    analysis.runFrom(startAddress);
    return analysis;
  }

  static interface DomainHierarchyFactory {
    public <D extends RootDomain<D>> D build ();
  }

  public static interface AnalysisDebugHooks {
    public <D extends RootDomain<D>> void beforeEval (RReil insn, ProgramPoint point, D domainState, Analysis<D> analysis);

    public <D extends RootDomain<D>> void afterEval (RReil insn, ProgramPoint point, RReilAddr target, D domainState,
        Analysis<D> analysis);
  }
}
