int f (int);
int a (int);
int b (int);
int c (int);
int u (int);
int v (int);
int w (int);

int f (int i) {
    return i;
}

int a (int x) {
	b(x);
	return x;
}

int b (int y) {
    c(y);
	return y;
}

int c (int y) {
    f(y);
    return y;
}

int u (int x) {
    v(x);
    return x;
}

int v (int y) {
    w(y);
    return y;
}

int w (int y) {
    f(y);
    return y;
}

int main (void) {
    int result = 0;
    a(0);
    u(1);
	return result;
}
