///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// Start up code
//
///////////////////////////////////////////////////////////////////////////////////////////////////

.section .init
.globl _start
.type _start @function

_start:

    // disable all the interrupt
    csrrci x0, mstatus, 0x8

.option push
.option norelax
    // assign address pointed by __global_pointer$ to gp register
    // __global_pointer is defined in linker script file
    la gp, __global_pointer$
.option pop
    // assign address pointed by _sp to sp register
    // _sp is defined in the linker script file
    la sp, _sp

    // Load data section
	la a0, _data_lma
	la a1, _data
	la a2, _edata
	bgeu a1, a2, 2f
1:
	lw t0, (a0)
	sw t0, (a1)
	addi a0, a0, 4
	addi a1, a1, 4
	bltu a1, a2, 1b
2:

    // initialize bss section
    la a0, __bss_start
    la a1, _end
    bgeu a0, a1, 2f
1:
    sw zero, 0(a0)
    addi a0, a0, 4
    bltu a0, a1, 1b
2:

    // call global constructors
    // Note:
    // __libc_fini_array, atexit and __libc_init_array function are special lib function in
    // Newlib C runtime library.
    la a0, __libc_fini_array
    call atexit
    call __libc_init_array


    // call init function
    call _init

    // call main function
    // argc == argv = 0
    li a0, 0
    li a1, 0
    call main
    tail exit

1:
    j 1b