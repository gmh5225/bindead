int f (int z);
int a (int x);
int b (int y);

int f (int i) {
    return i;
}

int a (int x) {
	f(x);
	return x;
}

int b (int y) {
    f(y);
	return y;
}

int main (void) {
    int result = 0;
    a(0);
    b(1);
	return result;
}
