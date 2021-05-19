# AppleRISCV

- [AppleRISCV](#appleriscv)
  - [Introduction](#introduction)
  - [Key Features](#key-features)
  - [Environment Dependency](#environment-dependency)
  - [Repo Structure](#repo-structure)
  - [Getting Started](#getting-started)
  - [Acknowledgement](#acknowledgement)

## Introduction

AppleRISCV is a RISC-V CPU design using SpinalHDL. I am designing this CPU to learn RISC-V, SpinalHDL, SoC, and also some embedded design.

This repo design provide a RISC-V CPU core and a system-on-chip (SoC) design similar to SiFive Freedom E310. (Not all the peripherals are currently implemented). Since the design is mainly targeting FPGA, the SoC design are based on the the FPGA board (with slightly different peripherals and IO mappings)

## Key Features

### Hardware

- Supporting RV32I ISA, and 'Zicsr' extensions.
  - Only a subset of the csr register are implemented.
- Supporting RV32M ISA (optional)
  - Multiplier is implemented with FPGA DSP resource.
  - Divider is a serial divider taking 33 clocks to complete.
- CPU core micro-architecture: 5 stage pipeline design with IF, ID, EX, MEM, WB stages
- Optional Branch Prediction Unit/Branch Target Buffer to improve branch performance.
- Support 4 interrupts (external, timer, software, debug) defined in RISC-V Specification

### Software

- Provide a simple Board Support Package (BSP) for different board and SoC.
- Ported newlib to support AppleRISCV SoC.
- Support C programming language compiled to newlib c library.
- Support C input and output using Uart console

## Environment Dependency

### RISC-V Tool Chain

**GNU MCU Eclipse RISC-V Embedded GCC** is used to compile the C code into RISC-V ISA using newlib as the c standard library.

- GNU MCU Eclipse RISC-V Embedded GCC: <https://gnu-mcu-eclipse.github.io/blog/2019/05/21/riscv-none-gcc-v8-2-0-2-2-20190521-released>

### SpinalHDL, Scala and SBT

SpinalHDL is a Scala based Hardware Construction Language. The HDL code in the repo is written in scala. SBT is an interactive build tool for Scala.

- SpinalHDL: <https://spinalhdl.github.io/SpinalDoc-RTD/>
- Scala: <https://www.scala-lang.org/>
- SBT: <https://www.scala-sbt.org/>

### Python3, Cocotb, Icarus Verilog

cocotb is a COroutine based COsimulation TestBench environment for verifying VHDL and SystemVerilog RTL using Python. Icarus Verilog is an open source verilog simulator. The test environment in this repo is cocotb and the verilog code is simulated in Icarus Verilog

- Python3: <https://www.python.org/downloads/>
- Cocotb: <https://docs.cocotb.org/en/stable/index.html>
- Icarus Verilog <http://iverilog.icarus.com/>

### Xilinx Vivado/Intel Quartus

Vivado and Quartus are used to synthesis the design and generate FPGA bit stream.

- Vivado: <https://www.xilinx.com/products/design-tools/vivado.html>
- Quartus: <https://www.intel.com/content/www/us/en/software/programmable/quartus-prime/overview.html>

## Repo Structure

Here is the structures for AppleRISCV repo

| Name    | Description                                                              |
| ------- | ------------------------------------------------------------------------ |
| doc     | documents related to design and architecture                             |
| fpga    | FPGA related build script and constraints files for different FPGA board |
| project | SBT related folder                                                       |
| src     | scala HDL code and verilog code                                          |
| tests   | riscv-test, cocotb framework                                             |
| sdk     | board support package, libraries, demo program and benchmark             |

## Getting Started

You build FPGA image, downloads it to the FPGA board and run the demo program. An Arty-A7 board is preferred for the quick demo.

1. Connect the Arty-A7 FPGA development board to the host machine
2. `cd fpga`
3. `make arty`
4. `make blink_arty`

This will run the blink program and blink the 4 LEDs in the FPGA board.

For more details and other demo, check [fpga/README.md](fpga/README.md)

## Acknowledgement

1. The SoC design (address mapping, peripherals) is based on the SiFive Freedom E310 SoC. <https://old-www.sifive.com/products/freedom-e310/>
2. The SDK design is mainly based on hbird-e-sdk <https://github.com/SI-RISCV/hbird-e-sdk>