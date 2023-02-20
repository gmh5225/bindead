package javalx.persistentcollections;

import javalx.fn.Fn;

/**
 * Stores the result of a three-way-split {@link FiniteMap#split(javalx.persistentcollections.FiniteMap)}.
 */
public class ThreeWaySplit<T> {
  private final T first;
  private final T second;
  private final T bothButDiffering;

  private ThreeWaySplit (T a, T b, T c) {
    first = a;
    bothButDiffering = b;
    second = c;
  }

  public T onlyInFirst () {
    return first;
  }

  public T onlyInSecond () {
    return second;
  }

  /**
   * Return all mappings from the first result that are mapped to a different value in the second argument.
   *
   * @return mappings of the first argument
   */
  public T inBothButDiffering () {
    return bothButDiffering;
  }

  public static <A, B> ThreeWaySplit<B> map (ThreeWaySplit<A> p3, Fn<A, B> fn) {
    return make(fn.apply(p3.onlyInFirst()), fn.apply(p3.inBothButDiffering()), fn.apply(p3.onlyInSecond()));
  }

  public static <T> ThreeWaySplit<T> make (T l, T b, T r) {
    return new ThreeWaySplit<T>(l, b, r);
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    builder.append(first);
    builder.append(", ");
    builder.append(bothButDiffering);
    builder.append(", ");
    builder.append(second);
    builder.append(")");
    return builder.toString();
  }

}
