package bindead.domains.heap;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import bindead.FiniteDomainHelper;
import bindead.FiniteDomainHelper.RichFiniteDomain;
import bindead.abstractsyntax.finite.Finite;
import bindead.abstractsyntax.finite.FiniteFactory;
import bindead.analyses.DomainFactory;
import bindead.data.ListVarPair;
import bindead.data.NumVar;
import bindead.debug.DebugHelper;
import bindead.domainnetwork.interfaces.FiniteDomain;
import bindead.domains.apron.NativeLibsLoading;

@SuppressWarnings({"rawtypes"})
public class HolgersSummarizationTests {
  private static final NumVar x1 = NumVar.fresh("x1");
  private static final NumVar x2 = NumVar.fresh("x2");
  private static final NumVar x3 = NumVar.fresh("x3");
  private static final NumVar x4 = NumVar.fresh("x4");

  private static FiniteDomain genInitialSystem (FiniteDomain domain) {
    RichFiniteDomain d = FiniteDomainHelper.for32bitVars(domain);
    d = d.introduce(x1, 0);
    d = d.introduce(x2, 1);
    d = d.introduce(x3, 1);
    d = d.introduce(x4, 2);
    return d.getWrappedDomain();
  }

  private static ListVarPair genVarPair () {
    ListVarPair vp = new ListVarPair();
    vp.add(x1, x3);
    vp.add(x2, x4);
    return vp;
  }

  Finite.Test t0 = FiniteFactory.getInstance().equalToZero(31, x1);
  Finite.Test t1 = FiniteFactory.getInstance().equalToOne(31, x2);

  @Test public void testOcti () {
//    DebugHelper.analysisKnobs.printLogging();
    // ignore Tests if the preconditions (native library installed) are not satisfied
    assumeTrue(NativeLibsLoading.haveApronNativeLibraries());
    FiniteDomain octi = DomainFactory.parseFiniteDomain("Wrapping Apron(Octagons)");

    DebugHelper.analysisKnobs.printFullDomain();
    FiniteDomain d0 = genInitialSystem(octi);
    DebugHelper.logln("Domain: " + d0);
    ListVarPair pairs = genVarPair();
    FiniteDomain d = d0.foldNG(pairs);
    DebugHelper.logln("Folded with mapping: " + pairs + "\nResult: " + d);
    d = d.eval(t0);
    DebugHelper.logln("Reconstructed first pair <x1,x2>==<0,1>\nResult: " + d);
    d = d.eval(t1);
    DebugHelper.logln("Reconstructed first pair <x1,x2>==<0,1>\nResult: " + d);
  }
}
