# SDK

- [SDK](#sdk)
  - [Introduction](#introduction)
  - [Dependency](#dependency)
  - [Structures](#structures)
  - [Getting Started](#getting-started)
  - [Supported FPGA Board](#supported-fpga-board)
  - [Acknowledgement](#acknowledgement)

## Introduction

This folder contains the SDK (Software Development Kit) for the cpu and the SoC.

It contains the drivers for the cpu, drivers for the peripherals, some demo program and benchmark.

## Dependency

**GNU MCU Eclipse RISC-V Embedded GCC** is used to compile C code.

<https://gnu-mcu-eclipse.github.io/blog/2019/05/21/riscv-none-gcc-v8-2-0-2-2-20190521-released>

## Structures

Here is the structure of the SDK package

```text
├── benchmark             -> Containing benchmark program
│   └── coremark            -> coremark benchmark
├── bsp                   -> Board Support Package
│   ├── arty                -> arty A7 board
│   └── de2                 -> de2 board
├── common                -> Common library
│   ├── boot                -> boot related code
│   ├── common.mk           -> makefile for compilation
│   ├── driver              -> driver
│   │   ├── driver.mk         -> makefile for driver
│   │   ├── peripherals       -> peripherals driver
│   │   └── platform          -> platform driver
│   └── newlib              -> ported newlib stub function
├── demo                  -> demo program
│   ├── blink               -> blink LED program
    ...
└── tools                 -> containing useful scripts/tools
    ...
```

## Getting Started

All the compilation is makefile based flow and is quite simple. Just simply cd into the code directory and run:

```bash
make BOARD=<board>
```

Use `make clean` to clean up the directory

For example, to compile the blink demo program for Arty board:

```bash
cd sdk/demo/blink
make BOARD=arty
```

Same make command applies to both demo program and benchmark program.


## Supported FPGA Board

- Arty A7
- DE2

## Acknowledgement

The software design are referenced from the following repo:

- [e200_opensource](https://github.com/SI-RISCV/e200_opensource)
- [hbird-e-sdk](https://github.com/SI-RISCV/hbird-e-sdk)

The newlib porting is referenced from:

- [Howto: Porting newlib A Simple Guide](https://www.embecosm.com/appnotes/ean9/ean9-howto-newlib-1.0.html#id2719973)