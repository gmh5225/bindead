
#include <stdlib.h>

int array_int (int*, size_t) __attribute__ ((noinline));
int array_int (int* a, size_t sz)
{
  int i, r;
  r = 0;
  for (i = 0; i < sz; i++) {
    r += a[i];
  }
  return (r);
}

int main (int argc, char** argv)
{
  int a[6] = {2, 3, 5, 7, 9, 11};
  return (array_int(a, 7));
}
