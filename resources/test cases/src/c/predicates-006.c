
int main() {
  int a[100];
  int n, i;

  for (i = 0; i < 100; i++) {
    n = (a[i] < 42) ? n + 1 : n;
  }
  return n;
}

