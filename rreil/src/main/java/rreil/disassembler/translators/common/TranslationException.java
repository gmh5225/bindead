package rreil.disassembler.translators.common;

/**
 *
 * @author mb0
 */
public class TranslationException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final TranslationCtx translationCtx;

  public TranslationException (TranslationCtx ctx) {
    this.translationCtx = ctx;
  }

  public TranslationCtx getTranslationCtx () {
    return translationCtx;
  }

  @Override public String getMessage () {
    return "Unable to translate instruction: " + translationCtx.getCurrentInstruction();
  }
}
