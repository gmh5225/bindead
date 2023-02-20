package bindead.domainnetwork.channels;

import javalx.data.Option;
import javalx.numeric.FiniteRange;
import javalx.numeric.Range;
import rreil.lang.MemVar;
import rreil.lang.Rhs.Lin;
import rreil.lang.Rhs.Rval;
import bindead.data.NumVar;

public interface RootQueryChannel {
  /**
   * Query the range valuation of the given right-hand-side value.
   *
   * @param value The rhs value (a variable or literal).
   * @return The range valuation of the value or {@code null} if it is a variable and cannot be resolved.
   */
  public Range queryRange (Rval value);

  /**
   * Query the range valuation of the given right-hand-side value.
   *
   * @param value The rhs value (an expression).
   * @return The range valuation of the expression or {@code null} if some variable and cannot be resolved.
   */
  public Range queryRange (Lin value);

  /**
   * Query the range valuation of interval-field {@code bits} contained in {@code region}.
   *
   * @param region The region in which to search for the field.
   * @param bits The interval of the bits comprising a field.
   * @return The range valuation of the field or {@code null} if the field cannot be resolved.
   */
  public Range queryRange (MemVar region, FiniteRange bits);

  /**
   * Picks the numeric variable that holds the valuation of a specific interval,
   * ignoring any overlappings. Note that this does _not_ create a field if one cannot
   * be found for the given access.
   */
  public Option<NumVar> resolveVariable (MemVar region, FiniteRange bits);

}
