package rreil.gdsl.builder;

public class Component<T> {
  private T t;
  boolean isSet;

  public Component () {
    isSet = false;
  }

  public void set (T t) {
    this.t = t;
    isSet = true;
  }

  public T get () {
    if (isSet)
      return t;
    throw new RuntimeException("Element not set");
  }
}
