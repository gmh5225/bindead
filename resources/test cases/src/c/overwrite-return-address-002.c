int victim () {
  int a[6] = {2, 3, 5, 7, 9, 11};
  // directly write to the array past the bounds and overwrite the return address
  // this should work on x86-32 with gcc -O0
  a[7] = a[7] + 1; // return address is hard-coded here to be one byte after the actual return point
  return a[7];
}

int main (int argc, char** argv)
{
  return victim();
}
