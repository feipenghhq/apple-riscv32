# CLIC - Core Level Interrupt Controller

- [CLIC - Core Level Interrupt Controller](#clic---core-level-interrupt-controller)
  - [Introduction](#introduction)
  - [IO Ports](#io-ports)
  - [CLIC Register Mapping](#clic-register-mapping)
  - [Register Description](#register-description)

## Introduction

CLIC contains the logic for triggering cpu timer interrupt and software interrupt.

## IO Ports

| Name               | direction | Description                |
| ------------------ | --------- | -------------------------- |
| software_interrupt | out       | software interrupt pending |
| timer_interrupt    | out       | timer interrupt pending    |
| clic_sib_*         | slave     | clic sib bus               |

clock and reset are not included in the table

## CLIC Register Mapping

The address map for the CLIC register is as follows:

| Register    | Address    |
| ----------- | ---------- |
| msip        | 0x02000000 |
| mtime_lo    | 0x02000004 |
| mtime_hi    | 0x02000008 |
| mtimecmp_lo | 0x0200000C |
| mtimecmp_hi | 0x02000010 |

## Register Description

- **msip**: machine-mode software interrupt pending.
  - Write 1 to this register will trigger **software interrupt.**
    - It will continue triggering interrupt until it is cleared
  - Write 0 to this register will stop the software interrupt.
- **mtime_lo/mtime_hi**: machine-mode timer register.
  - A free running timer.
  - The register is defined as 64 bits wide so it is divided to two register.
- **mtimecmp_lo/mtimecmp_hi**: machine-mode timer compare register.
  - mtime and mtimecmp together is used to trigger **timer interrupt**.
  - When mtime >= mtimecmp, timer interrupt will be triggered until cpu core has changed mtimecmp to a larger value of change mtime register to a smaller value.
  - When mtimecmp is set to 0, timer interrupt will not be triggered
  - The register is defined as 64 bits wide so it is divided to two register.
