package rreil.assembler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

import rreil.assembler.parser.ASTModule;
import rreil.assembler.parser.ParseException;
import rreil.assembler.parser.RReilParser;
import rreil.lang.RReilAddr;

public class LabelCollectorTest {
  @Test public void collectInstructionFromModuleWithoutLabels () throws ParseException {
    String input = ".@sp.d@?\n.@bp.d@0x100\nmov.d r1,100\nmov.d r2,100\nadd.d r3,r1,r2";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    ASTModule module = p.Module();
 // output the module structure if needed for debugging
//  module.dump("");

    LabelCollector visitor = new LabelCollector();
    module.jjtAccept(visitor, null);

    assertTrue(0 == visitor.getLabels().size());
  }

  @Test public void collectLabelsAndInstructions () throws ParseException {
    String input = ".@sp.d@?\n.@bp.d@0x100\nmain:\nmov.d r1,100\nmov.d r2,100\nadd.d r3,r1,r2\nexit:";
    Reader in = new BufferedReader(new StringReader(input));
    RReilParser p = new RReilParser(in);

    ASTModule module = p.Module();
// output the module structure if needed for debugging
//    module.dump("");

    LabelCollector visitor = new LabelCollector();
    module.jjtAccept(visitor, null);

    Map<String, RReilAddr> labels = visitor.getLabels();
    RReilAddr addressOfMain = labels.get("main");
    RReilAddr addressOfExit = labels.get("exit");

    assertTrue(2 == labels.size());
    assertEquals(RReilAddr.valueOf(0), addressOfMain);
    assertEquals(RReilAddr.valueOf(3), addressOfExit);
  }
}
