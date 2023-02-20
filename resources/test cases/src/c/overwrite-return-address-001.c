
#include <stdlib.h>

int array_init (int*, size_t) __attribute__ ((noinline));
int array_init (int* a, size_t sz)
{
  int i, r;
  r = 0;
  for (i = 0; i < sz; i++) {
    a[i] = r++;
  }
  return r;
}

int victim () {
  int a[6];
  // write to the array past the bounds and overwrite the return address
  // depending on the architecture and compiler the return address is at a different offset
  return array_init(a, 15);
}

int main (int argc, char** argv)
{
  return victim();
}
