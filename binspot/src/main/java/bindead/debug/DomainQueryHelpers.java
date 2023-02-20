package bindead.debug;

import java.util.NoSuchElementException;

import javalx.numeric.Range;
import rreil.lang.MemVar;
import rreil.lang.Rhs.Rvar;
import bindead.abstractsyntax.finite.Finite.Rlin;
import bindead.data.Linear;
import bindead.domainnetwork.channels.SetOfEquations;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.domains.pointsto.PointsToSet;
import bindead.environment.platform.Platform;

/**
 * Methods to help when querying a domain stack for its properties.
 * This methods are implemented on a best effort and hack and try ground,
 * thus should be only used for debugging purposes and _not_ in analysis code.
 *
 * @author Bogdan Mihaila
 */
public class DomainQueryHelpers {

  /**
   * Tries to resolve a variable by only having its name. Not overly reliable for non-platform registers,
   * i.e. temporary variables as it has to guess the size for these variables.
   */
  public static <D extends RootDomain<D>> Rvar resolveVariable (String variableName, D domain, Platform platform) {
    Rvar variable = resolvePlatformRegister(variableName, platform);
    if (variable == null)
      variable = resolveTemporaryRegister(variableName, domain, platform.defaultArchitectureSize());
    return variable;
  }

  private static Rvar resolvePlatformRegister (String variableName, Platform platform) {
    Rvar variable = null;
    try {
      variable = platform.getRegisterAsVariable(variableName);
    } catch (IllegalArgumentException e) {
    }
    return variable;
  }

  /**
   * Brute forces different variable sizes to find a known field for a variable.
   */
  private static <D extends RootDomain<D>> Rvar resolveTemporaryRegister (String variableName, D domain,
      int defaultSize) {
    MemVar regionId = MemVar.getVarOrNull(variableName);
    if (regionId == null)
      return null;
    Rvar variable = null;
    int[] possibleRegisterSizes = {defaultSize, 128, 64, 32, 16, 8, 1};
    for (int size : possibleRegisterSizes) {
      variable = new Rvar(size, 0, regionId);
      try {
        if (domain.getDebugChannel().resolve(variable) != null)
          return variable;
      } catch (NoSuchElementException e) {
        return null;
      }
    }
    return variable;
  }

  /**
   * Tries to resolve a variable by only having its name. Not overly reliable for non-platform registers,
   * i.e. temporary variables as it has to guess the size for these variables.
   *
   * @return rangeOf( variableName )
   */
  public static <D extends RootDomain<D>> Range queryRange (String variableName, D domain, Platform platform) {
    Rvar variable = resolveVariable(variableName, domain, platform);
    if (variable == null)
      return null;
    return queryRange(variable, domain);
  }

  /**
   * @return rangeOf( [pointerName + offset]:sizeInBits )
   */
  public static <D extends RootDomain<D>> Range queryRange (String pointerName, int offset, int sizeInBits, D domain,
      Platform platform) {
    Rvar variable = resolveVariable(pointerName, domain, platform);
    if (variable == null)
      return null;
    String tempReg1 = "treg1";
    String addOffset = String.format("add.%d %s, %s, %d", variable.getSize(), tempReg1, pointerName, offset);
    String tempReg2 = "treg2";
    String load = String.format("load.%d.%d %s, %s", sizeInBits, variable.getSize(), tempReg2, tempReg1);
    D resultDomain = domain.eval(addOffset, load);
    Rvar resultVariable = resolveTemporaryRegister(tempReg2, resultDomain, platform.defaultArchitectureSize());
    if (resultVariable == null)
      return null;
    return queryRange(resultVariable, domain);
  }

  public static <D extends RootDomain<D>> Range queryRange (Rvar variable, D domainState) {
    try {
      return domainState.queryRange(variable);
    } catch (Exception | AssertionError e) {
      return null;
    }
  }

  public static <D extends RootDomain<D>> SetOfEquations queryEqualities (Rvar variable, D domainState) {
    Rlin resolvedVariable = null;
    try {
      resolvedVariable = domainState.getDebugChannel().resolve(variable);
    } catch (Exception e) {
    }
    if (resolvedVariable == null)
      return null;
    // assert resolvedVariable.getLinearTerm().isSingleTerm() : "The variable is not a single term.";
    try {
      return domainState.queryEqualities(resolvedVariable.getLinearTerm().getKey());
    } catch (Exception | AssertionError e) {
      return null;
    }
  }

  public static <D extends RootDomain<D>> PointsToSet queryPointsToSet (Rvar variable, D domainState) {
    Rlin resolvedVariable = null;
    try {
      resolvedVariable = domainState.getDebugChannel().resolve(variable);
    } catch (Exception e) {
    }
    if (resolvedVariable == null)
      return null;
    try {
      return domainState.getDebugChannel().queryPointsToSet(resolvedVariable.getLinearTerm().getKey());
    } catch (Exception | AssertionError e) {
      return null;
    }
  }

  /**
   * Produces nicer looking linear equalities.
   */
  public static String formatLinearEqualities (SetOfEquations equalities) {
    StringBuilder builder = new StringBuilder();
    for (Linear linear : equalities) {
      builder.append(linear.toEquationString());
      builder.append(", ");
    }
    if (!equalities.isEmpty())
      builder.setLength(builder.length() - 2);
    return builder.toString();
  }

}
