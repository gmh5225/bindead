package bindead.data.properties;



abstract class Property<A> {
  private final String key;
  private final String defaultValue;

  Property (String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
    System.setProperty(this.key, defaultValue);
  }

  protected A getValue () throws PropertyException {
    String value = System.getProperty(key, defaultValue);
    return fromString(value);
  }

  public void setValue (A value) {
    System.setProperty(key, asString(value));
  }

  public abstract A fromString (String value) throws PropertyException;

  public abstract String asString (A value);
}