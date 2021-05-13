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
// Uart read and write test.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <stdio.h>

#include "periphals.h"
#include "sysutils.h"
#include "platform.h"

int main(int argc, char **argv)
{
    char b;
    char s[100];
    printf("Hello RISCV!\n");
    printf("Please enter something and I will echo back\n");
    while(1) {
        //b = uart_getc(UART0_BASE);
        //uart_putc(UART0_BASE, b);
        //b = getchar();
        //putchar(b);
        scanf("%s", s);
        puts(s);
    }
    return 0;
}