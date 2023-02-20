package javalx.fn;

/**
 * A function taking a two arguments.
 *
 * @param <A> the type of the function's first argument.
 * @param <B> the type of the function's second argument.
 * @param <C> the result type of the function.
 */
public abstract class Fn2<A, B, C> {
  /**
   * Apply value a to this function.
   *
   * @param a the function's first argument.
   * @param b the function's second argument.
   *
   * @return the result of applying this function to its arguments.
   */
  public abstract C apply (A a, B b);

  public final Fn2<B, A, C> flip () {
    return new Fn2<B, A, C>() {
      @Override
      public C apply (final B b, final A a) {
        return Fn2.this.apply(a, b);
      }
    };
  }

  /**
   * Transform this function into its curried form.
   */
  public final Fn<A, Fn<B, C>> curry () {
    return new Fn<A, Fn<B, C>>() {
      @Override
      public Fn<B, C> apply (final A a) {
        return new Fn<B, C>() {
          @Override
          public C apply (final B b) {
            return Fn2.this.apply(a, b);
          }
        };
      }
    };
  }

  /**
   * Returns a function that does return its first argument.
   */
  public static <A, B> Fn2<A, B, A> __1 () {
    return new Fn2<A, B, A>() {
      @Override
      public A apply (final A a, final B b) {
        return a;
      }
    };
  }

  /**
   * Returns a function that does return its second argument.
   */
  public static <A, B> Fn2<A, B, B> __2 () {
    return new Fn2<A, B, B>() {
      @Override
      public B apply (final A a, final B b) {
        return b;
      }
    };
  }
}
