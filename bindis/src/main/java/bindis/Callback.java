package bindis;

/**
 * Callback interface for disassemblers.
 *
 * @author mb0
 * @author Bogdan Mihaila
 */
public interface Callback<T> {

  /**
   * @return Returns <code>true</code> if the disassembly should continue or <code>false</code> otherwise.
   */
  public boolean visit (T obj);
}
