package bindead.domains.gauge;

import static bindead.data.Linear.linear;
import static bindead.data.Linear.term;
import static bindead.domains.gauge.Wedge.wedge;
import static javalx.data.Option.some;
import static javalx.data.products.P2.tuple2;
import static org.junit.Assert.assertEquals;
import javalx.numeric.BigInt;
import javalx.numeric.Bound;

import org.junit.Test;

import bindead.data.NumVar;

/** TODO  add more tests. */
public class WedgeTest {

  // counters
  static final NumVar l1 = NumVar.fresh("l1");
  static final NumVar l2 = NumVar.fresh("l2");
  static final NumVar l3 = NumVar.fresh("l3");

  // other variables
  static final NumVar x = NumVar.fresh("x");
  static final NumVar y = NumVar.fresh("y");
  static final NumVar z = NumVar.fresh("z");

  static final BigInt TWO = Bound.ONE.add(Bound.ONE);

  @Test
  public void joinTest1() {
	  Wedge a =  wedge(tuple2(some(linear(Bound.ZERO)),some(linear(Bound.ZERO))));
	  Wedge b =  wedge(tuple2(some(linear(Bound.ONE)),some(linear(Bound.ONE))));
	  Wedge a_join_b =  wedge(tuple2(some(linear(Bound.ZERO)),some(linear(Bound.ONE))));
	  assertEquals((a_join_b),a.join(b));
  }

  @Test
  public void widenTest1() {
	  Wedge a =  wedge(tuple2(some(linear(Bound.ZERO)),some(linear(Bound.ZERO))));
	  Wedge b =  wedge(tuple2(some(linear(Bound.ONE)),some(linear(Bound.ONE.add(Bound.ONE)))));
	  Wedge a_widen_b =  wedge(tuple2(some(linear(term(Bound.ONE,l1))),
			  						 some(linear(term(Bound.ONE.add(Bound.ONE),l1)))));
	  assertEquals((a_widen_b),a.widenLI(b,l1,Bound.ZERO,Bound.ONE));
  }

  @Test
  public void incTest1() {
	  Wedge a =  wedge(tuple2(some(linear(TWO,term(TWO,l1))),some(linear(TWO,term(TWO, l1)))));
	  Wedge a_inc_l1 = wedge(tuple2(some(linear(Bound.ZERO,term(TWO,l1))),some(linear(Bound.ZERO,term(TWO, l1)))));
	  assertEquals(a_inc_l1, a.inc(l1, Bound.ONE));
  }



}
