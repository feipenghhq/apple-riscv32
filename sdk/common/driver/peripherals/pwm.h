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
// Defining common routines for PWM
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __PWM_H__
#define __PWM_H__

#define PWM_CFG         0x00
#define PWM_COUNT       0x08
#define PWM_MS          0x10
#define PWM_CMP0        0x20
#define PWM_CMP1        0x24
#define PWM_CMP2        0x28
#define PWM_CMP3        0x2C

#define _pwm_set_cfg(base, data) \
    IOWR(base, PWM_CFG, data)

#define _pwm_set_cmp0(base, data) \
    IOWR(base, PWM_CMP0, data)

#define _pwm_set_cmp1(base, data) \
    IOWR(base, PWM_CMP1, data)

#define _pwm_set_cmp2(base, data) \
    IOWR(base, PWM_CMP2, data)

#define _pwm_set_cmp3(base, data) \
    IOWR(base, PWM_CMP3, data)

#define _pwm_clr_cnt(base) \
    IOWR(base, PWM_COUNT, 0)


#endif