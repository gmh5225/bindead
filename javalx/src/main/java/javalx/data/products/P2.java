package javalx.data.products;

/**
 * A product-2 (tuple).
 *
 * @param <A> The type of the first element
 * @param <B> The type of the second element
 */
public class P2<A, B> {
  public final A fst;
  public final B snd;

  public P2 (A a, B b) {
    this.fst = a;
    this.snd = b;
  }

  public static <A, B> P2<A, B> tuple2 (A a, B b) {
    return new P2<A, B>(a, b);
  }

  /**
   * @return the first element of the product.
   */
  final public A _1 () {
    return fst;
  }

  /**
   * @return the second element of the product.
   */
  final public B _2 () {
    return snd;
  }

  @Override public String toString () {
    return "(" + fst + ", " + snd + ")";
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + (fst == null ? 0 : fst.hashCode());
    result = prime * result + (snd == null ? 0 : snd.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof P2))
      return false;
    P2<?, ?> other = (P2<?, ?>) obj;
    if (fst == null) {
      if (other.fst != null)
        return false;
    } else if (!fst.equals(other.fst))
      return false;
    if (snd == null) {
      if (other.snd != null)
        return false;
    } else if (!snd.equals(other.snd))
      return false;
    return true;
  }
}
