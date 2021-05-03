//
// In the minimal implementation, this function always fails, since there is only standard output,
// which is not a valid file to close.
//

#include <errno.h>


int _close (int file)
{
  return EBADF;
}