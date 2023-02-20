package bindead.domainnetwork.interfaces;

import java.util.List;

import javalx.data.products.P2;
import rreil.lang.MemVar;
import rreil.lang.RReil.Assign;
import rreil.lang.RReil.Branch;
import rreil.lang.RReil.Load;
import rreil.lang.RReil.PrimOp;
import rreil.lang.RReil.Store;
import rreil.lang.RReilAddr;
import rreil.lang.Test;
import bindead.debug.PrettyDomain;
import bindead.domainnetwork.channels.QueryChannel;
import bindead.domainnetwork.channels.RootQueryChannel;
import bindead.exceptions.Unreachable;

/**
 * Domain interface for the top-most domains in the abstract interpretation hierarchy.
 *
 * @param <D> Self type
 */
public interface RootDomain<D extends SemiLattice<D>>
    extends RootQueryChannel, SemiLattice<D>, QueryChannel, PrettyDomain {

  /* == Applicable transfer functions for root-domains. == */

  public abstract D eval (PrimOp prim);

  public abstract D eval (Assign stmt);

  public abstract D eval (Load stmt);

  public abstract D eval (Store stmt);

  public abstract D eval (Test test) throws Unreachable;

  public abstract List<P2<RReilAddr, D>> eval (Branch branch, ProgramPoint current, ProgramPoint next);

  /* == Support operations == */

  /**
   * Introduce a region with given region-identifier {@code region} using context {@code ctx}.
   *
   * @param region The region-identifier.
   * @param ctx The region's context (type).
   * @return The updated root-domain.
   * @throws {@code DomainStateException} if the region-identifier was already known to this.
   */
  public abstract D introduceRegion (MemVar region, RegionCtx ctx);

  /**
   * Convenience method to manipulate the current state by using a RREIL instruction given in RREIL assembler syntax.
   * Note though that only instructions without control flow will be executed, i.e. assignments, loads, stores and
   * primOps.
   *
   * @param instructions A list of non-branching RREIL assembler instructions to be executed on this state
   * @return The resulting state after executing the instructions
   * @see #evalBranch(String)
   */
  public D eval (String... instructions);

  /**
   * Returns this domain with its child domains as an XML representation.
   *
   * @return An XML representation of this domain.
   */
  public abstract String toXml ();
}
