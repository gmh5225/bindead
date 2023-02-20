package bindead.data;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.mulAdd;
import static bindead.data.Linear.term;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Random;
import java.util.Vector;

import javalx.data.products.P2;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;

import org.junit.Test;

import bindead.data.Linear.Divisor;
import bindead.domains.affine.Substitution;

/**
 * Try to modify Linear expressions.
 */
public class LinearTest {
  private static final NumVar x = NumVar.fresh("x");
  private static final NumVar y = NumVar.fresh("y");
  private static final NumVar z = NumVar.fresh("z");
  private static final NumVar v = NumVar.fresh("v");
  private static final Linear l1 = linear(BigInt.of(1), term(BigInt.of(2), x), term(BigInt.of(-4), y), term(BigInt.of(-4), x));
  private static final Linear l2 = linear(BigInt.of(1), term(BigInt.of(3), z), term(BigInt.of(8), y));

  private final Random r = new Random(293849283l);

  private final NumVar[] vars = {x,y,z,v};

  private Linear genLinear() {
    // generate -1,0,+1
    BigInt c = BigInt.of(r.nextInt(3)-1);
    BigInt c1 = BigInt.of(r.nextInt(3)-1);
    BigInt c2 = BigInt.of(r.nextInt(3)-1);
    BigInt c3 = BigInt.of(r.nextInt(3)-1);
    return linear(c,
        term(c1,vars[r.nextInt(vars.length)]),
        term(c2,vars[r.nextInt(vars.length)]),
        term(c3,vars[r.nextInt(vars.length)]));
  }

  @Test public void testAddTerm() {
    for (int i=0; i<10000; i++) {
      Linear l = genLinear();
      NumVar v = vars[r.nextInt(vars.length)];
      BigInt c = BigInt.of(r.nextInt(3)-1);
      Linear res = l.addTerm(c, v);
      //System.err.println(l+" plus "+ c3 + " * " + v + " is " + res);
      assertThat(res.sane(), is(true));
      assertThat(l.getConstant(), is(res.getConstant()));
      for (Linear.Term t : res) {
        if (t.getId().equalTo(v))
          assertThat(t.getCoeff(), is(l.getCoeff(t.getId()).add(c)));
        else
          assertThat(t.getCoeff(), is(l.getCoeff(t.getId())));
      }
    }
  }

  @Test public void testDropTerm() {
    for (int i=0; i<10000; i++) {
      Linear l = genLinear();
      NumVar v = vars[r.nextInt(vars.length)];
      Linear res = l.dropTerm(v);
      //System.err.println(l+" without " + v + " is " + res);
      assertThat(res.sane(), is(true));
      assertThat(l.getConstant(), is(res.getConstant()));
      for (Linear.Term t : l) {
        if (t.getId().equalTo(v))
          assertThat(res.getCoeff(t.getId()), is(Bound.ZERO));
        else
          assertThat(res.getCoeff(t.getId()), is(t.getCoeff()));
      }
    }
  }

  @Test public void testNormalization () {
    Linear zero = linear(term(Bound.ZERO, x));
    assertThat(zero, is(linear(Bound.ZERO)));
    zero = linear(Bound.ZERO, term(Bound.ZERO, x));
    assertThat(zero, is(linear(Bound.ZERO)));
    zero = linear(term(Bound.ZERO, x), term(Bound.ZERO, y), term(Bound.ZERO, z));
    assertThat(zero, is(linear(Bound.ZERO)));
  }

  @Test public void testMulAdd () {
    BigInt f1 = l2.getCoeff(y);
    BigInt f2 = Bound.ZERO.sub(l1.getCoeff(y));
    Linear l12 = mulAdd(f1, l1, f2, l2);
    System.out.println(f1 + "*" + l1 + " + " + f2 + "*" + l2 + " = " + l12);
    Divisor div = Divisor.one();
    div.mul(f1);
    div.mul(f2);
    l12 = l12.lowestForm(div);
    System.out.println("Lowest form: " + l12);
    assertThat(l12.getTerms().length == 2, is(true));
    assertThat(l12.getCoeff(x).isEqualTo(BigInt.of(-4)), is(true));
    assertThat(l12.getCoeff(y).isZero(), is(true));
    assertThat(l12.getCoeff(z).isEqualTo(BigInt.of(3)), is(true));
    assertThat(l12.getConstant().isEqualTo(BigInt.of(3)), is(true));
  }

  @Test public void testApplySubstitution () {
    // simulate the assignment x := 2x+y+1 in the system x+10=0, thus x' = 2x+y+1 is turned into x / (x'-y-1)/2 and
    // applied to x+10 [x / (x'-y-1)/2] = (x'-y-1)/2+10. Scaling gives x'-y-1+20=x'-y+19=0 thus x' = y-19.
    Linear rhs = linear(BigInt.of(1), term(BigInt.of(2), x), term(BigInt.of(1), y));
    Substitution subst = Substitution.invertingSubstitution(rhs, BigInt.of(1), x);
    System.out.println("substitution is " + subst);
    Linear eqn = linear(BigInt.of(10), term(BigInt.of(1), x));
    Linear res = eqn.applySubstitution(subst);
    System.out.println("applying update x := " + rhs + " to " + eqn + " results in " + res);
    Linear ref = linear(BigInt.of(19), term(BigInt.of(1), x), term(BigInt.of(-1), y));
    assertThat(ref, is(res));
  }

  @Test public void testDiffVars () {
    Linear pos = linear(BigInt.of(0), term(BigInt.of(1), v));
    Linear neg = linear(BigInt.of(1), term(BigInt.of(1), x), term(BigInt.of(1), y), term(BigInt.of(1), z));
    Vector<P2<Boolean, NumVar>> diff = Linear.diffVars(pos, neg);
    Vector<P2<Boolean, NumVar>> ref = new Vector<P2<Boolean, NumVar>>();
    ref.add(new P2<Boolean, NumVar>(true,x));
    ref.add(new P2<Boolean, NumVar>(true,y));
    ref.add(new P2<Boolean, NumVar>(true,z));
    ref.add(new P2<Boolean, NumVar>(false,v));
    assertThat(diff, is(ref));
  }

  @Test public void dropTermGcd001 () {
    Divisor gcd = Divisor.one();
    Linear l1 = linear(term(x), term(BigInt.of(4), y), term(BigInt.of(4), z));
    Linear l2 = l1.dropTerm(x);
    l2 = l2.lowestForm(); // or lowestForm(gcd) ?
    assertThat(gcd.get(), is(BigInt.of(1))); // XXX: 4?
    assertThat(l2.getCoeff(y), is(BigInt.of(1)));
    assertThat(l2.getCoeff(z), is(BigInt.of(1)));
  }

  @Test public void dropTermGcd002 () {
    Divisor gcd = Divisor.one();
    Linear l1 = linear(term(x), term(BigInt.of(4), y), term(BigInt.of(4), z));
    Linear l2 = l1.dropTerm(y);
    l2 = l2.lowestForm(gcd);
    assertThat(gcd.get(), is(BigInt.of(1)));
    assertThat(l2.getCoeff(z), is(BigInt.of(4)));
    assertThat(l2.getVars().contains(y), is(false));
  }

  @Test public void testRenaming () {
    Linear rhs = linear(BigInt.of(1), term(BigInt.of(2), x), term(BigInt.of(1), y));
    FoldMap pairs = new FoldMap();
    pairs.add(x,y);
    Linear renamed = rhs.renameVar(pairs);
    renamed = renamed.renameVar(pairs);
    assertThat(rhs, is(renamed));
  }
}