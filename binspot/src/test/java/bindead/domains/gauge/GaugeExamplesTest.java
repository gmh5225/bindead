package bindead.domains.gauge;

import static bindead.TestsHelper.lines;

import org.junit.Ignore;
import org.junit.Test;

import bindead.TestsHelper;
import bindead.analyses.Analysis;
import bindead.analyses.AnalysisFactory;
import bindead.debug.DebugHelper;

@Ignore("Not yet functional") // remove when working again on Gauge Domain
public class GaugeExamplesTest {
  private final static AnalysisFactory analyzer = new AnalysisFactory(
      "Root Fields Predicate PointsTo Wrapping DelayedWidening ThresholdsWidening Affine Gauge Intervals");

  @Test
  public void example0 () {
    // i = 0;
    // while (i < 100) {
    //  i++; x+=2;
    // }
    String assembly = lines(
        "mov.b i, 0 ",
        "mov.b x, 0 ",
        "loop: ",
        "cmples.b LES, 100, i ",
        "assert.b i = [0, 100] ",
        "brc.d LES, exit_loop: ",
        "assert.b i = [0, 99] ",
        "add.b i, i, 1 ",
        "add.b x, x, 2",
        "br.d loop: ",
        "exit_loop: ",
        "assert.b i = 100 ",
        "assert.b x = 200",
        "halt");
//    DebugHelper.analysisKnobs.printCodeListing();
//    DebugHelper.analysisKnobs.printInstructions();
//    DebugHelper.analysisKnobs.printWidening();
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test
  public void example1 () {
    // int x=0
    // int i=0
    // for(i=0; i < 100; i++) {
    //   if (*)
    //    x++;
    //   else
    //    x+=2;
    // }
    String assembly = lines(
        "mov.b x, 0",
        "mov.b i, 0",
        "loop:",
        "cmples.b LES, 100, i",
//        "assert.b i = [0, 100] ",
        "brc.b LES, exit_loop:",
//        "assert.b i = [0, 99] ",
        "mov.b RND, ?",
        "brc.b RND, else:",
        "add.b x, x, 2",
        "br.b end_if:",
        "else:",
        "add.b x, x, 1",
        "end_if:",
        "add.b i, i, 1",
        "br.b loop:",
        "exit_loop:",
        "assert.b i = 100 ",
        "assert.b x = [0, 200]", // <- this is the best we get.
//        "assert.b x = [100, 200]", // <- this is what we'd really want.
        "halt");
//    DebugHelper.analysisKnobs.printCodeListing();
//    DebugHelper.analysisKnobs.printInstructions();
    //DebugHelper.analysisKnobs.printWidening();
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Ignore("Fails due to undefined widening when a counter changes from [0,1] to [1]")
  @Test
  public void example2 () {
	 // int x=0, y=0;
	// while(y!=1) {
	// x++;
	// y=1;
	// }
    String assembly = lines(
        "mov.b i, 0 ",
        "mov.b x, 0 ",
        "loop: ",
        "cmpeq.b EQ, i, 1 ",
        "assert.b i = 1 ",
        "brc.d EQ, exit_loop: ",
        "mov.b i, 1 ",
        "add.b x, x, 1",
        "br.d loop: ",
        "exit_loop: ",
        "assert.b i = 1",
        "assert.b x = 1",
        "halt");
    DebugHelper.analysisKnobs.printCodeListing();
    DebugHelper.analysisKnobs.printInstructions();

    DebugHelper.analysisKnobs.printWidening();
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Test
  public void example3 () {
	  /** for(i=0;i < 100;i++) {
	   *  if(*)
	   *  	x++
	   *  	i+=2
	   *  else
	   *  	x+=2
	   *  	i++
	   *  }
	   */
	  String assembly = lines(
			  "mov.b x, 0",
			  "mov.b i, 0",
			  "loop:",
			  	"cmples.b LES, 100, i",
			  	"brc.b LES, exit_loop:",
			  	"mov.b RND, ?",
			  	"brc.b RND, else:",
			  		"add.b x, x, 2",
			  		"add.b i, i, 1",
			  		"br.b end_if:",
			  	"else:",
			  		"add.b i, i, 2",
			  		"add.b x, x, 1",
			  	"end_if:",
			  	"br.b loop:",
			  	"exit_loop:",
			  // "assert.b x = [50, 200]", // <- this is what we'd like to get
			  "assert.b x = [0, +oo]", // <- this is the best the current implementation can do
			  // "assert.b i = [100,101]", // <- this is what we'd like to get
			  "assert.b i = [100, 127 ]", // <- this is the best the current implementation can do
              "halt");
    DebugHelper.analysisKnobs.printCodeListing();
    DebugHelper.analysisKnobs.printInstructions();

    DebugHelper.analysisKnobs.printWidening();
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

  @Ignore("Fails due to DomainStateException in affine")
  @Test
  public void example4 () {
	  // for(i=0;i<10;i++) {
	  //  for(j=0;j<10;j++) {
	  //    i++;
	  //  }
	  // }
	  String assembly = lines(
			  "mov.b i, 0",
			  "mov.b j, 0",
			  "loop0:",
			  "cmples.b LES, 10, i",
			  "brc.b LES, exit_loop0:",
			    "loop1:",
			  	"cmples.b LES, 10, j",
			  	"brc.b LES, exit_loop1:",
			  	"add.b i, i, 1",
			  	"add.b j, j, 1",
			  	"br.b loop1:",
			  	"exit_loop1:",
			  "add.b i, i, 1",
			  "br.b loop0:",
			  "exit_loop0:",
			  "assert.b i = [10,20]",
			  "assert.b j = 10",
              "halt");
    DebugHelper.analysisKnobs.printCodeListing();
    DebugHelper.analysisKnobs.printInstructions();

    DebugHelper.analysisKnobs.printWidening();
    Analysis<?> analysis = analyzer.runAnalysis(assembly);
    TestsHelper.evaluateAssertions(analysis);
  }

}
