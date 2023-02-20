package bindead.debug;


public class DomainPrintHelpers {

  public static String printState(String name, Object state) {
    return StringHelpers.indentMultiline(name + ": ", state.toString()) + "\n";
  }
}
