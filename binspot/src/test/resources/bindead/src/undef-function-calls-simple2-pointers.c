int f (int*);
int a (int*);
int b (int*);
int c (int*);

int f (int *i) {
    return *i;
}

int a (int *x) {
	f(x);
	return *x;
}

int b (int *y) {
    f(y);
	return *y;
}

int c (int *y) {
    f(y);
    return *y;
}


int main (void) {
    int result = 0;
    int x = 0;
    int y = 1;
    int z = 2;
    a(&x);
    b(&y);
    c(&z);
	return result;
}
