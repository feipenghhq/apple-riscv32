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
// Defining common routines for CLIC
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __CLIC_H__
#define __CLIC_H__

#define CLIC_MSIP           0x0000
#define CLIC_MTIMECMPLO     0x4000
#define CLIC_MTIMECMPHI     0x4004
#define CLIC_MTIMELO        0xBFF8
#define CLIC_MTIMEHI        0xBFFC

#define _clic_write_reg(base, offset, data) \
*((volatile uint32_t*) (base + offset)) = data

#define set_msip(base)      _clic_write_reg(base, CLIC_MSIP, 0x1)
#define clr_msip(base)      _clic_write_reg(base, CLIC_MSIP, 0x0)

#define set_mtimecmplo(base, value) \
_clic_write_reg(base, CLIC_MTIMECMPLO, value)

#define set_mtimecmphi(base, value) \
_clic_write_reg(base, CLIC_MTIMECMPHI, value)

#define set_mtimelo(base, value) \
_clic_write_reg(base, CLIC_MTIMELO, value)

#define set_mtimehi(base, value) \
_clic_write_reg(base, CLIC_MTIMEHI, value)

#endif