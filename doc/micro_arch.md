# Micro Architecture

- [Micro Architecture](#micro-architecture)
  - [Apple RISCV soc](#apple-riscv-soc)
  - [Address Mapping](#address-mapping)
  - [SOC Bus](#soc-bus)
  - [Main SOC Component](#main-soc-component)
  - [Peripherals Component](#peripherals-component)
  - [Debug Component](#debug-component)

## Apple RISCV soc

The apple riscv soc contains necessary peripherals for the cpu core to run basic embedded task.

Here is the block diagram of the soc

![soc](assets/img/soc.drawio.png)

## Address Mapping

Here is the address mapping for each memory-mapped components:

| Component       | Address Range           | Size |
| --------------- | ----------------------- | ---- |
| Instruction ROM | 0x00000000 - 0x00FFFFFF | 16MB |
| Data RAM        | 0x01000000 - 0x01FFFFFF | 16MB |
| CLIC            | 0x02000000 - 0x02000FFF | 4KB  |
| PLIC            | 0x02001000 - 0x02001FFF | 4KB  |
| Timer           | 0x02002000 - 0x02002FFF | 4KB  |
| Uart            | 0x02003000 - 0x02003FFF | 4KB  |
| GPIO0           | 0x02004000 - 0x02004FFF | 4KB  |
| GPIO1           | 0x02005000 - 0x02005FFF | 4KB  |

## SOC Bus

Apple RISC-V SOC is using a proprietary bus called SIB - simple internal bus. Here is the feature of the bus

- Using a valid/ready based handshaking mechanism.
- Point to point connection, supporting complex connection such as bus matrix
- Supporting pipelined and back-to-back operation
- No burst operation, if needed, the main module needs to put the request into the bus one by one

Here is the signal definition for SIB.

| Signal Name | Width | Direction   | Description                                                                         |
| ----------- | ----- | ----------- | ----------------------------------------------------------------------------------- |
| sib_sel     | 1     | from source | When sel is high, this module is selected for access                                |
| sib_enable  | 1     | from source | When enable is low, write should be blocked and read should hold the previous value |
| sib_addr    | 1     | from source | Address                                                                             |
| sib_write   | 1     | from source | When write is high, the operation is write, otherwise the operation is read         |
| sib_wdata   | DW    | from source | The write data from the main module                                                 |
| sib_rdata   | DW    | to source   | The read data from the targeting module                                             |
| sib_mask    | DW/8  | from source | Byte enable for write, when set to 1 the corresponding byte is enabled              |
| sib_ready   | 1     | to source   | Indicate the targeting module is ready for the access                               |
| sib_resp    | 1     | to source   | Indicate whether the transaction is good or not. 1 - good, 0 - error                |

For more detains and the waveform for different transaction, please check the [SIB document](ip/sib.md)

## Main SOC Component

### CLIC (Core Level Interrupt Controller)

CLIC contains the logic for triggering cpu timer interrupt and software interrupt.

Check [CLIC document](ip/clic.md) for more details and internal register address mapping.

### PLIC (Platform Level Interrupt Controller)

CLIC contains the logic for triggering cpu timer interrupt and software interrupt.

Check [CLIC document](ip/clic.md) for more details and internal register address mapping.

### RstCtrl (Reset Controller)

## Peripherals Component

### Timer

### Uart

### GPIO

## Debug Component

### uart2imem

This module is used to download instruction stream into instruction ram through uart. It takes the data received from the uart rx port and send the data to instruction rom. Check [uart2imem documents](ip/uart2imem.md) for more details.
