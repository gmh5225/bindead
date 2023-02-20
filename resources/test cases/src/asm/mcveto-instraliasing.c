//@summarystart
// Example which illustrates McVeto's ability to deal with
// instruction aliasing. 
//@summaryend
// %keyword cav10-2
//#include "../include/reach.h"

int n;

int foo(int a) {
  n += a;
  return 1;
}

int main() {
  int x;
  int y;
  n = y;

  if (y < 0)
      n = 3;
  else
      n = 4;

  asm (
      "mov $2, %eax\n"
      "mov $4, %ebx\n"
  "L1: mov $0x5bd0ff53, %edx\n" // This instruction becomes "push ebx; call eax; pop ebx" when disassembled one byte later
      "cmp $1, %eax\n"
      "jz  L2\n"
      "lea foo, %eax\n"
      "lea L1+1, %ebx\n"
      "jmp *%ebx\n"
  "L2: xor %edx, %edx\n");

  return (n == 7);  
}