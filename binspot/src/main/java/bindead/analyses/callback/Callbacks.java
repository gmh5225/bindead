package bindead.analyses.callback;

import java.util.HashMap;
import java.util.Map;

import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import bindead.analyses.algorithms.data.Flows;
import bindead.domainnetwork.interfaces.ProgramPoint;
import bindead.domainnetwork.interfaces.RootDomain;
import bindead.environment.AnalysisEnvironment;

/**
 * A collection of callbacks that are triggered when the analysis reaches the execution of certain addresses.
 * Here "reaches" means that the control flow uses a <u>jump</u> or <u>call</u> to reach the address and does not
 * merely fall through from a previous instruction. In the later case the callback won't be executed.
 *
 * @author Bogdan Mihaila
 */
public class Callbacks {
  private final Map<RReilAddr, CallbackHandler> handlers = new HashMap<RReilAddr, CallbackHandler>();

  /**
   * Register a callback to be executed each time the analysis jumps to the given address.
   *
   * @param address The address at which the callback should be executed
   * @param handler The callback to execute
   */
  public void addHandler (RReilAddr address, CallbackHandler handler) {
    handlers.put(address, handler);
  }

  /**
   * Execute a callback for the current address if there exists one.
   *
   * @return {@code true} if a callback was registered and executed for this address and {@code false} otherwise.
   */
  public <D extends RootDomain<D>> boolean tryCallback (RReil insn, D domainState, ProgramPoint point,
      AnalysisEnvironment env, Flows<D> flow) {
    CallbackHandler handler = handlers.get(point.getAddress());
    if (handler == null) {
      return false;
    } else {
      handler.run(insn, domainState, point, env, flow);
      return true;
    }
  }

}