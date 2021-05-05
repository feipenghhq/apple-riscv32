//
// Provide Process Timing Information, times
//
// A minimal implementation need not offer any timing information,
// so should always fail with an appropriate value in errno.
//

#include <errno.h>
#include <sys/times.h>

#undef errno
extern int  errno;

int _times (struct tms *buf)
{
  errno = EACCES;
  return  -1;
}
