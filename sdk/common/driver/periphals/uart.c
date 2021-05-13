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

#define UART_TXDATA                 0x000
#define UART_RXDATA                 0x004
#define UART_TXCTRL                 0x008
#define UART_RXCTRL                 0x00C
#define UART_IE                     0x010
#define UART_IP                     0x014
#define UART_DIV                    0x018

#define uart_tx_full(base)          ((IORD(base, UART_TXDATA) >> 31) & 0x1)
#define uart_tx_byte(base, data)    (IOWR(base, UART_TXDATA, data & 0xFF))

#define uart_rx(base)               (IORD(base, UART_RXDATA))
#define uart_rx_data(data)          (data & 0xFF)
#define uart_rx_valid(data)         ((data >> 31) & 0x1)

#define uart_tx_cfg(base, data)     (IOWR(base, UART_TXCTRL, data))
#define uart_rx_cfg(base, data)     (IOWR(base, UART_RXCTRL, data))

#define uart_tx_en(base)            (IOSET(base, UART_TXCTRL, 0x1))
#define uart_rx_en(base)            (IOSET(base, UART_RXCTRL, 0x1))

#define uart_txwm_en(base)          (IOSET(base, UART_IE, 0x1))
#define uart_rxwm_en(base)          (IOSET(base, UART_IE, 0x2))
#define uart_wm_en(base)            (IOSET(base, UART_IE, 0x3))

#define uart_txwm(base)             (IORD(base, UART_IP) & 0x1))
#define uart_rxwm(base)             ((IORD(base, UART_IP) >> 1) & 0x1))

#define uart_set_div(base, data)    (IOWR(base, UART_DIV, data))

#define BAUDRATE                    115200
#define SAMPLE                      8
//CLKDIV = clkFrequency / baudrate / g.rxSamplePerBit
#define CLKDIV                      (CLK_FEQ_MHZ * 1000000 / SAMPLE / BAUDRATE)

/**
 * Predefined uart setup for apple riscv
 * - Disable All the interrupt
 * - Enable both RX and TX path
 * - Stop bit:   1 bit
 * - Baudrate:   115200
 */
void uart_init(uint32_t base) {

    const uint32_t tx_cfg = 0x1 | (0x7 << 16);
    const uint32_t rx_cfg = 0x1 | (0x7 << 16);
    uart_tx_cfg(base, tx_cfg);
    uart_rx_cfg(base, rx_cfg);
    uart_set_div(base, CLKDIV);
}

/**
 * Send a byte data through uart tx.
 * This will block if the uart tx buffer is full
 */
char uart_putc(uint32_t base, char c) {
    while (uart_tx_full(base));
    return uart_tx_byte(base, c);
}

/**
 * Send a string through uart tx.
 * This will block if the uart tx buffer is full
 */
void uart_puts(uint32_t base, char *s) {
    while (*s != '\0') {
        uart_putc(base, *s++);
    }
}

/**
 * Send n bytes through uart tx.
 * This will block if the uart tx buffer is full
 */
void uart_putnc(uint32_t base, char *buf, size_t nbytes) {
    while (nbytes-- > 0) {
        uart_putc(base, *buf++);
    }
}

/**
 * Read a bytes from uart rx.
 * This will block if the uart tx buffer is full
 */
char uart_getc(uint32_t base) {
    uint32_t data;
    do {
        data = uart_rx(base);
    }
    while (!uart_rx_valid(data));
    return uart_rx_data(data);
}