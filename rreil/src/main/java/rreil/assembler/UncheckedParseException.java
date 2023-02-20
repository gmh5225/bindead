package rreil.assembler;

import javalx.exceptions.UncheckedExceptionWrapper;
import rreil.assembler.parser.ParseException;

/**
 * Wraps a checked {@link ParseException} as unchecked (runtime) exception.
 */
public class UncheckedParseException extends UncheckedExceptionWrapper {
  private static final long serialVersionUID = 3993305570152666527L;

  public UncheckedParseException (ParseException e) {
    super(e);
  }

}
