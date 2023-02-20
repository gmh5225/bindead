package rreil.gdsl;

import rreil.lang.MemVar;
import rreil.lang.RReilAddr;

/**
 * This class is used by the RReil Gdsl builders to assign address offsets
 * to the newly built RReil statements. It also takes care of managing
 * temporary variables to be used by the RReil Gdsl builders for intermediate
 * results.
 * 
 * @author Julian Kranz
 */
public class BuildingStateManager {
  private RReilAddr currentInstructionAddress;
  private boolean staged = false;
  private int nextTemp = 0;
  
  /**
   * Construct a new BuildingStateManager object
   * 
   * @param start the address to start from
   */
  public BuildingStateManager (RReilAddr start) {
    this.currentInstructionAddress = start;
  }
  
  /**
   * Construct a new BuildingStateManager object and start at address ZERO
   */
  public BuildingStateManager () {
    currentInstructionAddress = RReilAddr.ZERO;
  }
  
  /**
   * Get the next offset (the offset may or may not be staged)
   * 
   * @return the next offset
   */
  public RReilAddr nextAddress() {
    RReilAddr result = currentInstructionAddress;
    currentInstructionAddress = currentInstructionAddress.nextOffset();
    staged = false;
    return result;
  }
  
  /**
   * Stage the next offset; this is useful if the next RReil address offset
   * needs to be referenced before it is actually used.
   * 
   * @return the staged address
   */
  public RReilAddr stageAddress() {
    staged = true;
    return currentInstructionAddress;
  }
  
  /**
   * Check whether the next address offset is staged
   * 
   * @return the result of the check
   */
  public boolean hasStagedAddress() {
    return staged;
  }
  
  /**
   * Get a new temporary variable; all temporary variables created
   * by the same {@link BuildingStateManager} object are unique.
   * 
   * @return the temporary variable
   */
  public MemVar nextTemporary () {
    MemVar var = MemVar.getVarOrFresh("tt" + nextTemp++);
    return var;
  }
}
