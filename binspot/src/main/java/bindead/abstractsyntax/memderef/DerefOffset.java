package bindead.abstractsyntax.memderef;

import javalx.numeric.Range;
import bindead.data.Linear;
import bindead.domainnetwork.channels.QueryChannel;

public abstract class DerefOffset {
  public abstract Range getRange (QueryChannel state);

  public abstract Linear upperBound ();

  public abstract Linear lowerBound ();

  public abstract DerefOffset add (Linear ofs);
}
