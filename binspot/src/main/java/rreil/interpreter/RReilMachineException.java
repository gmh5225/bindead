package rreil.interpreter;

/**
 *
 * @author mb0
 */
public class RReilMachineException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  RReilMachineException (final String message) {
    super(message);
  }
}
