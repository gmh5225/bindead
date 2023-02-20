package bindead.analyses.systems.natives;

import java.util.SortedMap;

import javalx.data.Option;
import rreil.assembler.SingleFunctionParser;
import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.RReilParser;
import rreil.lang.RReil;
import rreil.lang.RReilAddr;

public class DefaultDefinitionProvider implements IDefinitionProvider {
  private final IDefinitionLoader loader;

  public DefaultDefinitionProvider (IDefinitionLoader loader) {
    this.loader = loader;
  }

  @Override public boolean hasNativeFunction (DefinitionId id) {
    return loader.hasEntry(id);
  }

  @Override public Option<FunctionDefinition> getNativeFunction (DefinitionId id, TemplateArguments args) {
    final Option<RawDefinition> rawData = loader.getEntry(id);
    if (rawData.isNone())
      return Option.none();

    try {
      // Parse function definition
      RReilParser tree = new RReilParser(rawData.get().wrap());
      ASTModule module = tree.Module();
      SingleFunctionParser parser = new SingleFunctionParser(args.getInputVars(), args.getOutputVars(), null);
      SortedMap<RReilAddr, RReil> instructions = parser.instantiateFunctionTemplate(module);

      return Option.some(new FunctionDefinition(instructions));
    } catch (ParseException err) {
      System.err.println("Error parsing a native function definition:");
      err.printStackTrace();
    }

    return Option.none();
  }

  @Override public IDefinitionProvider cached () {
    return new ProviderCacheDecorator(this);
  }
}
