package bindead.data.properties;


class PropertyException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final Property<?> property;
  private final String value;

  public PropertyException (Property<?> property, String value) {
    this.property = property;
    this.value = value;
  }

  @Override public String getMessage () {
    StringBuilder builder = new StringBuilder("Invalid valuation for ");
    return builder.append(property).append(": ").append(value).toString();
  }

  public Property<?> getProperty () {
    return property;
  }

  public String getValue () {
    return value;
  }
}
