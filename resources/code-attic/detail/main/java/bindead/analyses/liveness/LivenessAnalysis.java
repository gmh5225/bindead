package bindead.analyses.liveness;

import bindead.analyses.Analysis;
import bindead.analyses.BackwardAnalysis;
import bindead.analyses.WorklistSolver;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domains.liveness.FieldLiveness;
import bindead.domains.liveness.LivenessFinitePart;
import bindead.domains.liveness.LivenessRootPart;
import bindead.platforms.Platforms.AnalysisPlatform;
import javalx.digraph.Digraph.Vertex;
import rreil.cfa.Cfa;
import rreil.cfa.util.CfaBuilder;
import rreil.cfa.CompositeCfa;

/**
 */
public class LivenessAnalysis {
  private final AnalysisPlatform platform;
  private final CompositeCfa ccfa;

  public LivenessAnalysis (CompositeCfa cfa, AnalysisPlatform platform) {
    this.ccfa = cfa;
    this.platform = platform;
  }

  public LivenessAnalysis (AnalysisPlatform platform) {
    this(null, platform);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static RootDomain<?> initialDomainHierarchy () {
    return new LivenessRootPart(new FieldLiveness(new LivenessFinitePart()));
  }

  public static Cfa run (Cfa cfa, AnalysisPlatform platform) {
    LivenessAnalysis analysis = new LivenessAnalysis(platform);
    return analysis.run(cfa);
  }

  public CompositeCfa runOnAll () {
    CompositeCfa cleanedForest = new CompositeCfa(null);
    for (Cfa cfa : ccfa) {
      cleanedForest.put(run(cfa));
    }
    return cleanedForest;
  }

  private Cfa run (Cfa cfa) {
    @SuppressWarnings({"rawtypes", "unchecked"})
    Analysis<?> analysis = new BackwardAnalysis(cfa, initialDomainHierarchy());
    Vertex start = cfa.getExit();
    analysis.setStartVertex(start);
    analysis.bootstrapState(start, platform.livenessBootstrap());
    // Start fixpoint solver using the given analysis.
    WorklistSolver solver = new WorklistSolver(analysis);
    solver.solve();
    // Mark dead code
    DeadCodeAnalyzer deadCode = new DeadCodeAnalyzer(analysis);
    deadCode.run();
    return CfaBuilder.removeDeadCode(cfa, deadCode.getDeadCodeAddresses());
  }

  public LivenessAnalysisCtx runWithResults (Cfa cfa) {
    @SuppressWarnings({"rawtypes", "unchecked"})
    Analysis<?> analysis = new BackwardAnalysis(cfa, initialDomainHierarchy());
    Vertex start = cfa.getExit();
    analysis.setStartVertex(start);
    analysis.bootstrapState(start, platform.livenessBootstrap());
    // Start fixpoint solver using the given analysis.
    WorklistSolver solver = new WorklistSolver(analysis);
    solver.solve();
    // Mark dead code
    DeadCodeAnalyzer deadCode = new DeadCodeAnalyzer(analysis);
    deadCode.run();
    Cfa cleanCfa = CfaBuilder.removeDeadCode(cfa, deadCode.getDeadCodeAddresses());
    LivenessAnalysisCtx result = new LivenessAnalysisCtx();
    result.put(cleanCfa, analysis);
    return result;
  }

  public CompositeCfa getCompositeCfa () {
    return ccfa;
  }

  public AnalysisPlatform getPlatform () {
    return platform;
  }
}
