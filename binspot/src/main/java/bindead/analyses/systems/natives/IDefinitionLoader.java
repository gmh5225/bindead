package bindead.analyses.systems.natives;

import javalx.data.Option;

public interface IDefinitionLoader {
  public boolean hasEntry(DefinitionId id);
  public Option<RawDefinition> getEntry(DefinitionId id);
}
