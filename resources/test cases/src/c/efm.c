//@summarystart
// This example is adapted from StInG
// Sriram Sankaranarayanan, Henny Sipma and Zohar Manna,
// "Constraint-based Linear-Relations Analysis", in Static Analysis Symposium, 2004
//@summaryend
#include "../include/reach.h"


void foo(unsigned * comp, unsigned * sub, unsigned * add)
{
  if ((*sub >= 1) && (*comp >= 1)) {
    *sub=*sub-1;
    *add=*add+1;
  }
}


void bar(unsigned * dec, unsigned * inc, unsigned * added, unsigned * zeroed)
{
  if (*dec >= 1) {
    *dec = *dec - 1;
    *inc = *inc + 1;
    *added = *added + *zeroed;
    *zeroed = 0;
  }
}

int main()
{
  unsigned int X1;
  unsigned int X2;
  unsigned int X3;
  unsigned int X4;
  unsigned int X5;
  unsigned int X6;

  if ((X1>=1) && (X2==0) && (X3==0) && (X4==1) && (X5==0) && (X6==0)) {

    while(MakeChoice() & 1)
      {
	if (MakeChoice() & 1)
	  {
	    if ((X1 >= 1) &&(X4 >= 1)) {
	      X1=X1-1;
	      X4=X4-1;
	      X2=X2+1;
	      X5=X5+1;
	    }
	    else if (MakeChoice() & 1)
	      {
		foo(&X6, &X2, &X3);
	      }
	    else if (MakeChoice() & 1)
	      {
		foo(&X4, &X3, &X2);
	      }
	    else if (MakeChoice() & 1)
	      {
		bar(&X3, &X1, &X6, &X5);
	      }
	    else
	      {
		bar(&X2, &X1, &X4, &X6);
	      }
	  }
      }

    if (X4 + X5 + X6 -1 != 0 || X4 + X5 > 1 || X5  < 0 || X4  < 0 || X3  < 0
	|| X2  < 0 || X1 + X5 < 1 || X1 + X2 < X4 + X5 || X1 + X2 + X3 < 1) 
      UNREACHABLE();
  }
}

