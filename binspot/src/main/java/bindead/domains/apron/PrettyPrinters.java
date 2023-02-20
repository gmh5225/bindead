package bindead.domains.apron;

import java.util.Iterator;
import java.util.Set;

import javalx.numeric.Interval;
import javalx.persistentcollections.BiMap;
import apron.Abstract1;
import apron.ApronException;
import apron.Manager;
import bindead.data.NumVar;
import bindead.debug.StringHelpers;


class PrettyPrinters {

  public static String asIntervals (Abstract1 state, BiMap<NumVar, String> variablesMapping, boolean withConstants) {
    Manager man = state.getCreationManager();
    try {
      if (state.isBottom(man))
        return "<empty>";
      if (state.isTop(man))
        return "<universal>";
      StringBuilder builder = new StringBuilder();
      builder.append('{');
      printIntervals(state, variablesMapping, builder, withConstants);
      builder.append('}');
      return builder.toString();
    } catch (ApronException e) {
      return "<caught exception: " + e.getMessage() + ">";
    }
  }


  private static void printIntervals (Abstract1 state, BiMap<NumVar, String> variablesMapping, StringBuilder builder,
      boolean withConstants) throws ApronException {
    Set<NumVar> sorted = StringHelpers.sortLexically(variablesMapping.keys());
    Iterator<NumVar> iterator = sorted.iterator();
    while (iterator.hasNext()) {
      NumVar variable = iterator.next();
      String varName = variable.toString();
      Interval value =
        Marshaller.fromApronInterval(state.getBound(state.getCreationManager(), variablesMapping.get(variable).get()));
      if (!withConstants && value.isConstant())
        continue;
      builder.append(varName);
      builder.append('=');
      builder.append(value);
      if (iterator.hasNext())
        builder.append(", ");
    }
  }

  public static String compactOctagons (Abstract1 state, BiMap<NumVar, String> variablesMapping) {
    try {
      Manager man0 = state.getCreationManager();
      if (state.isBottom(man0))
        return "<empty>";
      if (state.isTop(man0))
        return "<universal>";
      OctagonsMatrixPrinter octagonsMatrix = new OctagonsMatrixPrinter(state, variablesMapping);
      return octagonsMatrix.printMinimalContraints();
    } catch (ApronException e) {
      return "<caught exception: " + e.getMessage() + ">";
    }
  }

}
