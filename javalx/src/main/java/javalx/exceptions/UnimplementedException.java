package javalx.exceptions;

/**
 * Use in methods/stub code that have not yet been implemented.
 *
 * @author Bogdan Mihaila
 */
public class UnimplementedException extends RuntimeException {

  private static final long serialVersionUID = -650686096465049141L;

  public UnimplementedException () {
  }

  public UnimplementedException (String message) {
    super(message);
  }

}
