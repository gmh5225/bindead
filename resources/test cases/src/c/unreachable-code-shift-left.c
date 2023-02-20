
#include <stdlib.h>

int shiftLeft (int) __attribute__ ((noinline));
int shiftLeft (int n) {return n << 4; }

int main (int argc, char** argv)
{
  return (shiftLeft(argc));
}
