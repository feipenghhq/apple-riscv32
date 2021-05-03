# Development Log

## Milestone 0.2

- 04/28/2021
  - RTL update
    - All the operation needs to check whether the instruction in that stage is flushed or not
    - Simplified hdu logic

- 04/27/2021
  - Added a reset controller to soc
  - Added clock domain in order to control the reset of the cpu.
  - Cleaned up the scala codes

- 04/26/2021
  - Changed soc architecture. Added a new gpio port to control led/switch/button
  - Designed an uart2imem module which can fill the instruction rom from uart rx module
  - Added the uart2imem module and another uart controller to soc

- 04/23/2021
  - Designed a simple plic module.

- 04/22/2021
  - Redesigned the SIB module.
    - Designed the slaveFactory, so we can use SpinalHDL slaveFactory to create memory mapped register accessing logic
  - Redesigned the clic/gpio/timer module using sibSlaveFactory.
  - Designed the SIB Uart module
  - Wrote a test to test software interrupt
    - **[RTL Bug Fix]** Fixed bug in trap control logic:
      - Corrected the wrong pc output value when interrupt triggers trap
      - Added flushing logic for the interrupt. Need to flush the pipeline when we switch pc to mtval 
  - Wrote a test to test timer interrupt
        
- 04/21/2021
  - Designed a sib switch with 1 to N topology
  - Replaced the AHB bus with the SIB bus
  - Moved memory into soc folder
  - Designed the CLIC module and updated SOC
  - Designed timer and gpio module and updated soc

- 04/20/2021
  - Designed the sib (simple internal bus)
    - sib will be replacing the AHB and used for the SOC inter-connection

- 04/19/2021
  - Designed soc bus
    - Only imem and dmem bus are connected. Other bus will be added in future
    - now instruction memory and data memory are in separate address range and are connected to cpu core though the bus controller
    - updated all the tests related to this change

## Milestone 0.1

Summary:

- Completed Most of the instructions for RV32I ISA including CSR extension and Interrupt
- Setup RISC-test ISA compatible test environment

Details:

- 04/19/2021
  - **[RTL Bug Fix]** Fixed bugs found in tests for csr instruction and exception
    - Fixed bug in mcsr module: wrong mie and mtvec address
    - Fixed bug in dmem_ctrl module: do not read/store memory when there is address misalign exception
    - Fixed bug in cpu: wb stage also need flushing when there are exception happends at mem stage. For example, we need to disable rd_wr when there is load_addr misalign in mem stage
    - Fixed bug in decode logic: corrected logic for invalid instruction detection
    - Fixed bug in trap ctrl module: ecall should trigger pc_trap too.
  - Added more isa tests for csr instruction and exception.

- 04/18/2021
  - Implemented interrupt logic and added few more csr register
  - Updated pipeline logic for zicsr and exception
  - Implemented mret and ecall instruction
  - **[RTL Bug Fix]** Fixed bugs found in tests for csr instruction
    - Fixed bug in hdu logic: there are data dependency on csr instruction, need to add stall logic
    - Fixed bug in mcsr module: missing csr output decode for mscratch register
    - Fixed bug in csr logic: csrrs logic is wrong. The original logic also clear other bits that are not set. It should only set bit
    - Fixed bug in decode logic: csrrw/csrrs/csrrc also read rs1, need to set rs1_rd

- 04/17/2021
  - Designed the csr module and exception logic
  - Implemented the 'Zicsr' csr related instructions

- 04/14/2021
  - Updated instruction decoder logic.
    - Using switch structure for decode logic

- 04/09/2021
  - Updated register file
    - Using single ram instead of two.
  - Updated memory
    - Reduced the size so it can be synthesis in Arty A7 FPGA board
    - Renamed the memory name.
    - Added a debug port on instruction and bring the port to top level.
    - With the debug port FPGA synthesis will not optimize out the instruction memory

