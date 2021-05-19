///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/01/2021
//
// ================== Description ==================
//
// Defining common routines to read/write GPIO
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __GPIO_H__
#define __GPIO_H__

#include "sysutils.h"

#define GPIO_VAL                    0x000
//#define GPIO_IEN                    0x004
#define GPIO_OEN                    0x008
#define GPIO_PRT                    0x00C
#define GPIO_RIE                    0x018
#define GPIO_RIP                    0x01C
#define GPIO_FIE                    0x018
#define GPIO_FIP                    0x01C
#define GPIO_HIE                    0x020
#define GPIO_HIP                    0x024
#define GPIO_LIE                    0x028
#define GPIO_LIP                    0x02C
#define GPIO_XOR                    0x040

#define gpio_en(base, mask)         IOWR(base, GPIO_OEN, mask)
#define gpio_en_all(base)           gpio_en(base, 0xFFFFFFFF)

#define gpio_wr(base, data)         IOWR(base, GPIO_PRT, data)
#define gpio_rd(base)               IORD(base, GPIO_VAL)

#endif