package bindead.domains;

import static bindead.TestsHelper.evaluateAssertions;
import static bindead.TestsHelper.lines;

import org.junit.Test;

/**
 * Test the various assertions that can be expressed in RREIL assembler.
 *
 * @author Bogdan Mihaila
 */
public class Assertions {

  @Test public void examples1 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov x, [1, 2]",
//        "assert x <= [1, 2]",   // this fails
        "assert x <= 2",
//        "assert x < 2",         // this fails
        "assert x <= [2, 3]",
//        "assert [1, 2] <= x",   // this fails
        "assert 1 <= x",
//        "assert 1 < x",        // this fails
        "assert x = [1, 2]",
        "assert x != [3, 4]",
//        "assert x != [1, 2]",   // this fails
//        "assert x != 2",        // this fails
        "halt");
    evaluateAssertions(assembly);
  }

  /**
   * Tests linear equalities between variables.
   */
  @Test public void examples2 () {
    String assembly = lines(
        "option DEFAULT_SIZE = 32",
        "mov a, [1, 2]",
        "mov x, [1, 2]",
        "mov y, x",
        "mov z, y",
        "assert x *+= z",
//        "assert a *+= z",        // this fails
        "halt");
    evaluateAssertions(assembly);
  }

}
