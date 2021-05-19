# SDK

- [SDK](#sdk)
  - [Introduction](#introduction)
  - [Dependency](#dependency)
  - [Getting Started](#getting-started)
  - [Structures](#structures)
  - [Supported FPGA Board](#supported-fpga-board)
  - [Acknowledgement](#acknowledgement)

## Introduction

This folder contains the SDK (Software Development Kit) for the cpu and the SoC.

It contains the drivers for the cpu, drivers for the peripherals, some demo program and benchmark.

## Dependency

Following environment are required to run the flow.

1. Scala and SBT (To generate verilog source code from SpinalHDL)
2. The xPack GNU RISC-V Embedded GCC (To compile C program)
3. Xilinx Vivado/Intel Quartus (To generate bitstream from verilog and program FPGA)
4. Python3 with pyserial package (To download instruction rom image to FPGA)

## Getting Started

Here is the steps to run the coremark program on Arty A7 FPGA Board. All the command start from the repo root directory.

- Step 1: Create verilog source file from SpinalHDL Scala Code.

```bash
sbt "runMain Board.arty_a7.ArtyA7_AppleRISCVSoCMain
```

- Step 2: Generate FPGA bit stream and program the Arty A7 Board. Make sure you have the board connected to you host machine. Or you can do the FPGA program later by hand.

```bash
cd fpga/arty-a7
# To generate bitstream and program FPGA at the same time
make soc PROGRAM=1
# Or to generate the bitstream only, the generated bit stream is under
make soc
```

- Step 3: Compile the coremark software and dump the Instruction ROM file.

```bash
cd sdk/software
make dasm PROGRAM=coremark
```
The generated instruction rom file is is `coremark/coremark.verilog`

- Step 4: Download the instruction rom image into the CPU instruction rom.

  Make sure the the SW0 is in the ON position (this is to enable Introduction rom downloading.) Remember to open a serial port monitor program to receive the output from CPU.

```bash
cd sdk/tool
sudo ./uart_download.py ../software/coremark/coremark.verilog
```

It will take around 40 seconds for the program to complete and send output to the screen under the current configuration.

## Structures

Here is the structure of the SDK package

```text
├── bsp
│   ├── driver
│   ├── env
│   └── newlib
├── demo
│   ├── demo_gpio0
│   ├── demo_led
|   ...
├── software
│   ├── coremark
│   ...
└── tool
    └── ...
```

### BSP

This folder contains Board Support Package.

- driver: contains C driver code for the soc and different peripherals
- env: contains cpu start-up code, init code, gcc linker script and a makefile to compile the C program
- newlib: continas the soc platform specific implementation for newlib library.

### Demo

This folder contains some demo program running on the FPGA development board. It is also used to tests some HW/SW functions. Check the description in the C program to find out more details for each program.

### Software

This folder contains some ported software running on the FPGA development board.

- coremark: contains the ported coremark benchmark program.

## Supported FPGA Board

- Arty A7

## Acknowledgement

Some parts of the software are mainly taken from the following repo/documents with some modification made by myself.

The code in env folder:

- [e200_opensource](https://github.com/SI-RISCV/e200_opensource)
- [hbird-e-sdk](https://github.com/SI-RISCV/hbird-e-sdk)

The newlib porting:

- [Howto: Porting newlib A Simple Guide](https://www.embecosm.com/appnotes/ean9/ean9-howto-newlib-1.0.html#id2719973)