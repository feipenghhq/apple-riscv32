# uart2imem

- [uart2imem](#uart2imem)
  - [Introduction](#introduction)
  - [IO Ports](#io-ports)
  - [Register Mapping](#register-mapping)
  - [Function Description](#function-description)

## Introduction

This module is used to download instruction stream into instruction ram through uart. It takes the data received from the uart rx port and send the data to instruction rom.

## IO Ports

| Name          | direction | Description                                                                  |
| ------------- | --------- | ---------------------------------------------------------------------------- |
| uart_txd      | out       | uart tx port. Not used                                                       |
| uart_rxd      | in        | uart rx port. Shared with the uart peripheral                                |
| load_imem     | in        | load imem strap signal, set it to 1 will enable the imem downloading feature |
| downloading   | out       | indicate instruction downloading in process                                  |
| imem_dbg_sib* | slave     | imem_dbg sib bus                                                             |

clock and reset are not included in the table

## Register Mapping

No register in this module

## Function Description

This module receives bytes from the uart controller and aggregate 4 bytes in to a word.

When the load_imem strap is set to 1, it will wait for the **start command** and then start to send received word sequentially to instruction ram with address in increasing order starting from 0. The start command is defined as 32'hFFFFFFFF

During the instruction downloading, it will assert downloading signal and the reset controller should keep the cpu under reset condition.

When it receives **stop command**, it will finish the downloading process, de-assert downloading signal and reset the address. The stop command is defined as 32'hFFFFFFFE.
