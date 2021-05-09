//
// Provide Process Timing Information, times
//

#include <errno.h>
#include <sys/times.h>
#include <stdint.h>
#include "utils.h"

clock_t
_times (struct tms *buf)
{

  // 32 bit counter is not good enough. so here we will ignore the lower 10 bits
  // and pad the mcycleh[9:0] and mcycle[31:10] together.
  // the clock is in term of 1024 clock cycle
  unsigned long clock;
  uint32_t clock_lo = (uint32_t) rdmcycle();
  uint32_t clock_hi = (uint32_t) rdmcycleh();
  //printf("clock lo = %u, clock hi = %u ", clock_lo, clock_hi);
  clock = (clock_hi << (32 - 10)) | (clock_lo >> 10);
  //printf("clock = %d\n", clock_lo, clock_hi, clock);

  buf->tms_cstime = clock;
  buf->tms_cutime = clock;
  return  clock;
}