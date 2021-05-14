#define UART_TXDATA                 0x000
#define UART_RXDATA                 0x004
#define UART_TXCTRL                 0x008
#define UART_RXCTRL                 0x00C
#define UART_IE                     0x010
#define UART_IP                     0x014
#define UART_DIV                    0x018

#define _uart_tx_full(base)          ((IORD(base, UART_TXDATA) >> 31) & 0x1)
#define _uart_tx_byte(base, data)    (IOWR(base, UART_TXDATA, data & 0xFF))

#define _uart_rx(base)               (IORD(base, UART_RXDATA))
#define _uart_rx_data(data)          (data & 0xFF)
#define _uart_rx_valid(data)         ((data >> 31) & 0x1)

#define _uart_tx_cfg(base, data)     (IOWR(base, UART_TXCTRL, data))
#define _uart_rx_cfg(base, data)     (IOWR(base, UART_RXCTRL, data))

#define _uart_tx_en(base)            (IOSET(base, UART_TXCTRL, 0x1))
#define _uart_rx_en(base)            (IOSET(base, UART_RXCTRL, 0x1))

#define _uart_txwm_en(base)          (IOSET(base, UART_IE, 0x1))
#define _uart_rxwm_en(base)          (IOSET(base, UART_IE, 0x2))
#define _uart_wm_en(base)            (IOSET(base, UART_IE, 0x3))

#define _uart_txwm(base)             (IORD(base, UART_IP) & 0x1))
#define _uart_rxwm(base)             ((IORD(base, UART_IP) >> 1) & 0x1)

#define _uart_set_div(base, data)    (IOWR(base, UART_DIV, data))

#define _BAUDRATE                   115200
#define _SAMPLE                     8
//_CLKDIV = clkFrequency / baudrate / g.rxSamplePerBit
#define _CLKDIV                     (CLK_FEQ_MHZ * 1000000 / _SAMPLE / _BAUDRATE)

void _uart_init(uint32_t base);
char _uart_putc(uint32_t base, char c);
void _uart_puts(uint32_t base, char *s);
void _uart_putnc(uint32_t base, char *buf, size_t nbytes);
char _uart_getc(uint32_t base);