
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "platform.h"
#include "sysutils.h"

#define get_instruction(pc) ((uint32_t) *((uint32_t *) pc))

void handle_ld_addr_misalign(uintptr_t mepc) {
    printf("Exception cause Load address misaligned\n");
    printf("Instruction pc     = %x\n", mepc);
    printf("Instruction        = %x\n", get_instruction(mepc));
    printf("Misaligned address = %x\n", read_csr(mtval));
    exit(1);
}

void exit_trap(uintptr_t mepc, uintptr_t mcause) {
    printf("Error Hit an exception: mcause = %x\n", mcause);
    printf("Instruction pc     = %x\n", mepc);
    printf("Instruction        = %x\n", get_instruction(mepc));
    exit(1);
}