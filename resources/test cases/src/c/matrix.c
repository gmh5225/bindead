
#include <stdlib.h>

int main (int argc, char** argv)
{
  int m[100][100];
  int n = 1;

  int i,j;
  for (i = 0; i < 100; i++) {
    for (j = 0; j < 100; j++) {
       n += m[i][j];
    }
  }

  return n;
}
