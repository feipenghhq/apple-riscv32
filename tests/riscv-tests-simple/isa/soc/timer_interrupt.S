# See LICENSE for license details.

#*****************************************************************************
# timer_interrupt.S
#-----------------------------------------------------------------------------
#
# Test software interrupt behavior.
#

#include "riscv_test.h"
#include "test_macros.h"

RVTEST_CODE_BEGIN

  .align 2
  .equ MTIMECMP0_ADDR, 0x02004000

  # set the mtvec to interrupt handler address
  la t0, mtvec_handler
  slli t0, t0, 2 # left shift t bit to add the mode field
  csrw	mtvec,t0

  # enable global interrupt
  ori  t0, x0, 0x8
  csrs mstatus, t0

  # enable timer interrupt
  ori  t0, x0, 0x80
  csrs mie, t0

  # setup to mtcmp register to trigger software interrupt
  ori  t1, x0, 100
  li s0, MTIMECMP0_ADDR
  sw t1, 0(s0)

  # Wait for the timer to fire interrupt
  ori  t2, x0, 0
Wait:
  beq  t2, x0, Wait


  # check if the interrupt has changed register value
  ori t1, x0, 0xf
  beq a0, t1, pass

  j fail

TEST_PASSFAIL

  .global mtvec_handler
  mtvec_handler:
    # check mip
    ori t1, x0, 0x80
    csrr t0, mip
    bne t0, t1, fail
    # check mcause
    csrr t0, mcause
    li t1, 0x80000007
    bne t0, t1, fail
    li s0, MTIMECMP0_ADDR
    sw x0, 0(s0)  # clear the interrupt by setting mtimecmp to 0
    ori t2, x0, 1 # set t2 to none zero to break the wait loop
    li a0, 0xf
    mret

RVTEST_CODE_END

  .data
RVTEST_DATA_BEGIN

  TEST_DATA

RVTEST_DATA_END
