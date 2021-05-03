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
// A very basic FPGA board demo - Control the LED on the FPGA board.
// The program use 4 button on FPGA to control the LED
//
// Assuming GPIO0 Bit 0~3 is connected to LED
// Assuming GPIO0 Bit 4~7 is connected to button or switch
//
// Demostrate GPIO read/write function.
//
// To compile the program and generate verilog memory dump
//
// cd AppleRISCV/sdk/software
// make make dasm PROGRAM=demo_gpio0
//
// To upload the verilog file to instruction ram  through uart:
//
// cd AppleRISCV/sdk/tool
// sudo ./uart_download.py ../software/demo_gpio0/demo_gpio0.verilog
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