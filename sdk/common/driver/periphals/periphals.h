///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 05/11/2021
//
// ================== Description ==================
//
// Periphals header file
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __PERIPHALS_H__
#define __PERIPHALS_H__

#include "gpio.h"
#include "stdint.h"

void uart_init(uint32_t base);
char uart_putc(uint32_t base, char c);
void uart_puts(uint32_t base, char *s);
void uart_putnc(uint32_t base, char *buf, size_t nbytes);
char uart_getc(uint32_t base);

#endif