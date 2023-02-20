package bindead.platforms;

import bindead.analyses.Analysis;
import bindead.analyses.ForwardAnalysis;
import bindead.analyses.WorklistSolver;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domains.affine.Affine;
import bindead.domains.fields.Fields;
import bindead.domains.intervals.Intervals;
import bindead.domains.narrowing.Narrowing;
import bindead.domains.pointsto.PointsTo;
import bindead.domains.wrapping.Wrapping;
import bindead.domains.root.Root;
import bindead.domains.predicate.Predicate;
import bindead.domains.stripe.Stripes;
import bindead.platforms.RReil32Platform;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rreil.cfa.Cfa;
import rreil.disassembly.DisassemblyProvider;
import rreil.assembler.parsetree.ParseException;

/**
 */
public class RReilAnalysisHelpers {
  public static Analysis<?> runFile (String filePath) throws IOException {
    try {
      Cfa cfa = AssemblyLoader.loadCfa(filePath);
      return run(cfa);
    } catch (ParseException ex) {
      Logger.getLogger(RReilAnalysisHelpers.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static Analysis<?> run (String assembly) throws IOException {
    return run(assembly, initialState());
  }

  public static Analysis<?> run (String assembly, RootDomain<?> initialState) throws IOException {
    try {
      Cfa cfa = AssemblyLoader.loadInlineAssembly(assembly);
      return run(cfa, initialState);
    } catch (ParseException ex) {
      Logger.getLogger(RReilAnalysisHelpers.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static Analysis<?> run (Cfa cfa) throws IOException {
    return run(cfa, initialState());
  }

  public static Analysis<?> run (Cfa cfa, RootDomain<?> initialState) throws IOException {
    return run(cfa, null, initialState);
  }

  public static Analysis<?> run (Cfa cfa, DisassemblyProvider provider, RootDomain<?> initialState) throws IOException {
    @SuppressWarnings({"rawtypes", "unchecked"})
    ForwardAnalysis<?> analysis = new ForwardAnalysis(RReil32Platform.$Instance, cfa, provider, initialState);
    analysis.setStartVertex(cfa.getEntry());
    analysis.bootstrapState(cfa.getEntry(), RReil32Platform.$Instance.forwardAnalysisBootstrap());
    WorklistSolver solver = new WorklistSolver(analysis);
    solver.solve();
    return analysis;
  }

//  @SuppressWarnings({"rawtypes", "unchecked"})
//  public static RootDomain<?> initialState () {
//    return new Root(new Fields(new Wrapping(new PointsTo(new Affine(new Predicates(new Intervals()))))));
//  }
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static RootDomain<?> initialState () {
    return new Root(new Fields(new Predicate(new PointsTo(new Wrapping(new Affine(new Stripes(new Narrowing(new Intervals()))))))));
  }
}
