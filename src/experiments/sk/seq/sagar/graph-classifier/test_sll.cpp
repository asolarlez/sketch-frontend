#include <iostream>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#define N 3
using namespace std;
void s(int rels[N][N], int x, int result[N]) {
	for(int i = 0; i < N; i++) {
		result[i] = -1;
	}
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(rels[x][i] == 1) {
			result[count] = i;
			count++;
		}
	}
}

int countS(int rels[N][N], int x) {
	int result[N]; 
	s(rels, x, result);
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(result[i] == -1)
			break;
		count++;
	}
	return count;
}

void p(int rels[N][N], int x, int result[N]) {
	for(int i = 0; i < N; i++) {
		result[i] = -1;
	}
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(rels[i][x] == 1) {
			result[count] = i;
			count++;
		}
	}
}

int countP(int rels[N][N], int x) {
	int result[N]; 
	p(rels, x, result);
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(result[i] == -1)
			break;
		count++;
	}
	return count;
}

bool isPresent(int arr[N], int x) {
	for(int i = 0; i < N; i++) {
		if(arr[i] == x)
			return true;
	}
	return false;
}

void merge(int arr1[N], int arr2[N], int result[N]) {
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(arr1[i] != -1) {
			result[count] = arr1[i];
			count++;
		}
	}
	for(int i = 0; i < N; i++) {
		if(arr2[i] != -1 && !isPresent(result, arr2[i])) {
			result[count] = arr2[i];
			count++;
		}
	}
	for(int i = count; i < N; i++) {
		result[count] = -1;
	}
}

void sPlus(int rels[N][N], int x, int result[N]) {
	int curr[N];
	int processed[N];
	for(int i = 0; i < N; i++) {
		result[i] = -1;
		curr[i] = -1;
		processed[i] = -1;
	}
	curr[0] = x;
	int count = 0;
	int processedCount = 0;
	while(count != -1) {
		int temp[N];
		s(rels, curr[0], temp);
		processed[processedCount] = curr[0];
		processedCount++;
		for(int i = 0; i < N - 1; i++) {
			curr[i] = curr[i + 1];
		}
		count--;	
		if(temp[0] != -1) {
			merge(result, temp, result);
			for(int i = 0; i < N; i++) {
				if(temp[i] != -1 && !isPresent(processed, temp[i]) && !isPresent(curr, temp[i])) {
					count++;
					curr[count] = temp[i];
				}				
			}
		}
	}
}

void sStar(int rels[N][N], int x, int result[N]) {
	int curr[N];
	int processed[N];
	for(int i = 0; i < N; i++) {
		result[i] = -1;
		curr[i] = -1;
		processed[i] = -1;
	}
	result[0] = x;
	curr[0] = x;
	int count = 0;
	int processedCount = 0;
	while(count != -1) {
//		cout << "result: ";
//		for(int i = 0; i < N; i++) {
//			cout << result[i] << ", ";
//		}
//		cout << "curr: ";
//		for(int i = 0; i < N; i++) {
//			cout << curr[i] << ", ";
//		}
//		cout << "processed: ";
//		for(int i = 0; i < N; i++) {
//			cout << processed[i] << ", ";
//		}
//		cout << "processed count: " << processedCount;
//		cout << endl;
		int temp[N];
		s(rels, curr[0], temp);
		assert(processedCount < N);
		processed[processedCount] = curr[0];
		processedCount++;
		for(int i = 0; i < N - 1; i++) {
			curr[i] = curr[i + 1];
		}
		count--;	
		if(temp[0] != -1) {
			merge(result, temp, result);
			for(int i = 0; i < N; i++) {
				if(temp[i] != -1 && !isPresent(processed, temp[i]) && !isPresent(curr, temp[i])) {
					count++;
					curr[count] = temp[i];
				}				
			}
		}
	}
}
int countSPlus(int rels[N][N], int x) {
	int result[N];
 	sPlus(rels, x, result);
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(result[i] == -1)
			break;
		count++;
	}
	return count;
}

