package bindead.domains.sat;

import static bindead.debug.DebugHelper.logln;

import java.util.HashSet;

import org.junit.Test;

import satDomain.Flag;
import satDomain.Numeric;

public class ExpandTest {

  @Test public void test () {
//    DebugHelper.analysisKnobs.printLogging();
    final Numeric n = new Numeric();
    logln(n);
    final Flag v1 = n.freshTopVar();
    logln(n);
    final Flag v2 = n.freshTopVar();
    logln(n);
    n.assumeEqual(v1, v2);
    logln(n);
    final HashSet<Flag> e = new HashSet<Flag>();
    e.add(v2);
    e.add(v1);
    n.expand("test", e);
    logln(n);
  }
}
