# Test if we can access Instruction RAM using dbus

#include "riscv_test.h"

RVTEST_CODE_BEGIN

# Load the Instruction RAM address

li a0, 0x20000000;

lw t0, 0(a0);
li t1, 0x00000093;
bne t0, t1, FAIL;

lw t0, 4(a0);
li t1, 0x00000113;
bne t0, t1, FAIL;

lw t0, 8(a0);
li t1, 0x00000193;
bne t0, t1, FAIL;

lw t0, 12(a0);
li t1, 0x00000213;
bne t0, t1, FAIL;

PASS:
    RVTEST_PASS

FAIL:
    RVTEST_FAIL

RVTEST_CODE_END