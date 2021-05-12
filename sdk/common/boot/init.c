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

#include "platform.h"
#include "periphals.h"
#include "sysutils.h"

#define MCAUSE_MASK             0xFFFFFFFF
#define MCAUSE_LD_ADDR_MISALIGN 0x4

#define ENABLE_BRANCH_COUNT     1

extern void trap_entry();

void _init() {

    // init the uart with default configuration
    uart_init(UART0_BASE);

    // write the trap handler register
    uint32_t trap_entry_addr = (uint32_t) &trap_entry;
    trap_entry_addr = trap_entry_addr << 2;
    write_csr(mtval, trap_entry_addr);

    #ifdef ENABLE_BRANCH_COUNT
    clr_en_br_cnt();
    #endif
}

extern void handle_ld_addr_misalign(uintptr_t mepc);
extern void exit_trap(uintptr_t mepc, uintptr_t mcause);

uintptr_t handle_trap(uintptr_t mcause, uintptr_t mepc) {

    // Load Address Misaligned
    if((mcause & MCAUSE_MASK ) == MCAUSE_LD_ADDR_MISALIGN) {
        handle_ld_addr_misalign(mepc);
    } else {
        exit_trap(mepc, mcause);
    }
    return mepc;
}