package bindead.debug;

import java.util.EnumSet;
import java.util.List;

import javalx.data.products.P2;
import javalx.fn.Predicate;
import javalx.mutablecollections.CollectionHelpers;
import rreil.disassembler.Instruction;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;
import bindead.analyses.BinaryCodeCache;
import bindead.analyses.RReilCodeCache;
import bindead.analyses.algorithms.AnalysisProperties;
import bindead.analyses.algorithms.data.CallString;
import bindead.analyses.algorithms.data.Flows.FlowType;
import bindead.analyses.algorithms.data.Flows.Successor;
import bindead.analyses.algorithms.data.ProgramCtx;
import bindead.analyses.algorithms.data.StateSpace;
import bindead.analyses.warnings.WarningsMap;
import bindead.domainnetwork.channels.WarningMessage;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.ProgramPoint;
import binparse.BinaryFileFormat;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Helper to print various debug messages during an analysis.
 *
 * @author Bogdan Mihaila
 */
public class AnalysisDebugger {
  private final Multiset<ProgramPoint> iterationsCounter = HashMultiset.create();
  private final boolean DEBUGNATIVECODE = AnalysisProperties.INSTANCE.debugNativeCode.isTrue();
  private final boolean DEBUGRREILCODE = AnalysisProperties.INSTANCE.debugRReilCode.isTrue();
  private final boolean DEBUGWARNINGS = AnalysisProperties.INSTANCE.debugWarnings.isTrue();
  private final boolean DEBUGEVAL = AnalysisProperties.INSTANCE.debugAssignments.isTrue();
  private final boolean DEBUGSUMMARY = AnalysisProperties.INSTANCE.debugSummary.isTrue();

  private final BinaryCodeCache binaryCode;
  private final RReilCodeCache rreilCode;
  private final boolean hasNativeCode;

  public AnalysisDebugger (BinaryCodeCache binaryCode, RReilCodeCache rreilCode) {
    this.binaryCode = binaryCode;
    this.rreilCode = rreilCode;
    this.hasNativeCode = !binaryCode.getBinary().getFileFormat().equals(BinaryFileFormat.RREIL);
  }

  public void printSeparator () {
    if (DEBUGEVAL)
      System.out.println(StringHelpers.repeatString("¯", 100));
  }

  public void printInstruction (ProgramCtx programPoint) {
    iterationsCounter.add(programPoint);
    if (DEBUGEVAL) {
      CallString callString = programPoint.getCallString();
      RReilAddr address = programPoint.getAddress();
      // print native instruction if at the right address
      if (hasNativeCode && address.offset() == 0) {
        Instruction nativeInstruction = binaryCode.getInstruction(address.base());
        String parentFunctionName = binaryCode.getEnclosingFunction(nativeInstruction);
        String instructionString = parentFunctionName + ":   " + binaryCode.toRichInstructionString(nativeInstruction);
        StringBuilder builder = new StringBuilder();
        builder.append(StringHelpers.repeatString("_", instructionString.length()));
        builder.append("\n");
        builder.append(instructionString);
        builder.append("\n");
        builder.append(StringHelpers.repeatString("‾", instructionString.length()));
        System.out.println(builder.toString());
      }
      // print RREIL instruction
      StringBuilder builder = new StringBuilder();
      RReil instruction = rreilCode.getInstruction(address);
      String label = rreilCode.getLabel(instruction.getRReilAddress());
      if (!hasNativeCode && !label.isEmpty()) { // print the label only if no native code as that prints them already
        builder.append(StringHelpers.repeatString("_", label.length()));
        builder.append("\n");
        builder.append(label + ":");
        builder.append("\n");
        builder.append(StringHelpers.repeatString("‾", label.length()));
        builder.append("\n");
      }
      String instructionString = rreilCode.toRichInstructionString(instruction);
      builder.append(String.format("#%-4d", iterationsCounter.count(programPoint)));
      builder.append(String.format("%s %s", callString.pretty(5), address));
      builder.append(String.format(" | %-40s", instructionString));
      System.out.print(builder.toString());
    }
  }

  public void printSuccessors (List<P2<Successor<?>, Boolean>> successors) {
    if (DEBUGEVAL) {
      StringBuilder builder = new StringBuilder();
      if (successors.isEmpty()) {
        builder.append(String.format("%-10s", "NO SUCCESSORS"));
      } else {
        List<P2<Successor<?>, Boolean>> inWorkqueue =
          CollectionHelpers.filter(successors, new Predicate<P2<Successor<?>, Boolean>>() {
            @Override public Boolean apply (P2<Successor<?>, Boolean> tuple) {
              return tuple._2();
            }
          });
        List<P2<Successor<?>, Boolean>> notInWorkqueue =
          CollectionHelpers.filter(successors, new Predicate<P2<Successor<?>, Boolean>>() {
            @Override public Boolean apply (P2<Successor<?>, Boolean> tuple) {
              return !tuple._2();
            }
          });
        if (!inWorkqueue.isEmpty()) // the updated successors set
          builder.append(String.format("%-10s", toRichSuccessorsString(CollectionHelpers.split(inWorkqueue)._1())));
        if (!notInWorkqueue.isEmpty()) // the stable successors set
          builder.append(String.format("   STABLE: %-25s",
              toRichSuccessorsString(CollectionHelpers.split(notInWorkqueue)._1())));
      }
      System.out.println(builder.toString());
    }
  }

