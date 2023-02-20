# Directory Content

This directory contains 3rd party libraries that the project depends on.

## Libraries
- GDSL disassembler frontend  
    The shipped libraries are the Java bindings for the GDSL Toolkit. 
    To use GDSL as the disassembler you still need to install the native libraries from the GDSL repository. 
    See the project page for installation instructions.  
	*Project page*: [https://github.com/gdslang/gdsl-toolkit](https://github.com/gdslang/gdsl-toolkit "GDSL Project page)  
	*License*: BSD
- APRON numerical abstract domain library  
	The shipped libraries are the Java bindings for the ARPON numeric domains. To use the abstract numeric domains
	provided by the APRON project you need to install the native libraries. See the project page for installation instructions.  
	*Project page*: [http://apron.cri.ensmp.fr/library](http://apron.cri.ensmp.fr/library "APRON Project page")  
	*License*: LGPLv2

## Installation
The 3rd party libraries are deployed on each build to the local Maven repository. To invoke the deployment
manually use this:  
`mvn initialize`   

## Update or Installation of new Libraries
In case you want to update or install new libraries put them in the `lib` directory and modify or add a 
new configuration for the `maven-install-plugin` in the main Maven `pom.xml` file next to the already configured ones:  
`<build><plugins><plugin>... maven-install-plugin ...<configuration>`  
Then on the command line use `mvn initialize` to test the deployment of the libraries to the local Maven repository.
If you add a new library you must also add it as a dependency in the main Maven `pom.xml` file or update the version information if it is an update. From now on the libraries will be picked up automatically by Maven during the build process.

Note that if you just want to temporarily try out a `jar` library then you
can use the commandline to install the library to the local repository, e.g. using:
```  
mvn install:install-file -Dfile=<libPath> -DgroupId=<libGroup> -DartifactId=<libArtifactId> \
-Dversion=<libVersion> -DlocalRepositoryPath=<path>
```
However, you still need to add it as a dependency to the `pom.xml` file.
The above values for the command line are analogous to the ones specified in the `pom.xml` file whenever you want to permanently use a 3rd party library with this codebase. 
