package bindead.data;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import rreil.lang.RReilAddr;
import bindead.analyses.algorithms.data.CallString;
import bindead.analyses.algorithms.data.CallString.Transition;

public class CallStringTest {

  @Test public void equality () {
    Set<CallString> set = new HashSet<>();
    CallString root = CallString.root();
    CallString cs1 = root.push(new Transition(RReilAddr.valueOf(1), RReilAddr.valueOf(2)));
    CallString cs2 = root.push(new Transition(RReilAddr.valueOf(1), RReilAddr.valueOf(2)));
    set.add(cs1);
    assertThat(set, hasItem(cs2));
  }

}