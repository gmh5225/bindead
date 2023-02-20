package bindead.analyses.systems.natives;

import java.util.HashMap;
import java.util.Map;

import javalx.data.Option;

/**
 * Caches {@link FunctionDefinition}s (and its associated {@link TemplateArguments})
 */
public class ProviderCacheDecorator implements IDefinitionProvider {
  private final IDefinitionProvider provider;
  private final Map<DefinitionId, Map<TemplateArguments, Option<FunctionDefinition>>> cache =
    new HashMap<DefinitionId, Map<TemplateArguments, Option<FunctionDefinition>>>();

  public ProviderCacheDecorator (IDefinitionProvider provider) {
    this.provider = provider;
  }


  @Override public boolean hasNativeFunction (DefinitionId id) {
    Map<TemplateArguments, Option<FunctionDefinition>> data = cache.get(id);
    if (data == null) {
      return provider.hasNativeFunction(id);
    }
    for (Option<FunctionDefinition> optFunc : data.values()) {
      if (optFunc.isSome()) {
        return true;
      }
    }
    return false;
  }

  @Override public Option<FunctionDefinition> getNativeFunction (DefinitionId id, TemplateArguments args) {
    // Cached Read
    Map<TemplateArguments, Option<FunctionDefinition>> data = cache.get(id);
    Option<FunctionDefinition> function;
    if (data == null) {
      // Cache miss: fetch...
      function = provider.getNativeFunction(id, args);
      data = new HashMap<TemplateArguments, Option<FunctionDefinition>>();
      data.put(args, function);
      cache.put(id, data);      // ..and cache
    } else {
      // Partial hit: Check for template args
      function = data.get(args);
      if (function == null) {
        // Cache miss: fetch...
        function = provider.getNativeFunction(id, args);
        data.put(args, function);       // ..and cache
      }
    }
    return function;
  }

  @Override public IDefinitionProvider cached () {
    return this;
  }
}
