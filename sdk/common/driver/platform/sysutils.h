///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 05/11/2021
//
// ================== Description ==================
//
// Utility Functions
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __SYSUTILS_H__
#define __SYSUTILS_H__


#include <stddef.h>


/** Read IO device, return the value */
#define IORD(base, offset)          (*((volatile uint32_t *) (base + offset)))

/** Write IO device */
#define IOWR(base, offset, value)   (*((uint32_t *) (base + offset)) = value)
#define IOWH(base, offset, value)   (*((uint16_t *) (base + offset)) = value)
#define IOWB(base, offset, value)   (*((uint8_t  *) (base + offset)) = value)

/**
 * Set Specific bits using the mask
 * When the corresponding bit in mask is set, it will set that bit
 */
#define IOSET(base, offset, mask)   IOWR(base, offset, (IORD(base, offset) | mask))


/**
 * Clear Specific bits using the mask
 * When the corresponding bit in mask is set, it will clear that bit
 */
#define IOCLEAR(base, offset, mask) IOWR(base, offset, (IORD(base, offset) ^ mask));

/** Read CSR register */
#define read_csr(reg) ({ uint32_t __tmp; \
asm volatile ("csrr %0, " #reg:"=r"(__tmp)); \
__tmp;})

/** Write CSR register */
#define write_csr(reg, val) ({ \
  if (__builtin_constant_p(val) && (unsigned long)(val) < 32) \
    asm volatile ("csrw " #reg ", %0" :: "i"(val)); \
  else \
    asm volatile ("csrw " #reg ", %0" :: "r"(val)); })

/** clear and enable branch performance counter */
#define clr_en_br_cnt() ({ \
asm volatile ("csrci mcountinhibit, 24; csrw mhpmcounter3, x0; \
csrw mhpmcounter4, x0; csrsi mcountinhibit,  24");})

/** stop branch counter */
#define stp_br_cnt() ({asm volatile ("csrci mcountinhibit, 24");})

#endif