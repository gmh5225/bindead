int f (int param) {
    int array[10] = {0};
    for (int i = 0; i <= 20; i++) {
        array[i] = param;
    }
    return param;
}

int main (void) {
    int result = f(123);
    return result;
}