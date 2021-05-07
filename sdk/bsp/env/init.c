///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// CPU init code - config and setup the peripherials
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stdint.h>
#include <stdio.h>

#include "soc.h"
#include "uart.h"
#include "utils.h"

#define MCAUSE_MASK             0xFFFFFFFF
#define MCAUSE_LD_ADDR_MISALIGN 0x4

extern int main(int argc, char** argv);
extern void trap_entry();

void _init() {

    // init the uart with default configuration
    uart_setup_appleriscv(UART_BASE);


    uint32_t trap_entry_addr = (uint32_t) &trap_entry;
    trap_entry_addr = trap_entry_addr << 2;
    write_csr(mtvec, trap_entry_addr);
}

__attribute__((weak)) void handle_ld_addr_misalign(uintptr_t mepc)  {};
__attribute__((weak)) void exit_trap(uintptr_t mepc, uintptr_t mcause)  {};

uintptr_t handle_trap(uintptr_t mcause, uintptr_t mepc) {

    // Load Address Misaligned
    if((mcause & MCAUSE_MASK ) == MCAUSE_LD_ADDR_MISALIGN) {
        //printf("Get Load Address Misaligned???\n");
        handle_ld_addr_misalign(mepc);
    } else {
        exit_trap(mepc, mcause);
    }
    return mepc;
}