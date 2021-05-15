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
// PWM test
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <stdio.h>

#include "periphals.h"
#include "sysutils.h"
#include "platform.h"

void pwm_init() {
    uint32_t cfg = 0;
    const uint8_t scale     = 0x7;
    const uint8_t sticky    = 0;
    const uint8_t zerocmp   = 0;
    const uint8_t deglitch  = 1;
    const uint8_t enalways  = 1;
    const uint8_t center    = 0;
    const uint8_t gang      = 0;

    cfg = scale | sticky << 8 | zerocmp << 9 | deglitch << 10 | enalways << 12 | center << 16 | gang << 24;
    _pwm_set_cfg(PWM0_BASE, cfg);
    _pwm_set_cmp0(PWM0_BASE, 128);
    _pwm_set_cmp1(PWM0_BASE, 128);
    _pwm_set_cmp2(PWM0_BASE, 240);
    _pwm_set_cmp3(PWM0_BASE, 240);
    _pwm_clr_cnt(PWM0_BASE);
}

int main(int argc, char **argv)
{

    pwm_init();
    while(1) {
        ;
    }
    return 0;
}