# See LICENSE for license details.

#*****************************************************************************
# software_interrupt.S
#-----------------------------------------------------------------------------
#
# Test software interrupt behavior.
#

#include "riscv_test.h"
#include "test_macros.h"

RVTEST_CODE_BEGIN

  .align 2
  .equ MSIP_ADDR, 0x02000000

  # set the mtvec to interrupt handler address
  la t0, mtvec_handler
  #slli t0, t0, 2 # left shift t bit to add the mode field
  csrw	mtvec,t0

  # enable global interrupt
  ori  t0, x0, 0x8
  csrs mstatus, t0

  # enable software interrupt
  ori  t0, x0, 0x8
  csrs mie, t0

  # write 1 to msip to trigger software interrupt
  li s0, MSIP_ADDR
  ori t1, x0, 0x1
  ori a0, x0, 0xa
  sw t1, 0(s0)
  # few nop instructions


  # check if the interrupt has changed register value
  ori t1, x0, 0xf
  beq a0, t1, pass

  j fail

TEST_PASSFAIL

  .global mtvec_handler
  mtvec_handler:
    # check mip
    ori t1, x0, 0x8
    csrr t0, mip
    bne t0, t1, fail
    # check mcause
    csrr t0, mcause
    li t1, 0x80000003
    bne t0, t1, fail
    sw x0, 0(s0)  # clear the interrupt
    li a0, 0xf
    csrr t0, mepc
    #addi t0, t0, 4  # advance pc to pc + 4
    csrw mepc, t0
    mret

RVTEST_CODE_END

  .data
RVTEST_DATA_BEGIN

  TEST_DATA

RVTEST_DATA_END
