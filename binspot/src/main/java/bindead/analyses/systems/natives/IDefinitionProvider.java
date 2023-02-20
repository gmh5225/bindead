package bindead.analyses.systems.natives;

import javalx.data.Option;

public interface IDefinitionProvider {
  public boolean hasNativeFunction (DefinitionId id);
  public Option<FunctionDefinition> getNativeFunction (DefinitionId id, TemplateArguments args);
  public IDefinitionProvider cached();
}
