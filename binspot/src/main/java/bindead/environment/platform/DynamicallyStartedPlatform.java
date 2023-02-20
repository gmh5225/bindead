package bindead.environment.platform;

import java.util.Map;
import java.util.Map.Entry;

import javalx.numeric.BigInt;
import rreil.lang.MemVar;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import rreil.lang.lowlevel.LowLevelRReil;
import rreil.lang.lowlevel.LowLevelRReilFactory;
import rreil.lang.lowlevel.LowLevelRReilOpnd;
import bindead.analyses.Bootstrap;
import bindead.domainnetwork.interfaces.ContentCtx;
import bindead.domainnetwork.interfaces.RegionCtx;
import bindead.domainnetwork.interfaces.RootDomain;
import binparse.Segment;
import binparse.trace.TraceBinary;
import binparse.trace.TraceDump;

public class DynamicallyStartedPlatform extends Platform {
  private final TraceDump trace;
  private final Platform platform;
  private MemVar stackRegionId;

  public DynamicallyStartedPlatform (TraceBinary binary, Platform platform) {
    super(platform.getDisassembler());
    this.trace = binary.getTraceDump();
    this.platform = platform;
  }

  @Override public Bootstrap forwardAnalysisBootstrap () {
    return new Bootstrap() {
      @Override public <D extends RootDomain<D>> D bootstrap (D state, long initialPC) {
        Segment stack = trace.getStack();
        state = introSegment(stack, state);
        Map<String, BigInt> registerValues = trace.getRegisters();
        for (Entry<String, BigInt> register : registerValues.entrySet()) {
          String registerName = register.getKey();
          if (registerName.equals(platform.getStackPointer())) {
            stackRegionId = MemVar.getVarOrFresh(stack.getName().get());
            BigInt stackOffset = register.getValue().sub(stack.getAddress());
            state = introRegisterWithPointerValue(registerName, stackRegionId, stackOffset, state);
          } else if (!registerName.equals("PC")) { // ignore the artificial program counter value in the registers map
            state = introRegisterWithValue(registerName, register.getValue(), state);
          }
        }
        state = introSegment(trace.getHeap(), state);
        return state;
      }
    };
  }

  private <D extends RootDomain<D>> D introSegment (Segment segment, D state) {
    if (segment == null)
      return state;
    BigInt address = BigInt.of(segment.getAddress());
    long size = segment.getSize();
    ContentCtx segmentCtx = new ContentCtx(segment.getNameOrAddress(), address, size, segment.getPermissions(),
      segment.getData(), segment.getEndianness());
    state = state.introduceRegion(MemVar.fresh(segmentCtx.getName()), new RegionCtx(segmentCtx));
    state = state.eval(String.format("prim fixAtConstantAddress(%s.%d)", segmentCtx.getName(), platform.defaultArchitectureSize()));
    return state;
  }

  @Override public String getStackPointer () {
    return platform.getStackPointer();
  }

  @Override public String getInstructionPointer () {
    return platform.getInstructionPointer();
  }

  /**
   * Only useful after bootstrapping as the stack region is only instantiated at the analysis bootstrap.
   */
  @Override public MemVar getStackRegion () {
    return stackRegionId;
  }

  private <D extends RootDomain<D>> D assignValueToRegister (String registerName, BigInt value, D domain) {
    LowLevelRReilOpnd register = platform.getDisassembler().translateIdentifier(registerName);
    LowLevelRReilFactory rreil = LowLevelRReilFactory.getInstance();
    LowLevelRReil stmt = rreil.MOV(RReilAddr.ZERO, register, rreil.immediate(register.size(), value.getValue()));
    domain = domain.eval((RReil.Assign) stmt.toRReil());
    return domain;
  }

  private <D extends RootDomain<D>> D assignPointerToRegister (String registerName, MemVar pointsToRegionId,
      BigInt offset, D domain) {
    LowLevelRReilOpnd register = platform.getDisassembler().translateIdentifier(registerName);
    // FIXME: replace with current API
    //domain = domain.assignSymbolicAddressOf(((Rvar) register.toRReil()).asLhs(), pointsToRegionId);
    LowLevelRReilFactory rreil = LowLevelRReilFactory.getInstance();
    LowLevelRReil stmt = rreil.ADD(RReilAddr.ZERO, register, register, rreil.immediate(register.size(), offset.getValue()));
    domain = domain.eval((RReil.Assign) stmt.toRReil());
    return domain;
  }

  private <D extends RootDomain<D>> D introRegisterWithValue (String registerName, BigInt value, D domain) {
    MemVar registerRegionId = MemVar.getVarOrFresh(registerName);
    domain = domain.introduceRegion(registerRegionId, RegionCtx.EMPTYSTICKY);
    return assignValueToRegister(registerName, value, domain);
  }

  private <D extends RootDomain<D>> D introRegisterWithPointerValue (String registerName, MemVar pointsToRegionId,
      BigInt offset, D domain) {
    MemVar registerRegionId = MemVar.getVarOrFresh(registerName);
    domain = domain.introduceRegion(registerRegionId, RegionCtx.EMPTYSTICKY);
    return assignPointerToRegister(registerName, pointsToRegionId, offset, domain);
  }


}
