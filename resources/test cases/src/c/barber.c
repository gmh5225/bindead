//@summarystart
// This example is adapted from StInG
// Sriram Sankaranarayanan, Henny Sipma and Zohar Manna,
// "Constraint-based Linear-Relations Analysis", in Static Analysis Symposium, 2004
//@summaryend

void new_customer(int* waiting_seat, int* free_barbers, int* used_chairs) {
  if ((*waiting_seat == 0) && (*free_barbers >= 1)) {
    *free_barbers = *free_barbers - 1;
    *used_chairs = *used_chairs + 1;
    *waiting_seat = 1;
  }
}

void serve_customer(int* waiting_seat, int* open) {
  if ((*waiting_seat ==1) && (*open >=1)) {
    *open = *open - 1;
    *waiting_seat = 0;
  }
}


int main(int argc, char** argv) {
  unsigned int barber;
  unsigned int chair;
  unsigned int open;
  unsigned int p1;
  unsigned int p2;
  unsigned int p3;
  unsigned int p4;
  unsigned int p5;
  unsigned int uninitialized;
  unsigned int uninitialized2;

  barber=0;
  chair=0;
  open=0;
  p1=0;
  p2=0;
  p3=0;
  p4=0;
  p5=0;

  while(uninitialized2 & 1)
    {
      if (uninitialized & 1)
	{
	  new_customer(&p1, &barber, &chair);
	}
      else if (uninitialized & 1)
	{
	  new_customer(&p2, &barber, &chair);
	}
      else if (uninitialized & 1)
	{
	  serve_customer(&p2, &open);
	}
      else if (uninitialized & 1)
	{
	  new_customer(&p3, &barber, &chair);
	}
      else if (uninitialized & 1)
	{
	  serve_customer(&p3, &open);
	}
      else if (uninitialized & 1)
	{
	  new_customer(&p4, &barber, &chair);
	}
      else if (uninitialized & 1)
	{
	  serve_customer(&p4, &open);
	}
      else if (uninitialized & 1)
	{
	  if (p5==0) {
	    barber=barber+1;
	    p5=1;
	  }
	}
      else if (uninitialized & 1)
	{
	  if ( (p5 == 1) && (chair >= 1)) {
	    chair = chair - 1;
	    p5 = 2;
	  }
	}
      else if (uninitialized & 1)
	{
	  if (p5 == 2) {
	    open = open + 1;
	    p5 = 3;
	  }
	}
      else if (uninitialized & 1)
	{
	  if((p5 == 3)&&(open == 0)) {
	    p5=0;
	  }
	}
      else
	{
	  serve_customer(&p1, &open);
	}
    }
  if (!(p5 >= open) || ! (p1 <= 1) || !(p2 <= 1) || !(p3 <= 1) || !(p4 <= 1) || !(p5 <= 3) 
      || !(p4 >= 0) || !(p3 >= 0) || !(p2 >= 0) || !(p1 >= 0) || !(open >= 0) || !(chair >= 0) 
      || !(barber >= 0)) 
    return 1; // UNREACHABLE();
  return 0;
}
