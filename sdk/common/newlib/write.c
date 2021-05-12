//
// Write to a File, write
//
// A minimal implementation only supports writing to standard output.
//
//

#include <errno.h>
#include <unistd.h>
#include <stdint.h>

#include "platform.h"

extern void uart_putnc(uint32_t base, char *buf, size_t nbytes);

int _write (int file, char *buf, size_t nbytes)
{

  if (isatty(file)) {
    uart_putnc(UART0_BASE, buf, nbytes);
    return nbytes;
  }

  return EBADF;
}