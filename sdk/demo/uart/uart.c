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
    for (int i = 0; i < 10; i++) {
        printf("Hello RISCV!\n");
    }
    printf("Please enter something and I will echo back\n");
    while(1) {
        scanf("%s", s);
        puts(s);
    }
    return 0;
}