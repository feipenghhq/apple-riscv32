///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// CPU init code - config and setup the peripherials
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#include <uart.h>
#include <soc.h>

extern int main(int argc, char** argv);

void _init() {

    // init the uart with default configuration
    uart_setup_appleriscv(UART_BASE);

}

