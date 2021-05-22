OUTPUT_ARCH( "riscv" )
ENTRY(rvtest_entry_point)

SECTIONS
{
  . = 0x20000000;
  .text.init : { *(.text.init) }
  . = ALIGN(0x8000000);
  .tohost : { *(.tohost) }
  . = ALIGN(0x80000000);
  .text : { *(.text) }
  . = ALIGN(0x80000000);
  .data : { *(.data) }
  .bss : { *(.bss) }
  _end = .;
}

