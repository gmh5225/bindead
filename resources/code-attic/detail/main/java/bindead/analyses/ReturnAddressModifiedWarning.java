package bindead.analyses;

import bindead.data.Range;
import bindead.domainnetwork.channels.Message;

/**
 * A warning issued by the reconstruction that the return address has been modified.
 */
public class ReturnAddressModifiedWarning extends Message.Info {
  private static final String fmt = "Return address was modified: %s; address saved at procedure entry: %s";
  private final Range actualReturnAddress;
  private final Range savedReturnAddress;

  public ReturnAddressModifiedWarning (Range actualReturnAddress, Range savedReturnAddress) {
    this.actualReturnAddress = actualReturnAddress;
    this.savedReturnAddress = savedReturnAddress;
  }

  @Override public String message () {
    return String.format(fmt, actualReturnAddress, savedReturnAddress);
  }
}
