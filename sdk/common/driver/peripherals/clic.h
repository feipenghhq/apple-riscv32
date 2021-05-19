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

#define _set_msip(base)     IOWR(base, CLIC_MSIP, 0x1)
#define _clr_msip(base)     IOWR(base, CLIC_MSIP, 0x0)

#define _set_mtimecmplo(base, value) \
    IOWR(base, CLIC_MTIMECMPLO, value)

#define _set_mtimecmphi(base, value) \
    IOWR(base, CLIC_MTIMECMPHI, value)

#define _set_mtimelo(base, value) \
    IOWR(base, CLIC_MTIMELO, value)

#define _set_mtimehi(base, value) \
    IOWR(base, CLIC_MTIMEHI, value)

#endif