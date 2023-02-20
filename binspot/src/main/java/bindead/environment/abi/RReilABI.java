package bindead.environment.abi;

import java.util.List;

import javalx.exceptions.UnimplementedException;
import javalx.numeric.Range;
import rreil.lang.RReilAddr;
import rreil.lang.Rhs.Rvar;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.platform.Platform;

/**
 * Dummy ABI class for RREIL assembler code.
 *
 * @author Bogdan Mihaila
 */
public class RReilABI extends ABI {
  public RReilABI (Platform platform) {
    super(platform);
  }

  @Override public <D extends RootDomain<D>> D writeAnalysisStartStackCanary (RReilAddr startAddress, long stackCanary,
      D domainState) {
    return domainState; // should do nothing on RREIL code
  }

  @Override public RReilAddr getReturnAddress (RootDomain<?> domainState) {
    throw new UnimplementedException();
  }

  @Override public Range getFunctionParameterValue (int parameterNumber, RootDomain<?> domainState) {
    throw new UnimplementedException();
  }

  @Override public <D extends RootDomain<D>> D assignFunctionParameterToRegister (int parameterNumber, String reg,
      D domainState) {
    throw new UnimplementedException();
  }

  @Override public <D extends RootDomain<D>> D setRegisterAsReturnValue (String reg, D domainState) {
    throw new UnimplementedException();
  }

  @Override public Rvar getReturnParameterAllocation () {
    throw new UnimplementedException();
  }

  @Override public List<String> getSyscallInputRegisterNames () {
    throw new UnimplementedException();
  }

  @Override public String getSyscallNrRegisterName () {
    throw new UnimplementedException();
  }

  @Override public String getSyscallReturnRegisterName () {
    throw new UnimplementedException();
  }
}
