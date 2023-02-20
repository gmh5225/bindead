package rreil.gdsl.builder;

import java.util.SortedMap;
import java.util.TreeMap;

import rreil.lang.RReil;
import rreil.lang.RReilAddr;

/**
 * This class represents the result of a build process run by an
 * object of a subclass of the {@link Builder} class.
 * 
 * Such a build result consists of two elements: The RReil AST node
 * that represents the result of the build process and a list of
 * additional RReil statements used to calculate
 * this result. This is because the RReil language as defined in the
 * Gdsl framework is more expressive than the language defined by the
 * original RReil paper.; Therefor, a single Gdsl RReil statement
 * sometimes needs to be translated to a number of traditional RReil
 * statements.
 * 
 * @author Julian Kranz
 *
 * @param <T> the type of the resulting RReil AST node
 */
public class BuildResult<T> {
  private SortedMap<RReilAddr, RReil> statements;
  private T result;
  
  public static SortedMap<RReilAddr, RReil> emptyStatements() {
    return new TreeMap<RReilAddr, RReil>();
  }

  /**
   * Get the list of additional RReil statements.
   * 
   * @return the list of additional RReil statements
   */
  public SortedMap<RReilAddr, RReil> getStatements () {
    return statements;
  }

  /**
   * Get the object that represents the build result
   * 
   * @return the object that represents the build result
   */
  public T getResult () {
    return result;
  }

  public BuildResult (T result, SortedMap<RReilAddr, RReil> additional_statements) {
    this.statements = additional_statements;
    this.result = result;
  }

  public BuildResult (T result) {
    this.statements = new TreeMap<RReilAddr, RReil>();
    this.result = result;
  }

  /**
   * Combine the additional statements of two build results; the statements
   * of the other build result are appended to the build result of the called
   * object; Neither of the objects are changed.
   * 
   * @param other the other build result
   * @return the combined list of additional statements
   */
  public SortedMap<RReilAddr, RReil> before (BuildResult<?> other) {
    SortedMap<RReilAddr, RReil> result = new TreeMap<RReilAddr, RReil>();
    result.putAll(statements);
    result.putAll(other.statements);
    return result;
  }
}
