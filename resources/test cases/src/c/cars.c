//@summarystart
// This example is adapted from StInG
// Sriram Sankaranarayanan, Henny Sipma and Zohar Manna,
// "Constraint-based Linear-Relations Analysis", in Static Analysis Symposium, 2004
//@summaryend
#include "../include/reach.h"


void doThen(unsigned int *x1, unsigned int *x2, unsigned int *x3, unsigned int *v1, unsigned int *v2, unsigned int *v3, unsigned int *t )
{
  *x1 = *x1+*v1;
  *x3 = *x3+*v3;
  *x2 = *x2+*v2;
  *v2 = *v2-1;
  *t = *t+1;

}

void doElse(unsigned int *x1, unsigned int *x2, unsigned int *x3, unsigned int *v1, unsigned int *v2, unsigned int *v3, unsigned int *t)
{
  *x1 = *x1+*v1;
  *x3 = *x3+*v3;
  *x2 = *x2+*v2;
  *v2 = *v2+1;
  *t = *t+1;

}

int main()
{
  unsigned int x1;
  unsigned int v1;
  unsigned int x2;
  unsigned int v2;
  unsigned int x3;
  unsigned int v3;
  unsigned int t;

  x1=100;
  x2=75;
  x3=-50;
  t=0;

  if ( (v3 >= 0) && (v1 <= 5) && (v1 -v3 >= 0) && (2* v2 - v1 - v3 == 0) && (v2 +5 >=0) && (v2 <= 5))
    {
      while (MakeChoice() & 1)
	{
	  if (-5 <= v2 && v2 <= 5)
	    {
	      if (2*x2-x1-x3 >= 0)
		{
		  doThen(&x1, &x2, &x3, &v1, &v2, &v3, &t);
		}
	      else
		{
		  doElse(&x1, &x2, &x3, &v1, &v2, &v3, &t);
		}
	    }
	}
      if (v1 > 5 || 2*v2 + 2*t  < v1 + v3 || 5*t  + 75 < x2 || v2 > 6 || v3 < 0 || v2 + 6 < 0
	  || x2 + 5*t < 75 || v1 - 2*v2 + v3 + 2*t < 0 || v1 - v3 < 0)
	UNREACHABLE();

    }
  return 0;
}
