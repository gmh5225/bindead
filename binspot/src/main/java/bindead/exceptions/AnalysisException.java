package bindead.exceptions;

public class AnalysisException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new instance of {@code AnalysisException} without detail message.
   */
  public AnalysisException () {
  }

  /**
   * Constructs an instance of {@code AnalysisException} with the specified detail message.
   *
   * @param msg The detail message.
   */
  public AnalysisException (String msg) {
    super(msg);
  }
}
