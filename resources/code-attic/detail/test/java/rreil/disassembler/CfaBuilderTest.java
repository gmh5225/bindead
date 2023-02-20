package rreil.disassembler;

import java.io.IOException;

import org.junit.Test;

import rreil.BinaryLoader;
import rreil.platform.api.RReilPlatforms;
import rreil.platform.RReilPlatform;
import rreil.cfa.Cfa;
import rreil.cfa.util.CfaHelpers;

/**
 * Tests the main RREIL to CFG builder by
 * using the disassembler to retrieve an instruction list
 * from an executable file and building CFGs for them.
 *
 * @author Bogdan Mihaila
 */
public class CfaBuilderTest {
  private static final String rootPath = CfaBuilderTest.class.getResource("/executable/x86_32").getPath();
  private static final RReilPlatform platform = RReilPlatforms.X86_32;

  @Test public void disSimple () throws IOException {
    renderCfa("simple-jump.o");
  }

  @Test public void disCfa000 () throws IOException {
    renderCfa("cfa-builder-000.o");
  }

  @Test public void disCfa001 () throws IOException {
    renderCfa("cfa-builder-001.o");
  }

  @Test public void disCfa002 () throws IOException {
    renderCfa("cfa-builder-002.o");
  }

  @Test public void disCfa003 () throws IOException {
    renderCfa("cfa-builder-003.o");
  }

  @Test public void disCfa004 () throws IOException {
    renderCfa("cfa-builder-004.o");
  }

  @Test public void disCfa005 () throws IOException {
    renderCfa("cfa-builder-005.o");
  }

  @Test public void disCfa006 () throws IOException {
    renderCfa("cfa-builder-006.o");
  }

  private void renderCfa (String fileName) throws IOException {
    String elfFilePath = rootPath + "/" + fileName;
    Cfa cfa = new BinaryLoader(elfFilePath, platform).cfaOfRecursiveDescent("main");
    CfaHelpers.renderCfa(cfa, elfFilePath + ".graphml");
  }
}
