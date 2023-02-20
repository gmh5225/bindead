package bindead.domainnetwork.interfaces;

import javalx.data.Option;
import rreil.lang.RReilAddr;

/**
 * Represents a point in the program. A program point is mostly associated with an address in the program but may depend
 * on other state/properties depending on the analysis. Hence the main purpose of a program point is to uniquely identify
 * points in the program where the uniqueness is defined by the analysis.
 */
public interface ProgramPoint extends Comparable<ProgramPoint> {

  public static Option<ProgramPoint> nowhere = Option.<ProgramPoint>none();

  public RReilAddr getAddress ();

  public ProgramPoint withAddress (RReilAddr newAddress);

  @Override public int compareTo (ProgramPoint other);

  @Override public boolean equals (Object obj);

  @Override public int hashCode ();

}
