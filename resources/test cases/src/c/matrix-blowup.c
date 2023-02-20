
#include <stdlib.h>

int array_int (int*, size_t) __attribute__ ((noinline));
int array_int (int* a, size_t sz)
{
  int i, r;

  r = 0;
  for ( i = 1; i < sz; i++) {
    r += a [i] - a [i-1];
  }

  return (r);
}

int main (int argc, char** argv)
{
  int m1[100][100];
  int n1 = 1;

  int i,j;
  for (i = 0; i < 100; i++) {
    for (j = 0; j < 100; j++) {
       n1 += m1[i][j];
    }
  }

  int m2[100][100];
  int n2 = 1;

  for (i = 0; i < 100; i++) {
    for (j = 0; j < 100; j++) {
       n2 += m2[i][j];
    }
  }
  return n1 + n2;
}
