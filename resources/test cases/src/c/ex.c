//@summarystart
/* The target is reachable when the execution has a */
/* buffer overflow which                   */
/* (i) overwrites the fields "n" and "state" (along */
/* with "padding") in such a way that "state" is set to */
/* 8 and "n" is a small enough number so that the */
/* return address in foo's activation record is not */
/* clobbered, or (ii) overwrites the return address */
/* in foo's activation record so that the return from foo() */
/* goes directly to the site of the call on REACHABLE() */
/* in main().  */
//@summaryend
#include "../include/reach.h"

void target() {}
int main(){ 
  if(foo() == 1) REACHABLE();
  return 0;
}
int foo()
{
  struct s {
    int B[100];
    int A[3];
    char n;
    int padding;
    char state;
  } t;
  int i;
  t.n = 3;
  t.state = 7;
  for (i=0; i<=t.n; i++) 
    t.A[i] = t.B[i];
  if (t.state==8)  
    return 1;
  return 0;
}
