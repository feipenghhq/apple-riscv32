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