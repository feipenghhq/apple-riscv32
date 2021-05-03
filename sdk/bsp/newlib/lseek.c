//
// A minimal implementation has no file system, so this function can return 0,
// indicating that the only stream (standard output) is positioned at the start of file.
//

int _lseek (int file, int offset, int   whence)
{
  return  0;
}