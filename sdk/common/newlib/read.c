//
// Read from a File, read
//
// A minimal implementation has no file system.
// Rather than failing, this function returns 0, indicating end-of-file.
//
// In our case, we have a UART RX port so we read from there.
//

#include "platform.h"
#include "periphals.h"

extern int isatty (int file);

int _read (int file, char *ptr, int len)
{
  int i;

  // if the file is tty, we read from uart
  if (isatty(file)) {
    for (i = 0; i < len; i++) {
      ptr[i] = _uart_getc(UART0_BASE);

      // return partial value if we get EOL
      if ('\n' == ptr[i]) {
        return i;
      }
    }
    return i;
  }
  return  0;    // EOF
}