int countSStar(int rels[N][N], int x) {
	int result[N];
 	sStar(rels, x, result);
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(result[i] == -1)
			break;
		count++;
	}
	return count;
}

void pPlus(int rels[N][N], int x, int result[N]) {
	int curr[N];
	int processed[N];
	for(int i = 0; i < N; i++) {
		result[i] = -1;
		curr[i] = -1;
		processed[i] = -1;
	}
	curr[0] = x;
	int count = 0;
	int processedCount = 0;
	while(count != -1) {
		int temp[N];
		p(rels, curr[0], temp);
		processed[processedCount] = curr[0];
		processedCount++;
		for(int i = 0; i < N - 1; i++) {
			curr[i] = curr[i + 1];
		}
		count--;	
		if(temp[0] != -1) {
			merge(result, temp, result);
			for(int i = 0; i < N; i++) {
				if(temp[i] != -1 && !isPresent(processed, temp[i])) {
					count++;
					curr[count] = temp[i];
				}				
			}
		}
	}
}

int countPPlus(int rels[N][N], int x) {
	int result[N];
 	pPlus(rels, x, result);
	int count = 0;
	for(int i = 0; i < N; i++) {
		if(result[i] == -1)
			break;
		count++;
	}
	return count;
}

bool SLLspec(int edges[N][N]) {
	/*root r via N*/
	int viaR[N];
   	sStar(edges, 0, viaR);
		for(int j = 0; j < N; j++) {
			cout << viaR[j] << ", ";
		}
		cout << endl;
	for(int i = 0; i < N; i++) {
		if(!isPresent(viaR, i))
			return false;
	}
	/*No self-loops*/
	for(int i = 0; i < N; i++) {
		int temp[N];
		s(edges, i, temp);
//		cout << "s(" << i << ") ";
//		for(int j = 0; j < N; j++) {
//			cout << temp[j] << ", ";
//		}
//		cout << endl;
		if(isPresent(temp, i))
			return false;	
	}
	/*1:1 N*/
	for(int i = 0; i < N; i++) {
		int sum = 0;
		for(int j = 0; j < N; j++) {
			sum += edges[j][i];
		}
		if(sum > 1)
			return false;
	}
	/*Functional N*/
	for(int i = 0; i < N; i++) {
		int sum = 0;
		for(int j = 0; j < N; j++) {
			sum += edges[i][j];
		}
		if(sum > 1)
			return false;
	}
	/*\forall u(\lnot N^+(u, u))*/
	for(int i = 0; i < N; i++) {
		int temp[N];
		sPlus(edges, i, temp);
		if(isPresent(temp, i))
			return false;
	}
	return true;
}

bool SLLsketch(int edges[N][N]) {
	/*root r via N*/
	int viaR[N];
   	sStar(edges, 0, viaR);
	for(int i = 0; i < N; i++) {
		if(!isPresent(viaR, i))
			return false;
	}
	/*Functional N*/
	for(int i = 0; i < N; i++) {
		int sum = 0;
		for(int j = 0; j < N; j++) {
			sum += edges[i][j];
		}
		if(sum > 1)
			return false;
	}
	if(countP(edges, 0) != 0)
		return false;
	for(int i = 1; i < N; i++) {
		if(countP(edges, i) > 1)
			return false;
	}
	return true;
}

int main() {
	srand(time(NULL));
	int edges[N][N];
	edges[0] = {1, 1, 1};
	edges[1] = {0, 0, 1};
	edges[2] = {0, 1, 0};
	for(int i = 0; i < N; i++) {
		for(int j =0; j < N; j++) {
//			edges[i][j] = rand()%2;
//			if(i == j) {
//				edges[i][j] = 0;
//			}
			cout << edges[i][j] << ",";
		}
		cout << endl;
	}
	cout << "result: " << SLLspec(edges) << endl;
	cout << "result: " << SLLsketch(edges) << endl;
	return 0;
}
