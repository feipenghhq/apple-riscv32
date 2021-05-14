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




#define ENABLE_BRANCH_COUNT     1


extern void trap_entry();

void _init() {

    // init the uart with default configuration
    _uart_init(UART0_BASE);

    // write the trap handler register
    uint32_t trap_entry_addr = (uint32_t) &trap_entry;
    _write_csr(mtvec, trap_entry_addr);

    // enable global interrupt (mstatus)
    _write_csr(mstatus, 0x8);
    // enable interrupt (mie)
    _write_csr(mie, 0x888);

    #ifdef ENABLE_BRANCH_COUNT
    clr_en_br_cnt();
    #endif
}
