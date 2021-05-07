#ifndef _TRAP_ENTRY_
#define _TRAP_ENTRY_

#define REGBYTES 4
#define STORE    sw
#define LOAD     lw

//#include "bits.h"

.macro TRAP_ENTRY
    // adjust stack pointer, assign 32 REGBYTES to store register
    addi sp, sp, -32*REGBYTES

    // for simplicity just store everything
    STORE x1,   1*REGBYTES(sp)
    STORE x2,   2*REGBYTES(sp)
    STORE x3,   3*REGBYTES(sp)
    STORE x4,   4*REGBYTES(sp)
    STORE x5,   5*REGBYTES(sp)
    STORE x6,   6*REGBYTES(sp)
    STORE x7,   7*REGBYTES(sp)
    STORE x8,   8*REGBYTES(sp)
    STORE x9,   9*REGBYTES(sp)
    STORE x10,  10*REGBYTES(sp)
    STORE x11,  11*REGBYTES(sp)
    STORE x12,  12*REGBYTES(sp)
    STORE x13,  13*REGBYTES(sp)
    STORE x14,  14*REGBYTES(sp)
    STORE x15,  15*REGBYTES(sp)
    STORE x16,  16*REGBYTES(sp)
    STORE x17,  17*REGBYTES(sp)
    STORE x18,  18*REGBYTES(sp)
    STORE x19,  19*REGBYTES(sp)
    STORE x20,  20*REGBYTES(sp)
    STORE x21,  21*REGBYTES(sp)
    STORE x22,  22*REGBYTES(sp)
    STORE x23,  23*REGBYTES(sp)
    STORE x24,  24*REGBYTES(sp)
    STORE x25,  25*REGBYTES(sp)
    STORE x26,  26*REGBYTES(sp)
    STORE x27,  27*REGBYTES(sp)
    STORE x28,  28*REGBYTES(sp)
    STORE x29,  29*REGBYTES(sp)
    STORE x30,  30*REGBYTES(sp)
    STORE x31,  31*REGBYTES(sp)
.endm

.macro TRAP_EXIT
    // restore everything back
    LOAD x1,   1*REGBYTES(sp)
    LOAD x2,   2*REGBYTES(sp)
    LOAD x3,   3*REGBYTES(sp)
    LOAD x4,   4*REGBYTES(sp)
    LOAD x5,   5*REGBYTES(sp)
    LOAD x6,   6*REGBYTES(sp)
    LOAD x7,   7*REGBYTES(sp)
    LOAD x8,   8*REGBYTES(sp)
    LOAD x9,   9*REGBYTES(sp)
    LOAD x10,  10*REGBYTES(sp)
    LOAD x11,  11*REGBYTES(sp)
    LOAD x12,  12*REGBYTES(sp)
    LOAD x13,  13*REGBYTES(sp)
    LOAD x14,  14*REGBYTES(sp)
    LOAD x15,  15*REGBYTES(sp)
    LOAD x16,  16*REGBYTES(sp)
    LOAD x17,  17*REGBYTES(sp)
    LOAD x18,  18*REGBYTES(sp)
    LOAD x19,  19*REGBYTES(sp)
    LOAD x20,  20*REGBYTES(sp)
    LOAD x21,  21*REGBYTES(sp)
    LOAD x22,  22*REGBYTES(sp)
    LOAD x23,  23*REGBYTES(sp)
    LOAD x24,  24*REGBYTES(sp)
    LOAD x25,  25*REGBYTES(sp)
    LOAD x26,  26*REGBYTES(sp)
    LOAD x27,  27*REGBYTES(sp)
    LOAD x28,  28*REGBYTES(sp)
    LOAD x29,  29*REGBYTES(sp)
    LOAD x30,  30*REGBYTES(sp)
    LOAD x31,  31*REGBYTES(sp)

    // adjust stack pointer back
    addi sp, sp, 32*REGBYTES

    // return from trap handler
    mret
.endm

.section .text.entry
.align  2
.global trap_entry
.weak trap_entry

trap_entry:
    TRAP_ENTRY

    csrr a0, mcause
    csrr a1, mepc
    call handle_trap
    csrw mepc, a0

    TRAP_EXIT

.weak handle_trap
handle_trap:
1:
    j 1b

#endif