  /**
   * Add target labels for branches if available.
   */
  private String toRichSuccessorsString (List<Successor<?>> successors) {
    StringBuilder builder = new StringBuilder();
    EnumSet<FlowType> branchTypes = EnumSet.of(FlowType.Call, FlowType.Return, FlowType.Jump, FlowType.Next);
    for (Successor<?> successor : successors) {
      builder.append("  » ");
      builder.append(successor);
      if (branchTypes.contains(successor.getType())) {
        RReilAddr target = successor.getAddress();
        String targetLabel = rreilCode.getLabel(target);
        if (!targetLabel.isEmpty())
          builder.append(" <" + targetLabel + ">");
      }
    }
    return builder.toString();
  }

  public void printWarnings (ProgramCtx programPoint, WarningsMap warningsMap) {
    if (DEBUGWARNINGS) {
      WarningsContainer warnings = warningsMap.get(programPoint);
      if (warnings.isEmpty())
        return;
      StringBuilder builder = new StringBuilder();
      for (WarningMessage message : warnings) {
        builder.append(message.detailedMessage());
        builder.append("\n");
      }
      System.out.println(StringHelpers.indentMultiline("  Warning: ", builder.toString()));
    }
  }

  public void printSummary (StateSpace<?> states, WarningsMap warnings) {
    printCode();
    if (DEBUGSUMMARY) {
      int totalSteps = 0;
      int maxIterations = 0;
      for (ProgramPoint programPoint : iterationsCounter.elementSet()) {
        int iterationsForProgramPoint = iterationsCounter.count(programPoint);
        totalSteps = totalSteps + iterationsForProgramPoint;
        maxIterations = Math.max(maxIterations, iterationsForProgramPoint);
      }
      Multiset<ProgramPoint> wideningPoints = states.getWideningPoints();
      StringBuilder wideningPointsListBuilder = new StringBuilder();
      wideningPointsListBuilder.append("{");
      for (ProgramPoint programPoint : wideningPoints.elementSet()) {
        int timesWidened = wideningPoints.count(programPoint);
        wideningPointsListBuilder.append(timesWidened + "*");
        wideningPointsListBuilder.append(programPoint);
        wideningPointsListBuilder.append(", ");
      }
      if (!wideningPoints.isEmpty())
        wideningPointsListBuilder.setLength(wideningPointsListBuilder.length() - 2); // remove last comma
      wideningPointsListBuilder.append("}");
      System.out.println();
      if (hasNativeCode) {
        String architecture = binaryCode.getBinary().getArchitectureName();
        System.out.println("Analyzed " + architecture + " code: " + binaryCode.instructionsCount() + " instructions");
      }
      System.out.println("Analyzed RREIL code: " + rreilCode.instructionsCount() + " instructions");
      if (rreilCode.instructionsCount() != 0) {
        System.out.println("Analysis steps: " + totalSteps + "  ≈"
            + (totalSteps / rreilCode.instructionsCount() + " iterations/instruction"));
      }
      System.out.println("Max iterations to fixpoint: " + maxIterations);
      System.out.println("Widening points: " + wideningPoints.elementSet().size() + " @ " + wideningPointsListBuilder);
      System.out.println("Warnings: " + warnings.totalNumberOfWarnings());
      System.out.println();
    }
  }

  public void printCode () {
    if (DEBUGRREILCODE) {
      System.out.println();
      System.out.println("Disassembled RREIL code (" + rreilCode.instructionsCount() + " instructions):");
      if (hasNativeCode)
        System.out.println(rreilCode.toDisassemblyString());
      else
        System.out.println(rreilCode);
    }
    if (DEBUGNATIVECODE && hasNativeCode) {
      System.out.println();
      System.out.println(StringHelpers.repeatString("¯", 100));
      String architecture = binaryCode.getBinary().getArchitectureName();
      System.out.println("Disassembled native (" + architecture + ") code (" + binaryCode.instructionsCount()
        + " instructions):");
      System.out.println(binaryCode.toDisassemblyString());
      System.out.println(StringHelpers.repeatString("¯", 100));
    }
  }

}
