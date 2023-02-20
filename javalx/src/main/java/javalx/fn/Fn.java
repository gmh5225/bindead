package javalx.fn;

import java.util.ArrayList;
import java.util.List;

/**
 * A function taking a single argument of type A and returning values of type B encoded as object (closure).
 *
 * @param <A> the type of the functions argument.
 * @param <B> the result type of the function.
 */
public abstract class Fn<A, B> {


  /**
   * Apply value a to this function.
   *
   * @param a the function's only argument.
   *
   * @return the result of applying this function to its argument
   */
  public abstract B apply (A a);

  /**
   * Function composition.
   *
   * f.o(g) == f o g == \x -> f(g(x))
   *
   * @param <C>
   * @param g the function to compose with this one.
   *
   * @return the function composition (this o g).
   */
  private final <C> Fn<C, B> o (final Fn<C, A> g) {
    return new Fn<C, B>() {
      @Override
      public B apply (C c) {
        return Fn.this.apply(g.apply(c));
      }
    };
  }

  /**
   * Flipped function composition.
   *
   * f.andThen(g) == g o f = \x -> g(f(x)) == g.o(f)
   *
   * @param g
   * @param <C>
   *
   * @return the (flipped) function composition (g o this)
   */
  public final <C> Fn<A, C> andThen (final Fn<B, C> g) {
    return g.o(this);
  }

  public final Fn<A, List<B>> times (final int n) {
    return new Fn<A, List<B>>() {
      @Override
      public List<B> apply (final A a) {
        List<B> results = new ArrayList<B>();
        for (int i = 0; i < n; i++) {
          final B b = Fn.this.apply(a);
          results.add(b);
        }

        return results;
      }
    };
  }
}
