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

#ifndef _UART_H_
#define _UART_H_

#include <io.h>

#define UART_CONTROL                        0x0
#define UART_CFG_FRAME                      0x4
#define UART_CLK_DIVIDER                    0x8
#define UART_TX_DATA                        0xC
#define UART_RX_DATA                        0xC
#define UART_STATUS                         0x10

// utility
#define set_bit(pos)                        (0x1 << pos)

// enable rx avail interrupt
#define uart_rx_avail_int_en(base)          IOSET(base, UART_CONTROL, set_bit(0))
// enable rx full interrupt
#define uart_rx_full_int_en(base)           IOSET(base, UART_CONTROL, set_bit(1))
// enable rx path
#define uart_rx_en(base)                    IOSET(base, UART_CONTROL, set_bit(4))
// enable tx path
#define uart_tx_en(base)                    IOSET(base, UART_CONTROL, set_bit(5))
// enable both tx and rx path
#define uart_en(base)                       IOSET(base, UART_CONTROL, set_bit(5) | set_bit(4))

// Set frame and colck divider
#define uart_set_frame(base, data)          IOWR(base, UART_CFG_FRAME, data)
#define uart_set_clkdiv(base, data)         IOWR(base, UART_CLK_DIVIDER, data)

// Check status
#define uart_rx_avail(base)                 ((IORD(base, UART_STATUS) & 0x1)
#define uart_rx_fulll(base)                 ((IORD(base, UART_STATUS) >> 1) & 0x1)
#define uart_tx_full(base)                  ((IORD(base, UART_STATUS) >> 4) & 0x1)
#define uart_rx_empty(base)                 ((IORD(base, UART_STATUS) >> 5) & 0x1)

// send/receive
#define uart_send_byte_raw(base, data)      IOWR(base, UART_TX_DATA, data)
#define uart_get_byte_raw(base)             (IORD(base, UART_RX_DATA) & 0xFF)

//
// Function prototype
//

/**
 * Predefined uart setup for apple riscv
 * - Disable All the interrupt
 * - Enable both RX and TX path
 * - Xfer size:  8 bits
 * - Stop bit:   1 bit
 * - Parity bit: None
 * - Baudrate:   115200
 */
void uart_setup_appleriscv(uint32_t base);

/**
 * Send a byte data through uart tx.
 * This will block if the uart tx buffer is full
 */
void uart_send_byte(uint32_t base, char data);

/**
 * Send a string through uart tx.
 * This will block if the uart tx buffer is full
 */
void uart_send_string(uint32_t base, char* s);

#endif