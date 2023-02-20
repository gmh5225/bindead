package bindead.analyses;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import bindead.analyses.warnings.WarningsMap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Provides an API to report the current state of an ongoing analysis.
 *
 * @author Bogdan Mihaila
 */
public class ProgressReporter {
  private final Multiset<RReilAddr> iterationsCounter = HashMultiset.create();
  private final List<InstructionListener> listeners = new ArrayList<>();
  private final WarningsMap warnings;

  public ProgressReporter (WarningsMap warnings) {
    this.warnings = warnings;
  }

  public void evaluatingInstruction (RReil insn) {
    iterationsCounter.add(insn.getRReilAddress());
    for (InstructionListener listener : listeners) {
      listener.evaluatingInstruction(insn);
    }
  }

  public void addInstructionListener (InstructionListener listener) {
    listeners.add(listener);
  }

  public int getIterationCount (RReil insn) {
    return iterationsCounter.count(insn.getRReilAddress());
  }

  public int getIterationCount (RReilAddr address) {
    return iterationsCounter.count(address);
  }

  public WarningsMap getWarnings () {
    return warnings;
  }

  public interface InstructionListener extends EventListener {
    public void evaluatingInstruction (RReil insn);
  }
}
