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
#include <sys/times.h>
#include "system.h"

int comparator(const void *p, const void *q) {
    return (*(int*) p) - (*(int*) q);
}

int main(void) {

    clock_t start, end;
    clock_t cpu_time_used;

    long long sum = 0;
    const int ArraySize = 8000;
    const int loopSize = 4000;
    const int CLOCKS_PER_SEC = 1000 * 1000 * 100 / 1024;
    //int *L = malloc(sizeof(int) * ArraySize);
    int L[ArraySize];

    printf("************************\n");
    printf("CLOCKS_PER_SEC=%d\n",CLOCKS_PER_SEC);
    printf(" == Prepare Data == \n");
    start = clock();
    printf("Start clock    =    %ld\n", start);

    for (unsigned i = 0; i < ArraySize; i++) {
        L[i] = rand() % 256;
    }

    printf(" == Sort Data == \n");
    start = clock();
    printf("Start clock    =    %ld\n", start);

    //qsort(L, ArraySize, sizeof(int), comparator);

    printf(" == Test Started == \n");
    start = clock();
    printf("Start clock    =    %ld\n", start);

    for (unsigned i = 0; i < loopSize; ++i)
    {
        for (unsigned i = 0; i < ArraySize; i++) {
            if (L[i] > 128) {
                sum += L[i];
            }
        }
    }
    stp_br_cnt();
    // end
    printf(" == Test End == \n");
    end = clock();
    cpu_time_used = ((end - start)) / CLOCKS_PER_SEC;
    printf("End clock = %ld\n", end);

    printf(" == Test Result == \n");
    printf("Result =    %l\n", sum);
    printf("Time   =    %d\n", cpu_time_used);
    printf("************************\n");

    for (int i = 0; i < 10; i++) {
        printf("%d ", L[i]);
    }

    printf("\n");
    printf("************************\n");
    printf("Branch Count %u + %u\n", rdmhpmcounter3h(), rdmhpmcounter3());
    printf("Good Predict Count %u + %u\n", rdmhpmcounter4h(), rdmhpmcounter4());
    printf("************************\n");
    return 0;
}