package bindead.data.properties;


public class BoolProperty extends Property<Boolean> {
  private static final String FALSE = "false";
  private static final String TRUE = "true";


  public BoolProperty (String key) {
    super(key, FALSE);
  }

  public Boolean isTrue () {
    return getValue();
  }

  @Override public Boolean fromString (String value) throws PropertyException {
    if (value.equals(TRUE))
      return true;
    else if (value.equals(FALSE))
      return false;
    else
      throw new PropertyException(this, value);
  }

  @Override public String asString (Boolean value) {
    return value ? TRUE : FALSE;
  }
}