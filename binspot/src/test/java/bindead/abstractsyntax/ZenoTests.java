package bindead.abstractsyntax;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Random;

import javalx.numeric.BigInt;
import javalx.persistentcollections.AVLSet;

import org.junit.Test;

import bindead.abstractsyntax.zeno.Zeno;
import bindead.abstractsyntax.zeno.Zeno.ZenoTestOp;
import bindead.abstractsyntax.zeno.ZenoFactory;
import bindead.data.Linear;
import bindead.data.NumVar;

public class ZenoTests {
  private static final ZenoFactory zeno = ZenoFactory.getInstance();
  private static final AVLSet<Zeno.Test> empty = AVLSet.<Zeno.Test>empty();
  private static final NumVar var1 = NumVar.fresh();
  private static final NumVar var2 = NumVar.fresh();
  private static final NumVar var3 = NumVar.fresh();

  @Test public void test001 () {
    Zeno.Test t1 = zeno.comparison(zeno.linear(var1), ZenoTestOp.LessThanOrEqualToZero);
    Zeno.Test t2 = zeno.comparison(zeno.linear(var1), ZenoTestOp.LessThanOrEqualToZero);
    assertThat(t1.compareTo(t2), is(0));
  }

  @Test public void test002 () {
    AVLSet<Zeno.Test> set = empty;
    Zeno.Test t1 = zeno.comparison(zeno.linear(var1), ZenoTestOp.LessThanOrEqualToZero);
    Zeno.Test t2 = zeno.comparison(zeno.linear(var1), ZenoTestOp.LessThanOrEqualToZero);
    set = set.add(t1);
    set = set.remove(t2);
    assertThat(set.size(), is(0));
  }

  @Test public void test003 () {
    Linear l1 = Linear.linear(Linear.term(BigInt.of(-8), var1), Linear.term(var2)).add(BigInt.of(36));
    Linear l2 = Linear.linear(Linear.term(var2), Linear.term(BigInt.of(-8), var1)).add(BigInt.of(36));
    Zeno.Test t1 = zeno.comparison((l1), ZenoTestOp.LessThanOrEqualToZero);
    Zeno.Test t2 = zeno.comparison((l2), ZenoTestOp.LessThanOrEqualToZero);
    assertThat(t1.compareTo(t2), is(0));
  }

  @Test public void test004 () {
    AVLSet<Zeno.Test> set = empty;
    Linear l1 = Linear.linear(Linear.term(BigInt.of(-8), var1), Linear.term(var2)).add(BigInt.of(36));
    Linear l2 = Linear.linear(Linear.term(var2), Linear.term(BigInt.of(-8), var1)).add(BigInt.of(36));
    Zeno.Test t1 = zeno.comparison((l1), ZenoTestOp.LessThanOrEqualToZero);
    Zeno.Test t2 = zeno.comparison((l2), ZenoTestOp.LessThanOrEqualToZero);
    Zeno.Test t3 = zeno.comparison(zeno.linear(var3), ZenoTestOp.LessThanOrEqualToZero);
    set = set.add(t1).add(t3);
    set = set.remove(t2);
    assertThat(set.size(), is(1));
  }

  @Test public void test005 () {
    AVLSet<Zeno.Test> set = empty;
    Random random = new Random();
    for (int i = 0; i < 10000; i++) {
      NumVar var1 = NumVar.fresh();
      NumVar var2 = NumVar.fresh();
      int rnd = random.nextInt(1000);
      Zeno.Test t1 =
        zeno.comparison((Linear.linear(BigInt.of(rnd), Linear.term(var1), Linear.term(var2))),
            ZenoTestOp.LessThanOrEqualToZero);
      Zeno.Test t2 =
        zeno.comparison((Linear.linear(BigInt.of(rnd), Linear.term(var2), Linear.term(var1))),
            ZenoTestOp.LessThanOrEqualToZero);
      assertThat(t1.compareTo(t2), is(0));
      assertThat(t2.compareTo(t1), is(0));
      set = set.add(t1);
    }
    for (Zeno.Test t : set) {
      assertThat(t.compareTo(t), is(0));
      assertThat(set.contains(t), is(true));
    }
  }

  @Test public void test006 () {
    AVLSet<Zeno.Test> set = empty;
    Zeno.Test t1 = zeno.comparison(zeno.linear(var1), ZenoTestOp.LessThanOrEqualToZero);
    Zeno.Test t2 =
      zeno.comparison((Linear.linear(Linear.term(var2), Linear.term(var3))), ZenoTestOp.LessThanOrEqualToZero);
    Zeno.Test t3 =
      zeno.comparison((Linear.linear(Linear.term(var1), Linear.term(var3))), ZenoTestOp.LessThanOrEqualToZero);
    set = set.add(t1).add(t2).add(t3);
    assertThat(set.size(), is(3));
    for (Zeno.Test t : set) {
      assertThat(t.compareTo(t), is(0));
      assertThat(set.contains(t), is(true));
    }
  }
}
