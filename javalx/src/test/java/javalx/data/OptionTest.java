package javalx.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javalx.fn.Fn;

import org.junit.Test;

public class OptionTest {
  @Test public void testOptionSuccOfNone () {
    Option<Integer> noneInt = Option.none();
    Fn<Integer, Integer> succ = new Fn<Integer, Integer>() {
      @Override public Integer apply (Integer n) {
        return n + 1;
      }
    };
    Option<Integer> succOfNoneInt = noneInt.fmap(succ);
    assertTrue(succOfNoneInt.isNone());
  }

  @Test public void testOptionSomeIntToString () {
    Option<Integer> someInt = Option.some(1);
    Fn<Integer, String> tostring = new Fn<Integer, String>() {
      @Override public String apply (Integer n) {
        return n.toString() + "XXX";
      }
    };
    Option<String> stringOfSomeInt = someInt.fmap(tostring);
    assertEquals("1XXX", stringOfSomeInt.get());
  }

  @Test public void testOptionNoneIntToString () {
    Option<Integer> noneInt = Option.none();
    Fn<Integer, String> tostring = new Fn<Integer, String>() {
      @Override public String apply (Integer n) {
        return n.toString() + "XXX";
      }
    };
    Option<String> stringOfNoneInt = noneInt.fmap(tostring);
    assertTrue(stringOfNoneInt.isNone());
  }
}
