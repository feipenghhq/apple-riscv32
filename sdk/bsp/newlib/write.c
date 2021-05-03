// A minimal implementation only supports writing to standard output.

#include <errno.h>

int _write (int file, char *buf, size_t nbytes)
{
    int i;

    if (isatty(file)) {
        // FIXME: Need to implement uart write logic
        return nbytes;
    }

  return EBADF;
}