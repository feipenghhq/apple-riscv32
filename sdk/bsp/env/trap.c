
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "utils.h"

//#define get_opcode(instr)   (instr & 0x7F)
//#define get_func3(instr)    ((instr >> 12) & 0x7)
//#define get_rd(instr)       ((instr >> 6) & 0x1F)
//#define get_rs1(instr)      ((instr >> 15) & 0x1F)
//#define get_i_imm(instr)    ((instr >> 20) & 0xFFF)
#define get_instruction(pc) ((uint32_t) *((uint32_t *) pc))

void handle_ld_addr_misalign(uintptr_t mepc) {
    printf("Exception cause Load address misaligned\n");
    printf("Instruction pc     = %x\n", mepc);
    printf("Instruction        = %x\n", get_instruction(mepc));
    printf("Misaligned address = %x\n", rdmtval());
    exit(1);
}

void exit_trap(uintptr_t mepc, uintptr_t mcause) {
    printf("Error Hit an exception: mcause = %x\n", mcause);
    printf("Instruction pc     = %x\n", mepc);
    printf("Instruction        = %x\n", get_instruction(mepc));
    exit(1);
}