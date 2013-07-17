#include <iostream>
#include <cmath>
using namespace std;

int main() {
	double x = double(2048)*2048*2048;
	long long y = round(x);
	cout << x << ' ' << y << endl;
	return 0;
}

