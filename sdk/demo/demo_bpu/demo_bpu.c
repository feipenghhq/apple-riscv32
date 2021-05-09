///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// Test Branch Prediction
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>


int comparator(const void *p, const void *q) {
    return (*(int*) p) - (*(int*) q);
}

int main(void) {

    clock_t start, end;
    clock_t cpu_time_used;

    long sum = 0;
    const int ArraySize = 800;
    const int loopSize = 100000;
    int *L = malloc(sizeof(int) * ArraySize);
    //int L[ArraySize];

    printf("************************\n");
    printf(" == Test Started == \n");
    start = clock();
    printf("Start clock    =    %ld\n", start);
    //printf("CLOCKS_PER_SEC =    %d\n", CLOCKS_PER_SEC);

    printf(" == Test Running == \n");
    for (unsigned i = 0; i < ArraySize; i++) {
        L[i] = rand() % 256;
    }

    // sort the array
    qsort(L, ArraySize, sizeof(int), comparator);

    for (unsigned i = 0; i < loopSize; ++i)
    {
        for (unsigned i = 0; i < ArraySize; i++) {
            if (L[i] > 128) {
                sum += L[i];
            }
        }
    }

    // end
    printf(" == Test End == \n");
    end = clock();
    cpu_time_used = ((end - start)) / (CLOCKS_PER_SEC / 10);
    printf("End clock = %ld\n", end);

    printf(" == Test Result == \n");
    printf("Result =    %ld\n", sum);
    printf("Time   =    %ld\n", cpu_time_used);
    printf("************************\n");

    for (int i = 0; i < 10; i++) {
        printf("%d ", L[i]);
    }
    printf("\n");
    return 0;
}
