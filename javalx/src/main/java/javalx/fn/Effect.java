package javalx.fn;


/**
 * Representing side-effecting functions.
 *
 * @param <A>
 */
public abstract class Effect<A> {
  /**
   * Apply value a to this side-effecting function.
   *
   * @param a the effect's argument.
   */
  public abstract void observe (A a);



  public static <A> Effect<A> ignoring () {
    return new Effect<A>() {
      @Override
      public void observe (A _) {
      }
    };
  }
}
