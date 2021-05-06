# Arch Explore

This document mainly stores the micro arch and the coremark score.

## 05/05/2021

- Basic 5 Pipeline Stage.
- ISA: RV32I
- Branch in EX stage. Static predict don't branch
- Git Commit: 1b49e48e31133bd79b5fcf2e9d1286cd9c708892
- Change:
  - Momve the memory input entry to the end of EX stage to leverage the input register of the block ram as the EX/MEM stage register.
  - Pipelined the dmem output as MEM/EX stage register to improve timing.
  - Moved CSR module into MEM stage and pipelined the output as MEM/EX stage to improve timing.
  - Moved the trap controller to MEM stage. Now all the interrupt and exception are handled at MEM stage. This is to match the case that we are writting to the memory at MEM stage.


```text
2K performance run parameters for coremark.
CoreMark Size    : 666
Total ticks      : 2071593593
Total time (secs): 20
Iterations/Sec   : 100
Iterations       : 2000
Compiler version : GCC10.1.0
Compiler flags   : -O2 -fno-common -funroll-loops -finline-functions --param max-inline-insns-auto=20 -falign-functions=4 -falign-jumps=4 -falign-loops=4
Memory location  : STACK
seedcrc          : 0xe9f5
[0]crclist       : 0xe714
[0]crcmatrix     : 0x1fd7
[0]crcstate      : 0x8e3a
[0]crcfinal      : 0x4983
Correct operation validated. See README.md for run and reporting rules.
```

## 05/04/2021

- Basic 5 Pipeline Stage.
- ISA: RV32I
- Branch in EX stage. Static predict don't branch
- Git Commit: 7232ebde484c9051de04e54f3d35e24a17ab494c

```text
2K performance run parameters for coremark.
CoreMark Size    : 666
Total ticks      : 2071593592
Total time (secs): 41
Iterations/Sec   : 48
Iterations       : 2000
Compiler version : GCC10.1.0
Compiler flags   : -O2 -fno-common -funroll-loops -finline-functions --param max-inline-insns-auto=20 -falign-functions=4 -falign-jumps=4 -falign-loops=4
Memory location  : STACK
seedcrc          : 0xe9f5
[0]crclist       : 0xe714
[0]crcmatrix     : 0x1fd7
[0]crcstate      : 0x8e3a
[0]crcfinal      : 0x4983
Correct operation validated. See README.md for run and reporting rules.
```
