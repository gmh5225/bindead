package bindis.gdsl;

import gdsl.Frontend;
import gdsl.Gdsl;
import gdsl.arch.AVRBinder;
import gdsl.arch.ArchBinder;
import gdsl.arch.ArchId;
import gdsl.arch.IConfigFlag;
import gdsl.arch.X86Binder;
import gdsl.decoder.Decoder;
import gdsl.decoder.NativeInstruction;
import gdsl.rreil.IRReilBuilder;
import gdsl.translator.OptimizationConfig;
import gdsl.translator.OptimizationOptions;
import gdsl.translator.TranslatedBlock;
import gdsl.translator.Translator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import bindis.DecodeException;
import bindis.DecodeStream;
import bindis.NativeDisassembler;
import javalx.exceptions.UncheckedExceptionWrapper;
import rreil.gdsl.BindeadExpGdslRReilBuilder;
import rreil.gdsl.BindeadGdslRReilBuilder;
import rreil.lang.RReilAddr;

public class GdslNativeDisassembler implements NativeDisassembler {
  private final Frontend frontend;
  private static boolean OPTIMIZE = false;

  public Frontend getFrontend() {
    return frontend;
  }

  public GdslNativeDisassembler (ArchId arch, IConfigFlag[] flags) {
    ArchBinder binder;
    switch (arch) {
    case X86:
      binder = new X86Binder();
      break;
    case AVR:
      binder = new AVRBinder();
      break;
    default:
      throw new IllegalArgumentException();
    }

    binder.resetConfig();
    for (IConfigFlag iConfigFlag : flags)
      binder.setConfigFlag(iConfigFlag);

    this.frontend = binder.getFrontend();
  }

  private Gdsl initGdsl (DecodeStream in, long pc) {
    Gdsl gdsl = new Gdsl(frontend);
    gdsl.setCode(in.getBuffer(), in.getIdx(), pc);
    return gdsl;
  }

  @Override public GdslNativeInstruction decode (DecodeStream in, long pc) throws DecodeException {
    IRReilBuilder builder;
    if (OPTIMIZE) {
      // new builder that does not simplify complex linear expressions
      builder = new BindeadGdslRReilBuilder(RReilAddr.valueOf(pc));
    } else {
      // old builder that simplifies expressions to 3-address code by introducing temporary variables
      builder = new BindeadExpGdslRReilBuilder(RReilAddr.valueOf(pc));
    }
    Gdsl gdsl = initGdsl(in, pc);
    Decoder decoder = new Decoder(gdsl);
    NativeInstruction insn = decoder.decodeOne();

    int insnSize = (int) insn.getSize();
    byte[] code = new byte[insnSize];
    for (int i = 0; i < code.length; i++) {
      code[i] = (byte) in.read8();
    }
    Translator translator = new Translator(gdsl, builder);
    return new GdslNativeInstruction(insn, code, pc, translator);
  }

  @Override public GdslBlockOfInstructions decodeBlock (DecodeStream in, long pc) {
    IRReilBuilder builder;
    OptimizationConfig optimizationOptions;
    if (OPTIMIZE) {
      // Note that preserve context/block only make sense when used together with blockwise disassembly!
      optimizationOptions = OptimizationOptions.PRESERVE_CONTEXT.and(OptimizationOptions.LIVENESS).and(OptimizationOptions.FSUBST);
      // new builder that does not simplify complex linear expressions
      builder = new BindeadGdslRReilBuilder(RReilAddr.valueOf(pc));
    } else {
      optimizationOptions = OptimizationOptions.PRESERVE_EVERYWHERE.config();
      // old builder that simplifies expressions to 3-address code by introducing temporary variables
      builder = new BindeadExpGdslRReilBuilder(RReilAddr.valueOf(pc));
    }
    Gdsl gdsl = initGdsl(in, pc);
    Translator translator = new Translator(gdsl, builder);
    TranslatedBlock gBlock = translator.translateOptimizeBlock(Integer.MAX_VALUE, optimizationOptions);

    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    NativeInstruction[] gInsns = gBlock.getInstructions();
    for (int i = 0; i < gInsns.length; i++) {
      NativeInstruction gInsn = gInsns[i];
      try {
        bo.write(in.read((int) gInsn.getSize()));
      } catch (IOException e) {
        e.printStackTrace();
        throw new UncheckedExceptionWrapper(e);
      }
    }

    return new GdslBlockOfInstructions(gBlock, bo.toByteArray(), pc, translator);
  }

  /**
   * GDSL should perform optimizations to the RREIL code that result in less instructions
   * and more compact code.
   */
  public static void enableOptimizations () {
    OPTIMIZE = true;
  }

  /**
   * GDSL should not perform optimizations to the RREIL code.
   */
  public static void disableOptimizations () {
    OPTIMIZE = false;
  }
}
