package rreil.tester.gdb;

public class Tuple<T, U> {
  public T x;
  public U y;
  
  public Tuple() {
  }
  
  public Tuple(T x, U y) {
    this.x = x;
    this.y = y;
  }
}
