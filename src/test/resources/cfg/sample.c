#include<stdio.h>
	int main(){
        int a, b, c, k;
	    while(k>0){
	        if(a>b){
            	printf("a is bigger than b");
            }
            else if(a==b){
                printf("a and b is equal");
            }
            else if(a>c) {
                printf("a is smaller than b");
            }
	        k--;
	    }
	    return 0;
	}