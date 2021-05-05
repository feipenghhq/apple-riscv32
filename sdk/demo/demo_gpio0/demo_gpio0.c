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
// A very basic FPGA board demo - Control the LED on the FPGA board through push button.
// The program use 4 button/switch on FPGA to control the 4 LEDs
//
// GPIO0 Bit 0~3 should be connected to LED
// GPIO0 Bit 4~7 should be connected to button or switch
//
// Demostrate GPIO read/write function.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <gpio.h>
#include <soc.h>

#define btn_read(base) ((gpio_read(base) >> 4) & 0xF)

int main(int argc, char **argv)
{

    volatile uint32_t   value;
    gpio_enable(GPIO0_BASE, 0xF);
    while(1) {
        value = btn_read(GPIO0_BASE);
        gpio_write(GPIO0_BASE, value);
    }
    return 0;
}