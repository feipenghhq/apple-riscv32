///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/20/2021
//
// ================== Description ==================
//
// Test the memory
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "peripherals.h"
#include "platform.h"

int main(int argc, char **argv)
{
    const int K    = 1024;
    const int SIZE = 8 * K;
    char      err  = 0;
    uint32_t  value = 0xFFFFFFFF;
    uint32_t  mem[SIZE];
    //uint32_t* mem;
    //mem = malloc(sizeof(uint32_t) * SIZE);

    printf("Starting Memory Test\n");
    printf("Writting %d number of data into memory\n", SIZE);
    printf("Memory Start Location %x\n", (uint32_t) mem);

    // Write the data into memory
    for (int i = 0; i < SIZE; i++) {
        mem[i] = value;
        value--;
    }

    // Read the data and check result
    value = 0xFFFFFFFF;
    for (int i = 0; i < SIZE; i++) {
        if (mem[i] != value) {
            err = 1;
            printf("Wrong memory data at location %d, Expected: %x, Actual %x\n", i, value, mem[i]);
        }
        value--;
    }

    // Check the result
    if (err == 0) {
        printf("TEST PASSED\n");
    } else {
        printf("TEST FAILED\n");
    }

    return 0;
}