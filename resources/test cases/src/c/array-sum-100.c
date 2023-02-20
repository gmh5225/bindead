
#include <stdlib.h>

int main (int argc, char** argv)
{
  int m[100];
  int n = 1;

  int i;
  for (i = 0; i < 100; i++) {
     n += m[i];
  }

  return n;
}
