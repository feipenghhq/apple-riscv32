// Allocate more Heap, sbrk

#include <errno.h>

#undef errno
extern int  errno;

void * _sbrk (int nbytes)
{
    // Symbol defined by linker map - start of free memory (as symbol)
    extern int _end[];

    // Value set by linker map - end of free memory
    extern int _heap_end;

    // The statically held previous end of the heap, with its initialization.
    static int *heap_ptr = _end;

    if ((heap_ptr + nbytes) < _end || (heap_ptr + nbytes > _heap_end)) {
        return NULL - 1;
    }

    return heap_ptr - nbytes;

}