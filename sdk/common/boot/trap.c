
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "platform.h"
#include "peripherals.h"
#include "sysutils.h"

#define MCAUSE_MASK             0x7FFFFFFF
#define MCAUSE_LD_ADDR_MISALIGN 0x4

#define INTERRUPT_MASK          (0x1 << 31)
#define M_SOFTWARE              3
#define M_TIMER                 7
#define M_EXTERNAL              11

#define get_instruction(pc) ((uint32_t) *((uint32_t *) pc))

// ================================
// Exception Handler Routine
// ================================
void __attribute__((weak)) exit_trap(uint32_t mepc, uint32_t mcause) {
    printf("Exception: mcause = %x\n", mcause);
    printf("PC = %x\n", mepc);
    printf("Instruction = %x\n", get_instruction(mepc));
    printf("mtval = %x\n", _read_csr(mtval));
    exit(1);
}

void exception_handler(uint32_t mepc, uint32_t mcause) {
    exit_trap(mepc, mcause);
}

// ================================
// Interrupt Handler Routine
// ================================
void __attribute__((weak)) m_timer_interrupt_handler() {
    // clear mtime
    _set_mtimehi(CLIC_BASE, 0);
    _set_mtimelo(CLIC_BASE, 0);
}

void __attribute__((weak)) m_software_interrupt_handler() {
    // clear msip
    _clr_msip(CLIC_BASE);
}

void __attribute__((weak)) m_external_interrupt_handler() {}

void interrupt_handler(uint32_t mcause) {
    switch(mcause & MCAUSE_MASK) {
        case M_SOFTWARE: {
            m_software_interrupt_handler();
            break;
        }
        case M_TIMER: {
            m_timer_interrupt_handler();
            break;
        }
        case M_EXTERNAL: {
            m_external_interrupt_handler();
            break;
        }
    }
}

uint32_t trap_handler(uint32_t mcause, uint32_t mepc) {
    if ((mcause & INTERRUPT_MASK) != 0) {
        interrupt_handler(mcause);
    } else {
        exception_handler(mepc, mcause);
    }
    return mepc;
}