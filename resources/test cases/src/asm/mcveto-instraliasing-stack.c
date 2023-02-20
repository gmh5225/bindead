//@summarystart
// Example which illustrates McVeto's ability to deal with
// instruction aliasing.
//@summaryend
//#include "../include/reach.h"

// This example is slightly modified to avoid the usage of a global data segment for the variable to be modified
// instead it uses a reference to modify a variable on the stack of the caller

int foo(int *a) { // In the object file, the address of foo is 0
  *a += 4;
  return 1;
}

int main() {
  int n = 3;

  asm (
    "nop\n"
    "mov $2, %eax\n"
    "lea -4(%ebp), %ebx\n" // load address of n; assumes the fixed offset from the base pointer, works only with GCC -O0
 "L1: mov $0x5bd0ff53, %ecx\n" // This instruction becomes "push ebx; call eax; pop ebx" when disassembled one byte later
    "cmp $1, %eax\n"
    "jz  L2\n"
    "lea foo, %eax\n"
    "lea L1+1, %edx\n"
    "jmp *%edx\n"
 "L2: xor %edx, %edx\n");

  return (n == 7);
}

