package rreil.interpreter.avr.chipstate;

//import implementations.avr.AvrImplementation;
//
//import java.io.PrintWriter;
//import java.io.StringWriter;

public class AvrChipStateDelta {
  private final boolean[] changedDataSpace;
  
  public boolean[] getChangedDataSpace() {
    return changedDataSpace;
  }
  
  public AvrChipStateDelta(boolean[] changedDataSpace) {
    this.changedDataSpace = changedDataSpace;
  }
  
//  private final boolean[] changedGeneralPurposeRegisters;
//  
//  private final boolean sregChanged;
//  private final boolean spChanged;
//  private final boolean pcChanged;
//  
//  public boolean[] getGeneralPurposeRegisters() {
//    return changedGeneralPurposeRegisters;
//  }
//  
//  public boolean sregChanged() {
//    return sregChanged;
//  }
//  
//  public boolean getSpChanged() {
//    return spChanged;
//  }
//  
//  public boolean getPcChanged() {
//    return pcChanged;
//  }
//  
//  public AvrChipStateDelta(boolean[] changedGeneralPurposeRegisters, boolean sregChanged, boolean spChanged, boolean pcChanged) {
//    this.changedGeneralPurposeRegisters = changedGeneralPurposeRegisters;
//    this.sregChanged = sregChanged;
//    this.spChanged = spChanged;
//    this.pcChanged = pcChanged;
//  }
//  
//  @Override public String toString() {
//    StringWriter sW = new StringWriter();
//    PrintWriter pW = new PrintWriter(sW);
//    
//    pW.println("AvrChipStateDelta {");
//    for(int i = 0; i < changedGeneralPurposeRegisters.length; i++)
//      pW.printf("\tr%d:\t%s\n", i, changedGeneralPurposeRegisters[i]);
//
//    pW.printf("\tSreg:\t%s\n", sregChanged);
//    pW.printf("\tSp:\t%s\n", spChanged);
//    pW.printf("\tPc:\t%s\n", pcChanged);
//    pW.println("}");
//    
//    return sW.toString();
//  }
}
