//
// This specifically checks whether a stream is a terminal.
// The minimal implementation only has the single output stream,
// which is to the console, so always returns 1.
//

int _isatty (int file)
{
  return  1;
}