- 04/08/2021
  - Moved hazard detection logic and forwarding logic into its own module.

- 04/07/2021
  - **[RTL Bug Fix]** Fixed bugs found in the tests.
    - Fixed bug in forwarding logic: need to check whether the write dest is x0 or not. If it is x0 then do not forward the data
    - Fixed bug in forwarding logic: jalr also read rs1
  - Enhanced the test framework for riscv-test
    - Update the cocotb run.py and makefile flow
    - Added a python script to run all the tests and gather result
  - **[TB Bug Fix]** Updated the tb file on how to load instruction rom. Added the ability to load data rom (for testing load instruction)
    - Now the data in the `.verilog` file is represented by 1 byte (rather than by word).
    - We need it to be byte because the .verilog file specify the address as the absolute byte address (such as @10 (in hex)) but verilog $readmemh treat the address as the index of the defined memory register. For example, if we define reg [31:0] mem[0:1023] then @10 will write the data into mem[10] instead of mem[4] even though each memory block is 4 bytes.
  - Updated the bus system. Using AHB-lite as I-mem and D-mem bus.

- 04/06/2021
  - Added some sample riscv-test code.
    - The codes are mainly copied from riscv-tests repo.
    - Made some modification to make it compatiable to my cpu design.
    - Check the tests/riscv-tests-simple/README.md for more details.
  - Created the cocotb test environment for the riscv-test.
    - Added the makefile and the main test program `run.py`
    - Updated the tb file so it can load the instruction rom generated by riscv-test
  - Updated documents.

## Milestone 0.0

Summary:

- CPU Core Initial Design: Completed the basic RV32I instruction implementation exception for FANCE, ECALL, EBREAK

Details:

- 04/05/2021
  - Major updates on the existing code.
    - Re-designed the ALU logic. Now different operation will share the same operation resources
      if they require the same operation.
    - Re-designed the decode logic based on the new ALU logic.
    - Renamed some signals and modules
  - Implemented jal/jalr instruction.
    - SpinalHDL design and cocotb sanity_check code.

- 04/04/2021
  - Implemented lui/auipc Instruction
    - SpinalHDL design and cocotb sanity_check code.

- 04/03/2021
  - Change on immediate value logic
    - Moved the signed extension logic from the EX stage to ID stage
    - Stores the entire 32 bit immediate value in ID/EX stage pipeline to reduce the work on EX stage
  - Implemented Branch Instruction
    - SpinalHDL design and cocotb sanity_check code.

- 04/01/2021
  - Updated the test directory and cocotb test environment
  - Wrote tests for the load/store instruction
  - Major design updates and bug fixes
    - Updated the data memory model to include byte enable
    - Added missing logic for the load/store instruction.
    - Fixed a major bug on the forwarding logic.
    - Re-designed the logic on how the rs2 register and immediate value are handled.
      - The original design combines them in the ID stage, which create a severe bug after introducing forwarding logic
  - Designed the stall logic on load data dependency
    - Added enable on the instruction ram model for the stall logic
    - Added a HDU (Hazard Detection Unit) to handle the stall logic on load data dependency
    - Added more tests on forwarding and stalling

- 03/31/2021
  - Implemented the forwarding logic and wrote cocotb test
  - Implemented the load/store instruction.
    - Designed a memory controller to handle memory related logic

- 03/30/2021
  - Built the instruction/data memory and soc top.
    - Designed the instruction and data ram module
    - Designed cpu soc top that connects the cpu core with the memory.
  - Build the cocotb test environment.
    - Setup the test directory and the cocotb environemt.
    - Wrote some basic tests for the existing function.
  - Implemented all the logic and arithmetic instruction and wrote cocotb test

- 03/29/2021
  - Built the cpu core logic:
    - Register files, instruction decoder, ALU
    - Built the cpu core top level and the pipeline stages in the top level

- 03/27/2021
  - Setup the repo and spinalHDL sbt environment.
  - Added documents
  - Wrote some components
