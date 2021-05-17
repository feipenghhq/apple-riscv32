///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 05/11/2021
//
// ================== Description ==================
//
// Platform specific variable
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __PLATFORM_H__
#define __PLATFORM_H__

#include "board.h"

// SOC component address mapping
#define CLIC_BASE       0x02000000
#define PLIC_BASE       0x0C000000
#define AON_BASE        0x10000000
#define RTC_BASE        AON_BASE

#define GPIO_BASE       0x10012000
#define UART0_BASE      0x10013000
#define PWM0_BASE       0x10015000

#endif