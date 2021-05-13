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
// Blink LED on the FPGA board.
//
// This program will blink 4 LED on the FPGA board.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include "periphals.h"
#include "platform.h"

int main(int argc, char **argv)
{
    volatile uint32_t   value = 0;

    gpio_en(GPIO0_BASE, 0xF);
    while(1) {
        gpio_wr(GPIO0_BASE, value);
        value = value + 1;
        for (int i = 0; i < 10000000; i++);
    }
    return 0;
}