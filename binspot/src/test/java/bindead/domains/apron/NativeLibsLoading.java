package bindead.domains.apron;

import static bindead.debug.DebugHelper.logln;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import gmp.Mpfr;

import org.junit.Before;
import org.junit.Test;

import apron.Abstract1;
import apron.Environment;
import apron.Linexpr0;
import apron.Linterm0;
import apron.MpfrScalar;
import apron.MpqScalar;
import apron.Polka;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import bindead.debug.DebugHelper;

/**
 * Tests to see if the Apron native library is working correct.
 *
 * @author Bogdan Mihaila
 */
public class NativeLibsLoading {

  @Before public void precondition () {
    // ignore Tests if the preconditions (native library installed) are not satisfied
    assumeTrue(haveApronNativeLibraries());
  }

  @Test public void simpleInstantiation () throws Exception {
//    DebugHelper.analysisKnobs.printLogging();
    // Apron level 0 API
    apron.Interval[] box = {new apron.Interval(1, 2), new apron.Interval(-3, 5), new apron.Interval(3, 4, 6, 5)};
    for (apron.Interval interval : box) {
      logln(interval);
    }
    Linterm0[] ltrms =
    {new Linterm0(1, new MpqScalar(-5)),
      new Linterm0(0, new apron.Interval(0.1, 0.6)),
      new Linterm0(2, new MpfrScalar(0.1, Mpfr.RNDU))
    };
    Linexpr0 linexp = new Linexpr0(ltrms, new MpqScalar(2));
    logln(linexp);
    assertTrue(true);
  }

  /**
   * Apron versions < 0.9.11 do not have a power operator which we need.
   * So this test will just crash with an error in the console for lower versions.
   */
  @Test public void powerOperatorExistence () throws Exception {
    Environment env = new Environment();
    Abstract1 state = new Abstract1(new Polka(false), env);
    Texpr1BinNode constants =
      new Texpr1BinNode(Texpr1BinNode.OP_POW, new Texpr1CstNode(new MpqScalar(2)), new Texpr1CstNode(new MpqScalar(5)));
    new Texpr1Intern(state.getEnvironment(), constants);
    // the error is being reported on stderr so it is difficult to catch here and let the test fail
    assertTrue(true);
  }

  /**
   * Runs the tests that come with the Apron java bindings.
   */
  @Test public void apronTests () throws Exception {
    DebugHelper.muteSystemOut();
    gmp.Test.main(new String[0]);
    apron.Test.main(new String[0]);
    DebugHelper.unmuteSystemOut();
  }

  /**
   * See if the native lib for apron is present on this machine.
   */
  public static boolean haveApronNativeLibraries () {
    try {
      System.loadLibrary("japron");
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
    return true;
  }

}
