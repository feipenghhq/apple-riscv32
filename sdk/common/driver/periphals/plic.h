///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/14/2021
//
// ================== Description ==================
//
// Defining common routines for PLIC
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __PLIC_H__
#define __PLIC_H__

#include "sysutils.h"

#define PLIC_PENDING1       0x1000
#define PLIC_PENDING2       0x1004
#define PLIC_ENABLE1        0x2000
#define PLIC_ENABLE2        0x2004

#define PLIC_UART0_MASK     (0x1 << 3)
#define PLIC_RTC_MASK       (0x1 << 2)

// FIXME: There is bug here. GPIO upper bit goes to ENABLE2 register
#define PLIC_GPIOX_MASK(x)  (0x1 << (8+x))
#define PLIC_GPIO_MASK      (0xFFFFFF00)

#define _plic_int_pending(base, offset, mask) \
    ((_plic_read_reg(base, offset) & mask) != 0)

#define _plic_rtc_int_en(plic_base)             IOSET  (plic_base, PLIC_ENABLE1, PLIC_RTC_MASK)
#define _plic_rtc_int_dis(plic_base)            IOCLEAR(plic_base, PLIC_ENABLE1, PLIC_RTC_MASK)

#define _plic_uart0_int_en(plic_base)           IOSET  (plic_base, PLIC_ENABLE1, PLIC_UART0_MASK)
#define _plic_uart0_int_dis(plic_base)          IOCLEAR(plic_base, PLIC_ENABLE1, PLIC_UART0_MASK)

#define _plic_gpioX_int_en(x, plic_base)        IOSET  (plic_base, PLIC_ENABLE1, PLIC_GPIOX_MASK(x))
#define _plic_gpioX_int_dis(x, plic_base)       IOCLEAR(plic_base, PLIC_ENABLE1, PLIC_GPIOX_MASK(x))

#endif