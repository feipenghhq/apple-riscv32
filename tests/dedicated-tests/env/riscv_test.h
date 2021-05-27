
#ifndef _RISCV_TEST_H_
#define _RISCV_TEST_H_



// 32 bits cpu
#define __riscv_xlen 32

// enable machine mode
#define __MACHINE_MODE

//-----------------------------------------------------------------------
// Begin Macro
//-----------------------------------------------------------------------


#define INIT_XREG                                                       \
  li x1, 0;                                                             \
  li x2, 0;                                                             \
  li x3, 0;                                                             \
  li x4, 0;                                                             \
  li x5, 0;                                                             \
  li x6, 0;                                                             \
  li x7, 0;                                                             \
  li x8, 0;                                                             \
  li x9, 0;                                                             \
  li x10, 0;                                                            \
  li x11, 0;                                                            \
  li x12, 0;                                                            \
  li x13, 0;                                                            \
  li x14, 0;                                                            \
  li x15, 0;                                                            \
  li x16, 0;                                                            \
  li x17, 0;                                                            \
  li x18, 0;                                                            \
  li x19, 0;                                                            \
  li x20, 0;                                                            \
  li x21, 0;                                                            \
  li x22, 0;                                                            \
  li x23, 0;                                                            \
  li x24, 0;                                                            \
  li x25, 0;                                                            \
  li x26, 0;                                                            \
  li x27, 0;                                                            \
  li x28, 0;                                                            \
  li x29, 0;                                                            \
  li x30, 0;                                                            \
  li x31, 0;

#define EXCEPTION       \
  .weak  mtvec_handler; \
  la t0, mtvec_handler; \
  csrw	 mtvec,t0;

#define RVTEST_CODE_BEGIN
          .section .text.init;                                             \
          .align  6;                                                       \
          .global _start;
_start:
          INIT_XREG                                                        \
          EXCEPTION                                                        \

#endif

//-----------------------------------------------------------------------
// End Macro
//-----------------------------------------------------------------------

#define RVTEST_CODE_END                                                 \
        unimp

//-----------------------------------------------------------------------
// Pass/Fail Macro
//-----------------------------------------------------------------------

#define RVTEST_PASS                                                     \
        li x1, 1;                                                       \
        li x2, 2;                                                       \
        li x3, 3;                                                       \
1:                                                                      \
        j 1b

#define RVTEST_FAIL                                                     \
        li x1, 0xf;                                                     \
        li x2, 0xf;                                                     \
        li x3, 0xf;                                                     \
1:                                                                      \
        j 1b
