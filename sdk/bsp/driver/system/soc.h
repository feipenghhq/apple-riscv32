///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// Define files for the SOC
//
///////////////////////////////////////////////////////////////////////////////////////////////////


#ifndef _SOC_H_
#define _SOC_H_

// Clock Frequency in MHz
#define CLK_FEQ_MHZ     100

// SOC component address mapping
#define IMEM_BASE       0x00000000
#define DMEM_BASE       0x01000000

#define CLIC_BASE       0x02000000
#define PLIC_BASE       0x02001000

#define PERIP_BASE      0x02000000
#define TIMER_BASE      (PERIP_BASE + 0x2000)
#define UART_BASE       (PERIP_BASE + 0x3000)
#define GPIO0_BASE      (PERIP_BASE + 0x4000)
#define GPIO1_BASE      (PERIP_BASE + 0x5000)

#endif