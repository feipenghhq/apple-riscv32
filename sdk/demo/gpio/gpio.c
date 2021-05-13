///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// GPIO demo with blink LED
//
// The 4 switch is used to control the blink speed.
// When the push button is pressed, the LED will be lighted instead of blinking
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include "platform.h"
#include "gpio.h"

int main(int argc, char **argv)
{

    volatile uint32_t   value;
    uint8_t             ctrl = 0xF;
    uint8_t             btn, sw;
    uint32_t            interval;

    interval = (1 << 20);
    gpio_en(GPIO0_BASE, 0xF);

    while(1) {
        value = gpio_rd(GPIO0_BASE);
        btn = (value >> 4) & 0xF;
        sw  = ((value >> 8) & 0xF) + 1;
        ctrl = ~ctrl;
        for (int i = 0; i < (interval * sw); i++);
        gpio_wr(GPIO0_BASE, ctrl | btn);
    }
    return 0;
}