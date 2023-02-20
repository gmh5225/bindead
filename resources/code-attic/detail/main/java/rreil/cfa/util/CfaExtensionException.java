package rreil.cfa.util;

import rreil.abstractsyntax.RReilAddr;
import rreil.cfa.Cfa;

/**
 * Thrown to indicate that an exception occurred while trying to extend a CFA.
 * @author Bogdan Mihaila
 */
public class CfaExtensionException extends RuntimeException {
  private final Cfa cfa;
  private final RReilAddr extensionAddress;

  public CfaExtensionException (Exception cause, Cfa cfa, RReilAddr extensionAddress) {
    super(cause);
    this.cfa = cfa;
    this.extensionAddress = extensionAddress;
  }

  @Override public String getMessage () {
    return "Extension of CFA@" + getCfa().getEntryAddress() + " at address: " + getExtensionAddress();
  }

  /**
   * @return the address at which the CFA should be extended.
   */
  public RReilAddr getExtensionAddress () {
    return extensionAddress;
  }

  /**
   * @return the CFA that should be extended
   */
  public Cfa getCfa () {
    return cfa;
  }

}
