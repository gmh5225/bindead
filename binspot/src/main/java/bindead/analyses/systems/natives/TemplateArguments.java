package bindead.analyses.systems.natives;

import java.util.LinkedList;
import java.util.List;

import rreil.assembler.SingleFunctionParser.SizedVariable;

public class TemplateArguments {
  private final List<SizedVariable> inputIds = new LinkedList<SizedVariable>();
  private final List<SizedVariable> outputIds = new LinkedList<SizedVariable>();

  public TemplateArguments () {
    
  }
  
  public TemplateArguments (List<String> inputIds, List<String> outputIds, Integer defaultRegisterSize) {
    for (String inId : inputIds) {
      this.inputIds.add(new SizedVariable(inId, defaultRegisterSize));
    }
    for (String outId : outputIds) {
      this.outputIds.add(new SizedVariable(outId, defaultRegisterSize));
    }
  }
  
  public List<SizedVariable> getInputVars () {
    return inputIds;
  }
  
  public List<SizedVariable> getOutputVars () {
    return outputIds;
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((inputIds == null) ? 0 : inputIds.hashCode());
    result = prime * result + ((outputIds == null) ? 0 : outputIds.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TemplateArguments other = (TemplateArguments) obj;
    if (inputIds == null) {
      if (other.inputIds != null)
        return false;
    } else if (!inputIds.equals(other.inputIds))
      return false;
    if (outputIds == null) {
      if (other.outputIds != null)
        return false;
    } else if (!outputIds.equals(other.outputIds))
      return false;
    return true;
  }
}
