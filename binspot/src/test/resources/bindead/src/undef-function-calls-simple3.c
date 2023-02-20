int f (int z);
int a (int x);
int b (int y);

int f (int i) {
    return i + 42;
}

int a (int x) {
    int res = f(x);
    return x + res;
}

int b (int y) {
    int res = f(y);
	return y + res;
}

int main (void) {
    int result = 0;
    result = result + a(0);
    result = result + b(1);
	return result;
}
