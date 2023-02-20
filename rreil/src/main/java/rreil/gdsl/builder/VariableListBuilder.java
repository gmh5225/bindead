package rreil.gdsl.builder;

import gdsl.rreil.ILimitedVariable;
import gdsl.rreil.IRReilCollection;

import java.util.ArrayList;
import java.util.List;

import rreil.gdsl.BuildingStateManager;
import rreil.lang.Lhs;
import rreil.lang.Rhs.Rval;
import rreil.lang.Rhs.Rvar;

public class VariableListBuilder extends Builder<List<Rvar>> implements
    IRReilCollection<ILimitedVariable> {
  List<VariableBuilder> variables = new ArrayList<VariableBuilder>();

  public VariableListBuilder(BuildingStateManager manager) {
    super(manager);
  }

  @Override
  public BuildResult<List<Rvar>> build() {
    List<Rvar> variables = new ArrayList<Rvar>(this.variables.size());
    for (VariableBuilder varBuilder : this.variables)
      variables.add(varBuilder.build().getResult());
    return result(variables);
  }

  @Override
  public void add(ILimitedVariable variable) {
    variables.add((VariableBuilder)variable);
  }

  @Override
  public ILimitedVariable get(int i) {
    return variables.get(i);
  }

  @Override
  public int size() {
    return variables.size();
  }
  
  public static List<Rval> asRvalList(List<Rvar> buildResult) {
    List<Rval> variables = new ArrayList<Rval>(buildResult.size());
    variables.addAll(buildResult);
    return variables;
  }

  public static List<Lhs> asLhsList(List<Rvar> buildResult) {
    List<Lhs> variables = new ArrayList<Lhs>(buildResult.size());
    for (Rvar rvar : buildResult)
      variables.add(rvar.asLhs());
    return variables;
  }
}
