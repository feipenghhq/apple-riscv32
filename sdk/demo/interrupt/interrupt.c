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
#include "sysutils.h"

struct time_s {
    uint32_t s;
    uint32_t m;
};

static struct time_s time = {0, 0};

void plus_one_second(struct time_s *t) {
    t->s += 1;
    if (t->s == 60) {
        t->s = 0;
        t->m += 1;
    }
}

void m_timer_interrupt_handler() {
    // clear mtime
    _set_mtimehi(CLIC_BASE, 0);
    _set_mtimelo(CLIC_BASE, 0);
    printf("MTI\n");
}

void m_software_interrupt_handler() {
    // clear msip
    _clr_msip(CLIC_BASE);
    printf("MSI\n");
}

void uart0_interrupt_handler() {
    char buf[20];
    scanf("%s\n", buf);
    printf("Gets something from uart: %s\n", buf);
}

void gpio_interrupt_handler() {
    uint32_t gpio_ip;
    if (gpio_ip = IORD(GPIO_BASE, GPIO_RIP)) {
        IOWR(GPIO_BASE, GPIO_RIP, 0); // clear interrupt
        printf("Get GPIO Rising Interrupt: %x\n", gpio_ip);
    }
}

void rtc_interrupt_handler() {
    _rtc_clr_hi(RTC_BASE);
    _rtc_clr_lo(RTC_BASE);
    plus_one_second(&time);
    printf("The program has been running for: %02d:%02d\n", time.m, time.s);
}


void m_external_interrupt_handler() {
    printf("MEI\n");
    uint32_t pending0, pending1;
    pending0 = IORD(PLIC_BASE, PLIC_PENDING1);
    pending1 = IORD(PLIC_BASE, PLIC_PENDING2);
    if (pending0) {
        if (pending0 & PLIC_UART0_MASK) {uart0_interrupt_handler();}
        if (pending0 & PLIC_GPIO_MASK)  {gpio_interrupt_handler();}
        if (pending0 & PLIC_RTC_MASK) {rtc_interrupt_handler();}
    }
}


void rtc_setup() {
    _rtc_clr_lo(RTC_BASE);
    _rtc_clr_hi(RTC_BASE);
    _rtc_set_cmp(RTC_BASE, 1);
    // scale to increment once per second
    _rtc_set_cfg(RTC_BASE, (0xF | (0x1 << 12)));
}

int main(int argc, char **argv)
{
    const int DIV = 200000000;
    // enable rtc interrupt and initialize rtc
    rtc_setup(&time);
    _plic_rtc_int_en(PLIC_BASE);
    //printf("Test started at time %d\n", time);
    // Set CLIC timer cmp to get the timer interrupt
    _set_mtimecmphi(CLIC_BASE, 0x0);
    _set_mtimecmplo(CLIC_BASE, DIV);
    // enable uart rx interrupt and PLIC uart interrupt
    _uart_rxwm_en(UART0_BASE);
    _plic_uart0_int_en(PLIC_BASE);
    // enable GPIO4 interrupt and PLIC GPIO4 interrupt
    IOSET(GPIO_BASE, GPIO_RIE, 0xF0);
    _plic_gpioX_int_en(4, PLIC_BASE);
    _plic_gpioX_int_en(5, PLIC_BASE);
    _plic_gpioX_int_en(6, PLIC_BASE);
    _plic_gpioX_int_en(7, PLIC_BASE);


    while(1) {
        // trigger software interrupt
        _set_msip(CLIC_BASE);
        for (int i = 0; i < DIV; i++);
    }
    return 0;
}