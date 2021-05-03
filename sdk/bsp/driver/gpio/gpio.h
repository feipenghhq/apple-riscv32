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


#ifndef _GPIO_H_
#define _GPIO_H_

#include <io.h>

#define GPIO_REG_WRITE              0x0
#define GPIO_REG_READ               0x0
#define GPIO_REG_WRITEENABLE        0x4

#define gpio_enable_all(base)       IOWR(base, GPIO_REG_WRITEENABLE, 0xFFFFFFFF)
#define gpio_enable(base, mask)     IOWR(base, GPIO_REG_WRITEENABLE, mask)
#define gpio_write(base, data)      IOWR(base, GPIO_REG_WRITE, data)
#define gpio_read(base)             IORD(base, GPIO_REG_READ)

#endif