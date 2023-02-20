package bindead.analyses;

import javalx.data.Option;
import rreil.lang.RReilAddr;
import bindead.domainnetwork.interfaces.ProgramPoint;

/**
 * A program point that is only an address.
 *
 * @author Bogdan Mihaila
 */
public class ProgramAddress implements ProgramPoint {
  private final RReilAddr address;

  public ProgramAddress (RReilAddr address) {
    this.address = address;
  }

  @Override public RReilAddr getAddress () {
    return address;
  }

  @Override public ProgramPoint withAddress (RReilAddr newAddress) {
    return new ProgramAddress(newAddress);
  }

  @Override public int compareTo (ProgramPoint other) {
    return this.getAddress().compareTo(other.getAddress());
  }

  public static Option<ProgramPoint> none () {
    return ProgramPoint.nowhere;
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((address == null) ? 0 : address.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof ProgramAddress))
      return false;
    ProgramAddress other = (ProgramAddress) obj;
    return this.compareTo(other) == 0;
  }

  @Override public String toString () {
    return address.toShortString();
  }
}
