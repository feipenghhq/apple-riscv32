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
#include <soc.h>


int main(int argc, char **argv)
{
    int b = 100;
    for (int a = 10; a >= 0; a--) {
        printf("%d * %d = %d\n", b, a, b * a);
        printf("%d / %d = %d\n", b, a, b / a);
        printf("%d mod %d = %d\n\n", b, a, b % a);
    }

    return 0;
}