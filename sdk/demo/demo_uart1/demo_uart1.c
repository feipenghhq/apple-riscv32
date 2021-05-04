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
// A very basic FPGA board demo - Send byte to host machine console through uart port.
//
// It will send latter a-z to the host machine console port
//
// cd AppleRISCV/sdk/tool
// sudo ./uart_download.py ../software/demo_uart0/demo_uart0.verilog
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <stdio.h>
#include <soc.h>


void test_printf(void) {
    char c = 'a';
    char string[]  = "Hello AppleRISCV ~\n";
    int i = 0;
    while(i < 5) {
        //putchar(c);
        printf("%s\n", string);
        for (int i = 0; i < 1000000; i++);
        i++;
    }
}

int main(int argc, char **argv)
{
    test_printf();
    return 0;
}