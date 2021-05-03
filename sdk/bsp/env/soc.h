#ifndef _SOC_H_
#define _SOC_H_

// SOC component address mapping
#define IMEM_BASE 0x00000000
#define DMEM_BASE 0x01000000
#define CLIC_BASE 0x02000000
#define PLIC_BASE 0x02001000
#define PERIP_BASE 0x02000000
#define TIMER_BASE (PERIP_BASE + 0x2000)
#define UART_BASE  (PERIP_BASE + 0x3000)
#define GPIO0_BASE (PERIP_BASE + 0x4000)
#define GPIO1_BASE (PERIP_BASE + 0x5000)

#endif