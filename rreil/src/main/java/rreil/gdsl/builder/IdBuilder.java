package rreil.gdsl.builder;

import gdsl.rreil.id.IId;

import java.util.HashMap;
import java.util.Map;

import rreil.gdsl.BuildingStateManager;
import rreil.lang.MemVar;

public class IdBuilder extends Builder<MemVar> implements IId {
  private final String id;
  private static Map<String, String> renamings = new HashMap<>();

  public IdBuilder (BuildingStateManager manager, String id) {
    super(manager);
    id = id.toLowerCase();
    if (renamings.containsKey(id))
      this.id = renamings.get(id);
    else
      this.id = id;
  }

  @Override public BuildResult<? extends MemVar> build () {
    return result(MemVar.getVarOrFresh(id));
  }

  /**
   * Retrieve the map of renamings for variables.
   */
  public static Map<String, String> getRenamings () {
    return renamings;
  }

  /**
   * Set the map of renamings for variables.
   * When instantiating a variable for a register the builder will look into the map
   * if it should use a different name than the canonical one.
   */
  public static void setRenamings (Map<String, String> renamings) {
    IdBuilder.renamings = renamings;
  }
}
