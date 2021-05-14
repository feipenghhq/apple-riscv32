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
// Interrupt Demo
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdio.h>

#include "platform.h"
#include "periphals.h"

void m_timer_interrupt_handler() {
    // clear mtime
    set_mtimehi(CLIC_BASE, 0);
    set_mtimelo(CLIC_BASE, 0);
    printf("MTI\n");
}

void m_software_interrupt_handler() {
    // clear msip
    clr_msip(CLIC_BASE);
    printf("MSI\n");
}

int main(int argc, char **argv)
{

    const int DIV = 100000000;
    printf("Test started\n");
    // Set CLIC timer cmp to get the timer interrupt
    set_mtimecmphi(CLIC_BASE, 0x0);
    set_mtimecmplo(CLIC_BASE, 20000000);
    while(1) {
        printf("----\n");
        // trigger software interrupt
        set_msip(CLIC_BASE);
        for (int i = 0; i < DIV; i++);
    }
    return 0;
}