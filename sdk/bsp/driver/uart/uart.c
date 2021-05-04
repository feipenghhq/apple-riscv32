///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/03/2021
//
// ================== Description ==================
//
// Defining common routines to read/write Uart
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <uart.h>

/**
 * Predefined uart setup for apple riscv
 * - Disable All the interrupt
 * - Enable both RX and TX path
 * - Xfer size:  8 bits
 * - Stop bit:   1 bit
 * - Parity bit: None
 * - Baudrate:   115200
 */
void uart_setup_appleriscv(uint32_t base) {

    uart_en(base);

    const int data_length = 7;  // data length + 1 = length for each transfer
    const int stop_bit   = 0;   // stop bit:    0 - one bit, 1 - two bits.
    const int parity_bit = 0;   // parity bit:  0 - None,    1 - Even,     2 - Odd
    const int frame_cfg  = data_length | stop_bit << 3 | parity_bit << 4;
    uart_set_frame(base, frame_cfg);

    const int baudrate = 115200;
    const int clkdiv   = 50 * 1000000 / (data_length + 1) / baudrate;
    uart_set_clkdiv(base, clkdiv);
}

/**
 * Send a byte data through uart tx.
 * This will block if the uart tx buffer is full
 */
void uart_send_byte(uint32_t base, char data) {
    // wait till tx buffer has space
    while(uart_tx_full(base))
        ;
    uart_send_byte_raw(base, data);
}

/**
 * Send a string through uart tx.
 * This will block if the uart tx buffer is full
 */
void uart_send_string(uint32_t base, char* s) {

    while(*s != '\0') {
       uart_send_byte(base, *s);
       s++;
    }
}

/**
 * Send n bytes through uart tx.
 * This will block if the uart tx buffer is full
 */
void uart_send_nbyte(uint32_t base, char *buf, size_t nbytes) {

    while(nbytes > 0) {
       uart_send_byte(base, *buf);
       buf++;
       nbytes--;
    }
}