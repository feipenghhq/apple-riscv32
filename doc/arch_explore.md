# Arch Explore

This document mainly stores the micro arch and the coremark score.

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
