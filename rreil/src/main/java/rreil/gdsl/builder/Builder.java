package rreil.gdsl.builder;

import rreil.gdsl.BuildingStateManager;
import rreil.gdsl.builder.BuildResult;

import java.util.SortedMap;
import java.util.TreeMap;

import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;
import rreil.lang.util.RReilFactory;
import rreil.lang.util.RhsFactory;

/**
 * This is a generic builder class; its subclasses are used to construct
 * object for Gdsl RReil AST nodes and combine them into a RReil program.
 * 
 * @author Julian Kranz
 * 
 * @param <T> the type of the object the builder builds
 */
public abstract class Builder<T> {
  protected BuildingStateManager manager;

  /**
   * Construct the builder
   * 
   * @param manager the address manager used for assigning offsets to the
   *          newly built RReil objects
   */
  public Builder (BuildingStateManager manager) {
    super();
    this.manager = manager;
  }

  /**
   * Build an object of type T or one of its subtypes
   * 
   * @return a {@link BuildResult} object containg the result of the
   *         build process
   */
  public abstract BuildResult<? extends T> build ();

  /**
   * Set the size of the object to be built.
   * 
   * @param size the new size
   * @return this
   */
  public Builder<T> size (int size) {
    throw new RuntimeException("No size field");
  }

  /**
   * Get the size of the object to be built.
   * 
   * @return the size
   */
  public int getSize () {
    throw new RuntimeException("No size field");
  }

  protected static <T> BuildResult<T> result (T result, SortedMap<RReilAddr, RReil> stmts) {
    return new BuildResult<T>(result, stmts);
  }

  protected static <T> BuildResult<T> result (T result, RReil stmt) {
    SortedMap<RReilAddr, RReil> map = new TreeMap<RReilAddr, RReil>();
    map.put(stmt.getRReilAddress(), stmt);
    return new BuildResult<T>(result, map);
  }

  protected static <T> BuildResult<T> result (T result) {
    return new BuildResult<T>(result);
  }

  /**
   * Build an {@link Rval} from an {@link Rhs} object; this is done
   * by assigning the right hand side value to a newly created temporary
   * variable.
   * 
   * @param from the right hand side value
   * @return the resulting {@link Rval} object
   */
  protected BuildResult<? extends Rval> buildRval (Rhs from) {
    MemVar var = manager.nextTemporary();

    Rvar temporary = RhsFactory.getInstance().variable(getSize(), 0, var);
    RReil assign = RReilFactory.instance.assign(manager.nextAddress(), temporary.asLhs(),
        from);

    return result(temporary, assign);
  }
}
