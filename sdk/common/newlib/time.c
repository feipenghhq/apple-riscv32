//
// Provide Process Timing Information, times
//

#include <sys/times.h>
#include <stdint.h>
#include "sysutils.h"

clock_t
_times (struct tms *buf)
{

  // clock_t is 32 bits and 32 bit counter is not good enough.
  // So here we will ignore the lower 10 bits
  // and pad the mcycleh[9:0] and mcycle[31:10] together.
  // the clock is in term of 1024 clock cycle
  clock_t clock;
  uint32_t clock_lo = (uint32_t) _read_csr(mcycle);
  uint32_t clock_hi = (uint32_t) _read_csr(mcycleh);
  clock = clock_hi << (32 - 10) | (clock_lo >> 10);

  buf->tms_utime = 0;
  buf->tms_stime = clock;
  return  clock;
}