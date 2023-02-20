#!/bin/sh

## Collects information about a compiled binary and the system and writes it to a file accompanying the binary.
## Use it by prepending it to the commandline of gcc or other tools.

# author: Bogdan Mihaila
# license: GPL2 and later

set -o errexit  # immediately exit on errors
set -o nounset  # use of uninitialized variables is an error
## below is a bashism
#set -o pipefail # fail on first command failure in a pipe of commands

systemInfo () {
  linuxDistribution="$(lsb_release -a)"
  kernelVersion="$(uname -svrmpio)"
}

generalInfo () {
  timestamp="$(date +'%F-%T')"
}

gccInfo () {
  gccVersion="$(gcc --version | head -n 1)"
  ldVersion="$(ld --version | head -n 1)"
  libcVersion="$(ldd --version | head -n 1)"
  gccCommand="$@"
  set +e
  echo "$@" | grep -q "\-O"
  if [ "$?" = 0 ]
  then
    optimizationLevel=$(echo "$@" | sed -ne 's/.*-O\([0-9]\|s\|fast\| \).*/\1/p')
  else
    optimizationLevel="0"
  fi
  # -O without a number means -O1
  if [ "$optimizationLevel" = " " ]
  then
    optimizationLevel=1
  fi
  set -e
}

binaryInfo () {
  machine=$(readelf -h "$binaryFileName" | grep "Machine" | sed -e 's/^.*Machine:[ \t]*//')
  fileInfo=$(file "$binaryFileName")
  archBits=$(echo "$fileInfo" | sed -ne 's/.* \([0-9]*\)-bit .*/\1/p')
  set +e
  echo "$fileInfo" | grep -q "not stripped"
  if [ "$?" = 0 ]
  then
    symbolTable="present"
  else
    symbolTable="not present"
  fi
  readelf -w "$binaryFileName" | grep -q ".debug"
  if [ "$?" = 0 ]
  then
    debugInfo="present"
  else
    debugInfo="not present"
  fi
  echo "$fileInfo" | grep -q "statically"
  if [ "$?" = 0 ]
  then
    linked="static"
  else
    linked="dynamic"
  fi
  echo "$fileInfo" | grep -q "LSB"
  if [ "$?" = 0 ]
  then
    endianness="little"
  else
    endianness="big"
  fi
  echo "$fileInfo" | grep -q "executable"
  if [ "$?" = 0 ]
  then
    type="executable"
  else
    type="shared library"
  fi

  ## security mechanisms checks below are taken from http://www.trapkit.de/tools/checksec.html
  readelf -s "$binaryFileName" | grep -q "__stack_chk_fail" 
  if [ "$?" = 0 ]
  then
    canary="yes"
  else
    canary="no"
  fi
  readelf -W -l "$binaryFileName" | grep 'GNU_STACK' | grep -q 'RWE'
  if [ "$?" = 0 ]
  then
    noExecute="no"
  else
    noExecute="yes"
  fi
  readelf -l "$binaryFileName" | grep -q 'GNU_RELRO'
  if [ "$?" = 0 ]
  then
    relocationRO="yes"
  else
    relocationRO="no"
  fi
  readelf -d "$binaryFileName" | grep -q 'BIND_NOW'
  if [ "$?" = 0 ]
  then
    gotRO="yes"
  else
    gotRO="no"
  fi
  readelf -s "$binaryFileName" | awk '{ print $8 }' | sed 's/_*//' | sed -e 's/@.*//' | grep -q "_chk"
  if [ "$?" = 0 ]
  then
    fortifySource="yes"
  else
    fortifySource="no"
  fi
  set -e
}

