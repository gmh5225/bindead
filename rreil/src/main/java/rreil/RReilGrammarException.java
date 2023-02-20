package rreil;

/**
 * To be used for translation exceptions between the various intermediate languages of the analyzer.
 *
 * @author Bogdan Mihaila
 */
public class RReilGrammarException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public RReilGrammarException () {
  }

  public RReilGrammarException (String message) {
    super(message);
  }

}
