int testTheSpec1 () 
{
	int[N] in = { RED, RED, WHITE, BLUE, RED };
	int[N] out = spec(in);
	assert(out[0]==RED && out[1]==RED && 
	       out[2]==WHITE && 
	       out[3]==BLUE && out[4]==BLUE);
	return 1;
}

// In this test, holes tell us the content of the out array. 
int testTheSpec2 () 
{
	int[N] in = { RED, WHITE, BLUE, RED, BLUE };
	int[N] out = spec(in);
	int i = 0;
	loop(N) { assert out[i]==??; i++; }
	return 1;
}

int testTheSpec3 () 
{
	int[N] in = { RED, WHITE, BLUE, RED, BLUE };
	int[N] out1 = dutch(in);
	int[N] out2 = spec(in);
	int i = 0;
	loop(N) { assert out1[i]==out2[i]; i++; }
	return 1;
}