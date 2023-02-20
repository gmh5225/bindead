/**
 * An exception that indicates an unexpected timeout.
 *
 */
package satDomain;

/**
 * @author Axel Simon
 *
 */
public class UnexpectedComplexity extends Error {
  private static final long serialVersionUID = 4927803712142544369L;

  public UnexpectedComplexity (String msg) {
    super(msg);
  }
}
