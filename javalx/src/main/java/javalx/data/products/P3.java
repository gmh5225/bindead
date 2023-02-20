package javalx.data.products;

/**
 * A product-3 (tuple).
 *
 * @param <A> The type of the first element
 * @param <B> The type of the second element
 * @param <C> The type of the third element
 */
public class P3<A, B, C> {
  private final A a;
  private final B b;
  private final C c;

  public P3 (A a, B b, C c) {
    this.a = a;
    this.b = b;
    this.c = c;
  }

  public static <A, B, C> P3<A, B, C> tuple3 (A a, B b, C c) {
    return new P3<A, B, C>(a, b, c);
  }

  public A _1 () {
    return a;
  }

  public B _2 () {
    return b;
  }

  public C _3 () {
    return c;
  }

  @Override public String toString () {
    return "(" + a + ", " + b + ", " + c + ")";
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + (a == null ? 0 : a.hashCode());
    result = prime * result + (b == null ? 0 : b.hashCode());
    result = prime * result + (c == null ? 0 : c.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof P3))
      return false;
    P3<?, ?, ?> other = (P3<?, ?, ?>) obj;
    if (a == null) {
      if (other.a != null)
        return false;
    } else if (!a.equals(other.a))
      return false;
    if (b == null) {
      if (other.b != null)
        return false;
    } else if (!b.equals(other.b))
      return false;
    if (c == null) {
      if (other.c != null)
        return false;
    } else if (!c.equals(other.c))
      return false;
    return true;
  }
}
