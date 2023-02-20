// Example with an Acceptable Execution (AE) violation
void bar() {
  //REACHABLE();
  return; // return to main
}

void foo() {
  int arr[2], n;
  void (*addr_bar)() = bar;
  // n = MakeChoice();

  if( n<0 || n > 10) return ;
  int i;
  for(i=0; i<n; i++) {
    arr[i] = (int)addr_bar; /* Can overwrite return address on stack with address of bar*/
  }
  return ; // can return to bar, instead of main
}

int main() {
  foo();
  return 0;
}
