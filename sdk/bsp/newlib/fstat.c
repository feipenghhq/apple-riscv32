//
// Provide the Status of an Open File, fstat
//
// A minimal implementation should assume that all files are character special devices
// and populate the status data structure accordingly.
//

#include <errno.h>
#include <sys/stat.h>
#include <unistd.h>

int
_fstat (int file, struct stat *st)
{
    if ((STDOUT_FILENO == file) || (STDERR_FILENO == file))
    {
        st->st_mode = S_IFCHR;
        return  0;
    }
    else
    {
        errno = EBADF;
        return  -1;
    }
}
