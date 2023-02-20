package javalx.data;

public final class Unit {
  private static final Unit UNIT = new Unit();

  public static Unit unit () {
    return UNIT;
  }
}
