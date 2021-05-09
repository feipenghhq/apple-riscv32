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
// A very basic FPGA board demo - Send data to host machine console through uart port.
//
// This program will send "Hello AppleRISCV ~" to the host machine.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <stdio.h>

#include "soc.h"


void test_printf(void) {
    char string[]  = "Hello AppleRISCV ~\n";
    while(1) {
        printf("%s\n", string);
        for (int i = 0; i < 1000000; i++);
    }
}

int main(int argc, char **argv)
{
    test_printf();
    return 0;
}