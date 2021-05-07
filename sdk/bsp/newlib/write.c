//
// Write to a File, write
//
// A minimal implementation only supports writing to standard output.
//
//

#include <errno.h>
#include <unistd.h>

#include <uart.h>
#include <soc.h>

int _write (int file, char *buf, size_t nbytes)
{

  if (isatty(file)) {
    uart_send_nbyte(UART_BASE, buf, nbytes);
    return nbytes;
  }

  return EBADF;
}