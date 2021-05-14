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

#include <stdint.h>
#include <stddef.h>

#include "platform.h"
#include "sysutils.h"
#include "uart.h"



/**
 * Predefined uart setup for apple riscv
 * - Disable All the interrupt
 * - Enable both RX and TX path
 * - Stop bit:   1 bit
 * - Baudrate:   115200
 */
void _uart_init(uint32_t base) {
    const uint32_t tx_cfg = 0x1 | (0x7 << 16);
    const uint32_t rx_cfg = 0x1 | (0x6 << 16);
    _uart_tx_cfg(base, tx_cfg);
    _uart_rx_cfg(base, rx_cfg);
    _uart_set_div(base, _CLKDIV);
}

/**
 * Send a byte data through uart tx.
 * This will block if the uart tx buffer is full
 */
char _uart_putc(uint32_t base, char c) {
    while (_uart_tx_full(base));
    return _uart_tx_byte(base, c);
}

/**
 * Send a string through uart tx.
 * This will block if the uart tx buffer is full
 */
void _uart_puts(uint32_t base, char *s) {
    while (*s != '\0') {
        _uart_putc(base, *s++);
    }
}

/**
 * Send n bytes through uart tx.
 * This will block if the uart tx buffer is full
 */
void _uart_putnc(uint32_t base, char *buf, size_t nbytes) {
    while (nbytes-- > 0) {
        _uart_putc(base, *buf++);
    }
}

/**
 * Read a bytes from uart rx.
 * This will block if the uart tx buffer is full
 */
char _uart_getc(uint32_t base) {
    uint32_t data;
    do {
        data = _uart_rx(base);
    }
    while (!_uart_rx_valid(data));
    return _uart_rx_data(data);
}