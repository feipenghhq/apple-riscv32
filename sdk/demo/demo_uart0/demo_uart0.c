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
#include <uart.h>
#include <soc.h>


/**
 * This test will send a-z through uart port to the console
 * it does not check if the uart tx buffer is full or not so
 * some character will be lost
 */
void test0(void) {
    int i;
    volatile char c = 'a';
    uart_setup_appleriscv(UART_BASE);
    while(1) {
        uart_send_byte_raw(UART_BASE, c);
        c = c + 1;
        if (c > 'z') c = 'a';
    }
}

/**
 * This test will send a-z through uart port to the console
 * it does check if the uart tx buffer is full or not
 */
void test1(void) {
    int i;
    volatile char c = 'a';
    uart_setup_appleriscv(UART_BASE);
    while(1) {
        uart_send_byte(UART_BASE, c);
        c = c + 1;
        if (c > 'z') c = 'a';
    }
}


void test2(void) {
    char c = 'a';
    char string[]  = "Hello AppleRISCV ~\n";

    uart_setup_appleriscv(UART_BASE);
    while(1) {
        uart_send_string(UART_BASE, string);
        while(c < 'a' + 26) {
            uart_send_byte(UART_BASE, c++);
        }
        c = 'a';
        uart_send_byte(UART_BASE, '\n');
        for (int i = 0; i < 1000000; i++);
    }
}

int main(int argc, char **argv)
{
    test2();
    return 0;
}