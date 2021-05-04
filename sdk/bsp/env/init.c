///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// Init Code
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <uart.h>
#include <soc.h>

extern int main(int argc, char** argv);

void _init() {

    // init the uart module
    uart_setup_appleriscv(UART_BASE);

}

