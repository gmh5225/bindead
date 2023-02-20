package bindead.platforms;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import rreil.abstractsyntax.Field;
import rreil.abstractsyntax.RReil;
import rreil.abstractsyntax.RReilAddr;
import rreil.abstractsyntax.Rhs.Rvar;
import rreil.assembler.CompilationUnit;
import rreil.assembler.ParseTreeTranslator;
import rreil.assembler.parsetree.ASTModule;
import rreil.assembler.parsetree.ParseException;
import rreil.assembler.parsetree.ParseTree;
import rreil.cfa.Cfa;
import rreil.cfa.util.CfaBuilder;
import rreil.disassembly.DisassemblyProvider;
import rreil.disassembly.RReilDisassemblyCtx;

/**
 * A helper class that loads CFGs from RREIL assembler files.
 *
 * @author Bogdan Mihaila
 */
public class AssemblyLoader {
  private final CompilationUnit compilationUnit;
  private final Cfa cfa;

  public AssemblyLoader (CompilationUnit cu) throws ParseException {
    compilationUnit = cu;
    cfa = CfaBuilder.build(getInstructions(), compilationUnit.entryAddress());
  }

  public AssemblyLoader (String filePath) throws IOException, ParseException {
    compilationUnit = loadCompilationUnit(filePath);
    cfa = CfaBuilder.build(getInstructions(), compilationUnit.entryAddress());
  }

  public Cfa getCfa () {
    return cfa;
  }

  public CompilationUnit getCompilationUnit () {
    return compilationUnit;
  }

  public Field getField (String name) {
    return getVariable(name).asUnresolvedField();
  }

  public Rvar getVariable (String name) {
    return compilationUnit.getIdentifiers().get(name);
  }

  public static CompilationUnit loadCompilationUnit (String filePath) throws IOException, ParseException {
    InputStream in = new FileInputStream(filePath);
    ParseTree p = new ParseTree(in);
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    CompilationUnit cu = translator.translateModule(module);
    in.close();
    return cu;
  }

  public static Cfa loadCfa (String filePath) throws IOException, ParseException {
    AssemblyLoader provider = new AssemblyLoader(filePath);
    return provider.getCfa();
  }

  public static Cfa loadInlineAssembly (String assembly) throws ParseException {
    ParseTree p = new ParseTree(new StringReader(assembly));
    ASTModule module = p.Module();
    ParseTreeTranslator translator = new ParseTreeTranslator();
    CompilationUnit cu = translator.translateModule(module);
    AssemblyLoader loader = new AssemblyLoader(cu);
    return loader.cfa;
  }

  public RReilDisassemblyCtx getInstructions () {
    final RReilDisassemblyCtx disassembly = new RReilDisassemblyCtx();
    List<RReil> instructions = new ArrayList<RReil>();
    instructions.addAll(getCompilationUnit().getInstructionMapping().values());
    disassembly.add(instructions, null);
    return disassembly;
  }

  public DisassemblyProvider getInstructionsProvider () {
    return new DisassemblyProvider() {
      @Override public RReilDisassemblyCtx decodeFrom (RReilAddr start) {
        return getInstructions();
      }
    };
  }
}
