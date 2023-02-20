package rreil.gdsl;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import rreil.lang.RReil;
import rreil.lang.RReilAddr;

/**
 * This class represents a collection of RReil statements. Such a collection
 * is built by the Gdsl RReil builder.
 * 
 * @author Julian Kranz
 */
public class StatementCollection {
  private SortedMap<RReilAddr, RReil> instructions = new TreeMap<RReilAddr, RReil>();

  public StatementCollection() {
  }

  /**
   * Add a statement to the collection
   * 
   * @param stmt the statement to add
   */
  public void add(RReil stmt) {
    RReil rreil = (RReil) stmt;
    instructions.put(rreil.getRReilAddress(), rreil);
  }

  /**
   * Add a set of statements to the collection
   * 
   * @param elements the set of statements to add
   */
  public void addAll(Map<? extends RReilAddr, ? extends RReil> elements) {
    instructions.putAll(elements);
  }

  /**
   * Get the corresponding {@link SortedMap} containing the RReil statements
   * 
   * @return the {@link SortedMap} object
   */
  public SortedMap<RReilAddr, RReil> getInstructions() {
    return instructions;
  }

}
