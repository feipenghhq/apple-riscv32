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
// A very basic FPGA board demo - Flusing LED on the FPGA board.
// This program will blink 4 LEDs on the FPGA board.
//
// GPIO0 Bit 0~3 should be connected to LED
//
// Demostrate GPIO write function.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <gpio.h>
#include <soc.h>

int main(int argc, char **argv)
{
    int                 i;
    volatile uint32_t   value = 0;

    gpio_enable_all(GPIO0_BASE);
    while(1) {
        gpio_write(GPIO0_BASE, value);
        value = value + 1;
        for (i = 0; i < 10000000; i++);
    }
    return 0;
}