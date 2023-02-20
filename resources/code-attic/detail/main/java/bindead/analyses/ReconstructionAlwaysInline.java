package bindead.analyses;

import java.io.IOException;

import javalx.data.BigInt;
import bindead.analysis.util.CallString;
import bindead.domainnetwork.interfaces.RootDomain;
import binparse.Binary;

/**
 * Performs the reconstruction of the CFA for a binary. It uses inlining to perform an interprocedural fixpoint analysis.
 * The inlining is done on each iteration step of the fixpoint, even if the same calling context is analyzed. This can be
 * useful for debugging purposes.
 */
public class ReconstructionAlwaysInline<D extends RootDomain<D>> extends ReconstructionCallString<D> {

  private ReconstructionAlwaysInline (Binary binary) throws IOException {
    super(binary, new InfiniteCallString());
  }

  public static <D extends RootDomain<D>> ReconstructionCallString<D> run (Binary binary, BigInt startAddress) throws IOException {
    ReconstructionAlwaysInline<D> reconstruction = new ReconstructionAlwaysInline<D>(binary);
    reconstruction.run(startAddress);
    return reconstruction;
  }

  /**
   * A special version of the Call-String where each instance of the class is unique, thus each iteration step evaluating
   * a call will generate a new analysis -- inlining the called procedure.
   */
  private static class InfiniteCallString extends CallString {
    public InfiniteCallString () {
      super(Integer.MAX_VALUE);
    }

    private InfiniteCallString (InfiniteCallString other) {
      super(other);
    }

    @Override protected InfiniteCallString clone () {
      return new InfiniteCallString(this);
    }

    @Override public int hashCode () {
      return System.identityHashCode(this);
    }

    @Override public boolean equals (Object obj) {
      return this == obj;
    }
  }

}
