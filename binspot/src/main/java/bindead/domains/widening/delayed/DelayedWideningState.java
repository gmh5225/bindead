package bindead.domains.widening.delayed;

import javalx.persistentcollections.AVLSet;
import bindead.domainnetwork.interfaces.FunctorState;
import bindead.domainnetwork.interfaces.ProgramPoint;

import com.jamesmurty.utils.XMLBuilder;

/**
 * The state for the constant assignments domain is a set of program points with constant assignments.
 */
class DelayedWideningState extends FunctorState {
  protected final AVLSet<ProgramPoint> assignments;
  public static final DelayedWideningState EMPTY = new DelayedWideningState(AVLSet.<ProgramPoint>empty());

  public DelayedWideningState (AVLSet<ProgramPoint> assignments) {
    this.assignments = assignments;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    builder = builder.e(DelayedWidening.NAME)
        .e("Entry");
    builder = builder.a("type", "delayedWidening");
    for (final ProgramPoint locations : assignments) {
      builder = builder.e("ProgramPoint")
          .t(locations.toString());
      builder = builder.up();
    }
    builder = builder.up();
    builder = builder.up();

    return builder;
  }

  @Override public String toString () {
    return "#" + assignments.size() + " " + assignments.toString();
  }
}
