package bindead.data;

import static bindead.debug.DebugHelper.logln;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import bindead.debug.DebugHelper;
import javalx.numeric.BigInt;

public class VarSetTest {
  private BitSet ref;
  private VarSet vs;
  private static NumVar[] vars = new NumVar[2010];
  static {
    for (int i = 0; i < 2010; i++)
      vars[i] = NumVar.fresh();
  }

  /**
   * Silence any debug output that was enabled by previous tests.
   */
  @Before public void silence () {
    DebugHelper.analysisKnobs.disableAll();
  }

  private void superTester (int[] testData) {
    ref = new BitSet();
    vs = VarSet.empty();
    vs = fill(vs, ref, testData);
    logln("VarSet: " + vs + ", BitSet: " + ref);
    assertThat(vs.size(), is(ref.cardinality()));
    for (NumVar v : vs)
      assertThat (ref.get(v.getStamp()), is(true));
  }

  private static VarSet fill (VarSet vs, BitSet ref, int[] testData) {
    for (int i = 0; i < testData.length; i++) {
      int b = testData[i];
      if (b < 0) {
        ref.clear(vars[-b].getStamp());
        vs = vs.remove(vars[-b]);
      } else if (b > 0) {
        ref.set(vars[b].getStamp());
        vs = vs.add(vars[b]);
      }
    }
    return vs;
  }

  @Test public void testCheckPreviousNeighbour () {
    superTester(new int[] {4, 1, 5, 2, -1, 3});
  }

  @Test public void testSingleInsertRemove () {
    superTester(new int[] {4, -4});
  }

  @Test public void testDoubleInsertRemove () {
    superTester(new int[] {3, 4, -3, -4});
  }

  @Test public void testShortSequenceRemove () {
    superTester(new int[] {1, 2, 3, 4, -1, -2, -3});
  }

  @Test public void testLongSequenceRemove () {
    superTester(new int[] {1, 2, 3, 4, -2, -1, -3});
  }

  @Test public void testRandom () {
    long seed = System.currentTimeMillis();
    for (int times = 0; times < 100; times++) {
      testRandom(seed);
      seed++;
    }
  }

  private void testRandom (long seed) {
    int[] data = new int[2000];
    Random r = new Random(seed);
    final int range = 20;
    for (int i = 0; i < data.length; i++) {
      data[i] = r.nextInt(range) + 1;
      // set more bits in the first half and reset more in the second
      int negate = r.nextInt(256);
      if (((i < data.length / 2) && (negate < 100)) ||
        ((i >= data.length / 2) && (negate < 200)))
        data[i] = -data[i];
    }
    logln("testing random set with seed " + seed);
    superTester(data);
  }

  @Test public void testMerge () {
    long seed = System.currentTimeMillis();
    for (int times = 0; times < 100; times++) {
      testRandom(seed++);
      BitSet ref2 = ref;
      VarSet vs2 = vs;
      testRandom(seed++);
      ref.or(ref2);
      vs = vs.union(vs2);
      logln("union of last two sets:");
      logln("VarSet: " + vs + ", BitSet: " + ref);
      assertThat(vs.size(), is(ref.cardinality()));
      for (NumVar v : vs)
        assertThat (ref.get(v.getStamp()), is(true));
    }
  }

  @Test public void testIntersection () {
    long seed = System.currentTimeMillis();
    for (int times = 0; times < 100; times++) {
      testRandom(seed++);
      BitSet ref2 = ref;
      VarSet vs2 = vs;
      testRandom(seed++);
      ref.and(ref2);
      vs = vs.intersection(vs2);
      logln("intersection of last two sets:");
      logln("VarSet: " + vs + ", BitSet: " + ref);
      assertThat(vs.size(), is(ref.cardinality()));
      for (NumVar v : vs)
        assertThat (ref.get(v.getStamp()), is(true));
    }
  }

  @Test public void testSetminus () {
    long seed = System.currentTimeMillis();
    for (int times = 0; times < 100; times++) {
      testRandom(seed++);
      BitSet ref2 = ref;
      VarSet vs2 = vs;
      testRandom(seed++);
      int bit = ref2.nextSetBit(0);
      while (bit != -1) {
        ref.clear(bit);
        bit = ref2.nextSetBit(bit + 1);
      }
      vs = vs.difference(vs2);
      logln("set difference of last two sets:");
      logln("VarSet: " + vs + ", BitSet: " + ref);
      assertThat(vs.size(), is(ref.cardinality()));
      for (NumVar v : vs)
        assertThat (ref.get(v.getStamp()), is(true));
    }
  }

  @Test public void testSubset () {
    long seed = System.currentTimeMillis();
    for (int times = 0; times < 1000; times++) {
      testRandom(seed++);
      BitSet ref2 = ref;
      VarSet vs2 = vs;
      testRandom(seed++);
      BitSet merge = (BitSet) ref.clone();
      merge.or(ref2);
      boolean refSubset = merge.equals(ref);
      boolean vsSubset = vs.containsAll(vs2);
      logln("subset test of last two sets:");
      logln("VarSet: " + vsSubset + ", BitSet: " + refSubset);
      assertThat(vsSubset, is(refSubset));
    }
  }

  @Test public void testSubstitute () {
    long seed = System.currentTimeMillis();
    Random rand = new Random(seed);
    Map<NumVar, NumVar> substitutions = new HashMap<NumVar, NumVar>();
    for (int i = 0; i < 100; i++) {
      int x = rand.nextInt(vars.length);
      int y = rand.nextInt(vars.length);
      substitutions.put(vars[x], vars[y]);
    }
    VarSet set = VarSet.empty();
    for (NumVar var : vars) {
      set = set.add(var);
    }
    VarSet keys = VarSet.empty();
    for (NumVar var : substitutions.keySet()) {
      keys = keys.add(var);
    }
    VarSet values = VarSet.empty();
    for (NumVar var : substitutions.values()) {
      values = values.add(var);
    }
    assertThat(set.containsAll(keys), is(true));
    assertThat(set.containsAll(values), is(true));
    set = set.substitute(substitutions);
    assertThat(set.containsAll(keys), is(false));
    assertThat(set.containsAll(values), is(true));
  }
}