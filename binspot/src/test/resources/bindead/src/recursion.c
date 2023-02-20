int rand;

// variable sized array on stack
int var_array (int i) {
	int array[i];
	for (int j = 0; j < i; j++) {
		array[j] = j;
	}
	return i + 1;
}

int rec_nonterm (int i) {
	return rec_nonterm(i + 1);
}

int rec_simple (int i) {
	if (i > 100)
		return i;
	else
		return rec_simple(i + 1);
}

int main (void) {
	int i = 20;
	i = rec_simple(i);
	i = rec_nonterm(i);
	i = var_array(i);
	return i;
}