## example GCC invocation: gcc -m32 -O0 -o bla bla.c
handleGCC () {
  "$@" # run the commandline
  systemInfo
  generalInfo
  gccInfo "$@"
  # TODO: iterate over all params and grep for the one ending with .c to be able to
  # handle filenames containing spaces
  # sourceFileName=$(echo "$@" | sed -ne 's/.* \([^ ]*\.c\).*/\1/p')
  sourceFileName=$(echo "$@" | grep -E -o "[^ ]*\.c")
  binaryFileName=$(echo "$@" | sed -ne 's/.* -o[ ]*\([^ ]*\).*/\1/p')
  if [ -z "$binaryFileName" ]
  then
    binaryFileName="a.out"
  fi
  binaryInfo
  toolChain="gcc"
  printResults
  echo "Successfully saved the info to the file $outputFile"
  exit 0
}

handleFasm () {
  "$@" # run the commandline
  systemInfo
  generalInfo
  fasmVersion=$(fasm | head -n1)
  sourceFileName=$(echo "$@" | grep -E -o "[^ ]*\.fasm")
  # TODO: retrieve the last parameter (i.e. with binaryFileName="${@: -1}") and then see if it ends in .fasm
  # if not it should be the output filename
  binaryFileName="${sourceFileName%.fasm}.o"
  binaryInfo
  toolChain="fasm"
  printResults
  echo "Successfully saved the info to the file $outputFile"
  exit 0
}

printResults () {
  outputFile="$binaryFileName.info"
  INFOS_VERSION=0.3 # To know with what version of this script the infos were generated
  
  printHeaderInfo
  case "$toolChain" in
    gcc) printGCCInfo ;;
    fasm) printFasmInfo ;;
    *) exit 2 ;;
  esac
  printBinaryInfo
}

printHeaderInfo () {
cat >"$outputFile" <<End-of-Message
Info-Version: $INFOS_VERSION
Compilation info for source file "$sourceFileName" that was compiled to "$binaryFileName" on $timestamp
--------------------------------------------------------------------------------

System: $linuxDistribution
Kernel: $kernelVersion

--------------------------------------------------------------------------------
Toolchain
--------------------------------------------------------------------------------
End-of-Message
}

printGCCInfo () {
cat >>"$outputFile" <<End-of-Message
Compiler: $gccVersion
Optimization-Level: $optimizationLevel
Compiler command line: "$gccCommand"

Linker: $ldVersion
Libc: $libcVersion

End-of-Message
}

printFasmInfo () {
cat >>"$outputFile" <<End-of-Message
Assembler: $fasmVersion

End-of-Message
}

printBinaryInfo () {
cat >>"$outputFile" <<End-of-Message
--------------------------------------------------------------------------------
Binary
--------------------------------------------------------------------------------
Machine: $machine
Type: $type
Bits: $archBits
Endianness: $endianness
Linking: $linked
Symbol-Table: $symbolTable
Debug info: $debugInfo

Security Measures:
--------------------------------------------------------------------------------
No-Execute bit (NX): $noExecute
Stack Canary: $canary
Relocation Read-Only: $relocationRO
Global Offset Table (GOT) Read-Only: $gotRO
Fortify Source: $fortifySource
End-of-Message
}

defaultGCCOptions="-O0 -g"
# do a compilation of a C-file with the default parameters
compileWithGCC () {
  compile.sh gcc $defaultGCCOptions -o "${1%.c}-O0" "$1"
}

printUsageInformation () {
  scriptName=$(basename "$0")
  echo "Usage:"
  echo "Either you use a compiler/assembler command as the parameters to this script e.g.:"
  echo "$scriptName gcc -O0 -m32 -g -o prog prog.c"
  echo "Or you specify a C source file that will be compiled with GCC and some default options (i.e. \"gcc $defaultGCCOptions\")."
  exit 1
}

## program start
if [ "$#" = 0 ]
then
  printUsageInformation
fi

if [ "$1" = "gcc" ]
then 
  handleGCC "$@"
elif [ "$1" = "fasm" ] 
then
  handleFasm "$@"
elif echo "$1" | grep -E -q "^.*\.c"
then
  compileWithGCC "$@"
else
  echo
  echo "You did not use a known compiler/assembler or did not specify a C source file to be compiled. Your parameters: $1"
  printUsageInformation
fi
