package javalx.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javalx.fn.Fn;

public abstract class Option<A> implements Iterable<A> {
  private static final class Some<A> extends Option<A> {
    final A value;

    private Some (A value) {
      this.value = value;
    }

    @Override public A get () {
      return value;
    }

    @Override public <B> Option<B> fmap (Fn<A, B> f) {
      return some(f.apply(value));
    }

    @Override public boolean equals (Object obj) {
      if (obj == null)
        return false;
      if (!(obj instanceof Some))
        return false;
      @SuppressWarnings("unchecked")
      final Some<A> other = (Some<A>) obj;
      if (this.value != other.value && (this.value == null || !this.value.equals(other.value)))
        return false;
      return true;
    }

    @Override public int hashCode () {
      int hash = 5;
      hash = 37 * hash + (this.value != null ? this.value.hashCode() : 0);
      return hash;
    }

    @Override public String toString () {
      return "Some{" + value + '}';
    }
  }

  private static final class None<A> extends Option<A> {
    @SuppressWarnings("rawtypes")
    private static final None<?> instance = new None();

    private None () {
    }

    @Override public A get () {
      throw new NoSuchElementException("'None' does not contain any value");
    }

    @Override public <B> Option<B> fmap (Fn<A, B> f) {
      return none();
    }

    @Override public boolean equals (Object obj) {
      return obj instanceof None;
    }

    @Override public int hashCode () {
      return 3;
    }

    @Override public String toString () {
      return "None{}";
    }
  }

  public abstract <B> Option<B> fmap (Fn<A, B> f);

  /**
   * Returns the option's value. If this option does not have a value an exception is raised.
   *
   * @return The options value.
   * @throws java.util.NoSuchElementException if this option does not have a value.
   */
  public abstract A get ();

  /**
   * Returns the option's value if the option has an value. Otherwise the given defaultValue gets returned.
   *
   * @param defaultValue the value returned in case this option being empty.
   * @return the option's value or the given defaultValue.
   */
  public A getOrElse (final A defaultValue) {
    if (isSome())
      return get();
    else
      return defaultValue;
  }

  /**
   * Return the option's value if it has some or {@code null} otherwise.
   *
   * @return the option's value or {@code null}
   */
  public A getOrNull () {
    return getOrElse(null);
  }

  /**
   * @return true if this options contains a value
   */
  public boolean isSome () {
    return this instanceof Some;
  }

  /**
   * @return true if this options does not contain any value
   */
  public boolean isNone () {
    return this instanceof None;
  }

  /**
   * @return an empty option.
   */
  @SuppressWarnings("unchecked")
  public static <T> Option<T> none () {
    return (Option<T>) None.instance;
  }

  /**
   * Build an option containing the given value. Note that the value must not be {@code null}. If you want to construct
   * an option from a value that might be null use {@link #fromNullable(Object)}.
   *
   * @param value the value of the option.
   * @param <T> the type of the value.
   * @return an option containing the given value
   */
  public static <T> Option<T> some (T value) {
    assert value != null : "Tried to instantiate Some with null.";
    return new Some<T>(value);
  }

  /**
   * Build an option that might be some or none depending if the value is {@code null} or not.
   * @param value the value of the option.
   * @return
   */
  public static <T> Option<T> fromNullable (T value) {
    if (value == null)
      return none();
    else
      return some(value);
  }

  @Override public Iterator<A> iterator () {
    return new OptionIterator<A>(this);
  }

  private static class OptionIterator<A> implements Iterator<A> {
    private Option<A> option;

    private OptionIterator (Option<A> option) {
      this.option = option;
    }

    @Override public boolean hasNext () {
      return option.isSome();
    }

    @Override public A next () {
      A value = option.get();
      option = none();
      return value;
    }

    @Override public void remove () {
      throw new UnsupportedOperationException("Remove not allowed for Option values");
    }
  }
}
