//
// Closing a file, close
//
// In the minimal implementation, this function always fails, since there is only standard output,
// which is not a valid file to close.
//

#include <errno.h>
#undef errno
extern int  errno;

int _close (int file)
{
  return EBADF;
}