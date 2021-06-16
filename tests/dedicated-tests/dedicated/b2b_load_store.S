# Test back to back load and store

#include "riscv_test.h"

RVTEST_CODE_BEGIN

# Load the Instruction RAM address

li a0, 0x1
li a1, 0x2
li a2, 0x3
li a3, 0x4

li sp, 0x80000000;

sw a0, 0(sp);
sw a1, 4(sp);
sw a2, 8(sp);
sw a3, 12(sp);

lw t0, 0(sp);
lw t1, 4(sp);
lw t2, 8(sp);
lw t3, 12(sp);

bne t0, a0, FAIL;
bne t1, a1, FAIL;
bne t2, a2, FAIL;
bne t3, a3, FAIL;

PASS:
    RVTEST_PASS

FAIL:
    RVTEST_FAIL

RVTEST_CODE_END