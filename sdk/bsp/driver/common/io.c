///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/02/2021
//
// ================== Description ==================
//
// Defining routine to read/write IO
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <io.h>

/**
 * Read IO device, return the value
 */
int32_t IORD(uint32_t base, uint32_t offset) {
    uint32_t* ptr;
    ptr = (uint32_t *) (base + offset);
    return *ptr;
}

/**
 * Write IO device.
 */
void IOWR(uint32_t base, uint32_t offset, int32_t value) {
    uint32_t* ptr;
    ptr = (uint32_t *) (base + offset);
    *ptr = value;
}

/**
 * Set Specific bits using the mask
 * When the corresponding bit in mask is set, it will set that bit
 */
void IOSET(uint32_t base, uint32_t offset, int32_t mask) {
    int32_t data;

    data = IORD(base, offset);
    data = data | mask;
    IOWR(base, offset, data);
}

/**
 * Clear Specific bits using the mask
 * When the corresponding bit in mask is set, it will clear that bit
 */
void IOCLEAR(uint32_t base, uint32_t offset, int32_t mask) {
    int32_t data;

    data = IORD(base, offset);
    data = data ^ mask;
    IOWR(base, offset, data);
}