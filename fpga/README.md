# FPGA

- [FPGA](#fpga)
  - [Introduction](#introduction)
  - [Build FPGA Image and Program the Board](#build-fpga-image-and-program-the-board)
  - [Run Demo Program in FPGA Board](#run-demo-program-in-fpga-board)

## Introduction

This folder contains FPGA related build scripts and constraints files for different FPGA board.

Currently, the supported FPGA boards are:

- Arty A7
- Altera DE2 (Note: there are still bugs in the DE2 design)

In order to run the sample code in the FPGA, the uart port in the FPGA board should be connected to the host machine.

## Build FPGA Image and Program the Board

Make sure you have the correct Vivado/Quartus installed in your computer for the corresponding FPGA.

### One Shop Run

You can run the make command in the FPGA directory for different board.

| Command            | Description                 |
| ------------------ | --------------------------- |
| `make <board>`     | Build and program the board |
| `make <board>_pgm` | program the board only      |

For example: `make arty` or `make arty_pgm`

### Command for Each Board

In the board directory, the following command can be used:

| Item                  | Command      | Description                       |
| --------------------- | ------------ | --------------------------------- |
| Build FPGA image      | `make build` | Build the FPGA image              |
| Program the Board     | `make pgm`   | Program the board                 |
| Everything in one cmd | `make all`   | Build image and program the board |

## Run Demo Program in FPGA Board

I have pre-build some demo program for the FPGA Board for the user to try out.
The programs are store in **programs** directory.

Note: remember to press the reset button after downloading the program to FPGA board

### Blink

This program will blink the LED in the FPGA board.

- Command to run the program: `make blink_<board>`. For example: `make blink_arty`
- Program source: [blink.c](../sdk/demo/blink/blink.c)

### Uart

This program will send word through the uart console and it also takes work through uart console and send it back. A serial console is required to run this program.

- Command to run the program: `make uart_<board>`. For example: `make uart_arty`
- Program source: [uart.c](../sdk/demo/uart/uart.c)

### Coremark

Coremark is a benchmark for MCU. Check <https://www.eembc.org/coremark/> for more details

- Command to run the program: `make coremark_<board>`. For example: `make coremark_arty`
- Program source: [coremark](../sdk/benchmark/coremark/core_main.c)