# SIB - Simple Internal Bus

- [SIB - Simple Internal Bus](#sib---simple-internal-bus)
  - [Introduction](#introduction)
  - [Signal Definition](#signal-definition)
  - [Transfer](#transfer)

## Introduction

The Apple RISCV soc use **SIB** as the internal bus to connect each components.

Here are the SIB features:

- Using a valid/ready based handshaking mechanism.
- Point to point connection, supporting complex connection such as bus matrix
- Supporting pipelined and back-to-back operation
- No burst operation, if needed, the main module needs to put the request into the bus one by one

## Signal Definition

Here is the signal definition for SIB.

| Signal Name | Width | Dir       | Description                                                                         |
| ----------- | ----- | --------- | ----------------------------------------------------------------------------------- |
| sib_sel     | 1     | from Main | When sel is high, this module is selected for access                                |
| sib_enable  | 1     | from Main | When enable is low, write should be blocked and read should hold the previous value |
| sib_addr    | 1     | from Main | Address                                                                             |
| sib_write   | 1     | from Main | When write is high, the operation is write, otherwise the operation is read         |
| sib_wdata   | DW    | from Main | The write data from the main module                                                 |
| sib_rdata   | DW    | to Main   | The read data from the targeting module                                             |
| sib_mask    | DW/8  | from Main | Byte enable for write, when set to 1 the corresponding byte is enabled              |
| sib_ready   | 1     | to Main   | Indicate the targeting module is ready for the access                               |
| sib_resp    | 1     | to Main   | Indicate whether the transaction is good or not. 1 - good, 0 - error                |

## Transfer

### Simple Read/Write Operation

![simple rw](assets/waveform/sib_simple_rw.png)

### Pipelined Operation

![pipelined rw](assets/waveform/sib_pipelined_rw.png)

Pipelined operation supporting mixed operation (read followed by write or write followed by read)

### Operation with Wait state

![wait](assets/waveform/sib_wait.png)

### Operation with Read Stall state

![stall](assets/waveform/sib_stall.png)

The source can "stall" data by asserting **sel** signal and de-asserting **enable** signal. This tells the sink module to hold the read data from previous cycle.

Note: there is no write stall state.