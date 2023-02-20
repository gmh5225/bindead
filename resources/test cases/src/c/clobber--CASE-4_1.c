
int main() {
  int n, j[2];
  if(n < 0 || n > 10)
    return 0;
  int i;
  for (i = 0; i < n; i++) {
    j[i] = i;
  }
  return j[0];
}
