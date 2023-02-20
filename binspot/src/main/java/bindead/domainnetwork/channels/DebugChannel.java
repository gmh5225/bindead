package bindead.domainnetwork.channels;

import rreil.lang.Rhs.Rvar;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.data.NumVar;
import bindead.domains.pointsto.PointsToSet;

/**
 * Class representing debugging information that can be very specific to a domain.
 * The interface is meant to exist only temporarily as needed during the development
 * and should not be relied on for querying the domain stack.
 *
 * @author Bogdan Mihaila
 */
public class DebugChannel {
  private final DebugChannel childChannel;
  private final static DebugChannel emptyChannel = new EmptyDebugChannel();

  /**
   * Build an empty debug channel returning {@code null} in every method.
   */
  public DebugChannel () {
    this(emptyChannel);
  }

  /**
   * When instantiating a new debug channel pass the child channel such that methods not overridden
   * in your channel will be answered by children channels.
   */
  public DebugChannel (DebugChannel child) {
    this.childChannel = child;
  }

  /**
   * Query the points-to-set of a variable.
   *
   * @param variable A variable to know the points-to-set of.
   * @return The points to set for the variable.
   */
  public PointsToSet queryPointsToSet (NumVar variable) {
    return childChannel.queryPointsToSet(variable);
  }

  /**
   * Query the field linearization for the given variable.
   *
   * @param value The rhs value (a variable or literal)
   * @return The resolved field or {@code null} if the field cannot be resolved.
   */
  public Rlin resolve (Rvar value) {
    return childChannel.resolve(value);
  }

  /**
   * An empty implementation of a debug channel. Used as the end of the recursion for the calls to the debug children.
   */
  private static class EmptyDebugChannel extends DebugChannel {

    public EmptyDebugChannel () {
      super(null);
    }

    @Override public PointsToSet queryPointsToSet (NumVar variable) {
      return null;
    }

    @Override public Rlin resolve (Rvar value) {
      return null;
    }

  }

}
