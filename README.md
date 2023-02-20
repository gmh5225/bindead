# Bindead

Bindead is a static analyzer for binaries (machine code) developed at the Technical University of Munich at the [chair for programming language research](http://www2.in.tum.de/).
It features a disassembler that translates machine code bits into an assembler like language (*RREIL* - Relational Reverse Engineering Intermediate Language) that in turn is then analyzed by the static analysis component using abstract interpretation. The analyzer allows to reason about the runtime behavior of a program and find potential bugs. To this end we perform a set of numeric and symbolic analyses on the program and are able to infer memory access bounds and various other numeric properties statically, i.e. without running the program.

A more detailed description along with publications and other material can be found on the [wiki of the project page][wiki-url]. 

## Disclaimer
The analyzer is still a research prototype, missing various features. We are working on improving stability and scalability, however there are still lots of things to do before the analyzer may easily be used in the wild. Currently it works on small examples but bails out with exceptions on more complex binaries. Adding a graceful degradation mechanism to continue an analysis even in cases of precision loss is the first thing on the TODO list.  
Furthermore, the analyzer is not a one-click solution to binary analysis. A static analysis of binaries will often require manual refinement and interpretation of the results. Especially understanding what caused the warnings shown by the analyzer will require manual inspection (the GUI may help there). To improve this we are planning to add a mechanism to allow the user to provide refinement hints to the analysis.


## Download and Usage
Prebuild packages will be available in the download section of the site in the future. Currently you will need to download the sources and build the project to be able to use it. See the build instructions below. After successfully building the analyzer you can use it from the command line or you can try the optional GUI.

### Command line
To invoke the analyzer from the command line type:   
`java -jar bindead.jar <options> <file>`  
Type `java -jar bindead.jar --help` for a description of the possible commands and see the [wiki][wiki-url] for a more detailed explanation of the analyzer's features.


### GUI
There is also a GUI for the analyzer, that allows to inspect the results of an analysis by displaying the control flow graph of a program. See the [p9 project][p9-url] for more information on how to install and use it.  

## Building

### Dependencies
Most dependencies are shipped with the code in the repository or downloaded during the build process. The analyzer is written in Java and is mostly self contained with some external Java dependencies. However, some optional components require native libraries to be installed on the system.  

The analyzer is written in Java, thus the dependencies are:

* JDK 1.7 (or higher)
* Maven 3 build tool

Optional (require native libraries):

- [GDSL disassembler frontend](https://github.com/gdslang/gdsl-toolkit)  
	An alternative and more complete disassembler library.
- [Apron library of abstract domains](http://apron.cri.ensmp.fr/library/)  
	 Additional numeric abstract domains for more precise analyses but also much slower.

### Build process
To download the sources clone the [Bindead repository][bindead-url] using `git`. Bitbucket shows you the clone url on the top right. Check out the project and change into the `bindead` directory. Then use one of the methods below to build the project:

#### Provided build script
* under Unix use the shipped `build.sh` script in the top-level directory. It will build all the components and copy the resulting program to `bindead.jar` to the current directory. The jar contains everything except the optional native dependencies. Run it using:  
`java -jar bindead.jar <options> <file>` 

#### Command line

Under Windows and Unix or anywhere else with a command line:

* go into the top-level directory `bindead-toplevel` and start with a clean build by issuing `mvn clean`
* compile and install the various subprojects by typing `mvn package -DskipTests` in the top-level directory
* the produced standalone jar is in `binspot/target/bindead-<version>-shaded.jar` . Run it using:  
`java -jar bindead-<version>-shaded.jar <options> <file>`

## Development
Feel free to fork and send pull requests or report bugs in the [issue tracker](https://bitbucket.org/mihaila/bindead/issues). The project is developed with both Eclipse and Netbeans, thus you can find code-style files for these two IDEs in the `resources` directory. Although untested, the project should be loadable into any IDE that integrates with the Maven build tool or use you favorite editor and build from the command line.

### Project Structure
The analyzer consists of various sub-projects that may also be used alone. Below is a brief description of the projects, 
where later ones depend on the ones mentioned before:

- **JavaLX**: Library for data structures (e.g. immutable collections) and tools used throughout the project
- **Binparse**: Parsers for executable file formats (e.g. ELF, PE) 
- **RReil**: The intermediate language used in the analyzer and semantic translation from disassembled instructions
- **Bindis**: Disassemblers for various architectures
- **Binspot**: The static analyzer and main driver for the disassembler

### License
The project is open source using the GPL v3 License. See [LICENSE](https://bitbucket.org/mihaila/bindead/src/master/LICENSE.txt) file for details.

### Importing into Eclipse
Check out the project using the url provided on the project site either on the command line or in Eclipse itself. 
Then use *"File->Import->Existing Maven Projects"* and select the `bindead` directory that you just checked out. Select all the sub-modules of the project, click *"Finish"* and you should have
imported everything into the workspace.  
Eclipse might show a building error for the `rreil` sub-project due to the `javacc` parser generator plugin for Maven. If this happens use the "autofix"
feature of Eclipse either by right-clicking on the error in the problems pane or using the "Quick-Fix" shortcut (default is "Ctrl-1"). Eclipse will suggest you to discover some new Maven lifecycle
connectors. Let it do this and it will download the right Maven plugin to be able to work with the project. This is only necessary for Eclipse as building the project on the command line works
without this connector download. Anyways, you should be now good to go.  
If Eclipse still shows some build errors try building the project once on the command line with Maven or the provided script 
(see building instructions) and then refresh the project (press F5) in Eclipse. A Maven refresh might be necessary, too. Use the context Menu for that. 

### Importing into Netbeans
Check out the project using the url provided on the project site either on the command line or in Netbeans itself by using *"Team->Git->Clone"*.
If you use Netbeans to clone the repository it will ask to search and open the projects. 
If you used the command line then use *"File->Open Project"* and make sure to check the "Open Required Projects" checkbox and select all the shown Maven projects. 
Netbeans sometimes has problems parsing the dependencies. If it does not show the dependency projects automatically then select them manually as projects to import and click "Open Project".  Now go to the main `bindead` project and in the context menu choose "Clean and Build". This will build all the projects. However, Netbeans might still show an error in the `rreil` sub-project due to the `javacc` parser generator plugin for Maven. If this happens perform a "Clean and Build" on the `rreil` sub-project and restart the IDE as Netbeans sometimes needs time to pick up the generated parser sources.  

### Testing the IDE import
To test that the project was successfully built in your IDE try running the main class in `binspot` or the tests in any of the project. Note that some tests may fail but if there are still build problems report the issue.

### Where to start
After you managed to import the project in the IDE or editor of your choice you may want to start to get accustomed to the codebase. One possible entry point is the command line handler class [`cli.Bindead`](https://bitbucket.org/mihaila/bindead/src/master/binspot/src/main/java/cli/Bindead.java) in the `binspot` sub-project. Alternatively look around in the unit tests of any sub-project. The `binspot` analyzer contains the most high-level tests and shows how you would invoke the analyzer on a binary.


## Contact
Bogdan Mihaila <mihaila@in.tum.de>

[wiki-url]: https://bitbucket.org/mihaila/bindead/wiki/Home
[p9-url]: https://bitbucket.org/mihaila/p9
[bindead-url]: https://bitbucket.org/mihaila/bindead "Bindead page"