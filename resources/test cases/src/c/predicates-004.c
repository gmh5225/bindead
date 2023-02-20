
// @return eax \in [-2, 2]
int main() {
  int n;
  if (n >= -2 && n <= 2)
    // n \in [-2, +2]
    return n;
  return 0;
}