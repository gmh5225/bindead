package rreil.lang;

/**
 * Instructions implementing this interface can be printed
 * in a suitable way for the RREIL assembler parser to be able to parse
 * them and rebuild the instruction.
 *
 * @author Bogdan Mihaila
 */
public interface AssemblerParseable {

  /**
   * Return a string representation of the current instruction object
   * that can be parsed by the RREIL assembler.
   */
  public String toAssemblerString ();

}
