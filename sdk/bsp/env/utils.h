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
// Defining some common routine
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <stddef.h>

/**
 * Read CSR register
 */
#define read_csr(reg) ({ uint32_t __tmp; \
asm volatile ("csrr %0, " #reg:"=r"(__tmp)); \
__tmp;})

#define rdmcycle() read_csr(mcycle)
#define rdmcycleh() read_csr(mcycleh)
#define rdmtval()  read_csr(mtval)


/**
 * Write CSR register
 */
#define write_csr(reg, val) ({ \
  if (__builtin_constant_p(val) && (unsigned long)(val) < 32) \
    asm volatile ("csrw " #reg ", %0" :: "i"(val)); \
  else \
    asm volatile ("csrw " #reg ", %0" :: "r"(val)); })
