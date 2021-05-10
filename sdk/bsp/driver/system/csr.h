///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/04/2021
//
// ================== Description ==================
//
// Defining common routines for CSR register
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef _CSR_H_
#define _CSR_H_

#include <stddef.h>

/**
 * Read CSR register
 */
#define read_csr(reg) ({ uint32_t __tmp; \
asm volatile ("csrr %0, " #reg:"=r"(__tmp)); \
__tmp;})

#define rdmcycle()          read_csr(mcycle)
#define rdmcycleh()         read_csr(mcycleh)
#define rdmtval()           read_csr(mtval)
#define rdmcountinhibit()   read_csr(mcountinhibit)
#define rdmhpmcounter3()    read_csr(mhpmcounter3)
#define rdmhpmcounter3h()   read_csr(mhpmcounter3h)
#define rdmhpmcounter4()    read_csr(mhpmcounter4)
#define rdmhpmcounter4h()   read_csr(mhpmcounter4h)

/**
 * Write CSR register
 */
#define write_csr(reg, val) ({ \
  if (__builtin_constant_p(val) && (unsigned long)(val) < 32) \
    asm volatile ("csrw " #reg ", %0" :: "i"(val)); \
  else \
    asm volatile ("csrw " #reg ", %0" :: "r"(val)); })

#endif

/**
 * clear and enable branch counter
 */
void clr_en_br_cnt(void);

/**
 * stop branch counter
 */
void stp_br_cnt(void);
