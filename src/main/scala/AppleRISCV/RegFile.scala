///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: regfile
//
// Author: Heqing Huang
// Date Created: 03/27/2021
//
// ================== Description ==================
//
// Register File
//
// - RV32I ISA supports 32 register and each register is 32 bits.
// - Register File has two RW ports
// - x0 is fixed to value ZERO
// - Register File read is async
// - Support internal forwarding
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._

case class RegFile() extends Component {

    val io = new Bundle{
        val ADDR_WIDTH = log2Up(AppleRISCVCfg.RF_SIZE)
        // Read Port A
        val rs1_rd_addr = in UInt(ADDR_WIDTH bits)
        val rs1_data_out = out Bits(AppleRISCVCfg.XLEN bits)
        // Read Port B
        val rs2_rd_addr = in UInt(ADDR_WIDTH bits)
        val rs2_data_out = out Bits(AppleRISCVCfg.XLEN bits)
        // Write Port
        val register_wr = in Bool
        val register_wr_addr = in UInt(ADDR_WIDTH bits)
        val rd_wdata = in Bits(AppleRISCVCfg.XLEN bits)
    }
    noIoPrefix()

    val rs1_data = Bits(AppleRISCVCfg.XLEN bits)
    val rs2_data = Bits(AppleRISCVCfg.XLEN bits)

    // Implementation  Use same separate ram for the two ports
    val ram = Mem(Bits(AppleRISCVCfg.XLEN bits), wordCount = AppleRISCVCfg.RF_SIZE)

    ram.write(
        enable = io.register_wr,
        address = io.register_wr_addr,
        data = io.rd_wdata
    )
    rs1_data := ram.readAsync(
        address = io.rs1_rd_addr
    )
    rs2_data := ram.readAsync(
        address = io.rs2_rd_addr
    )

    // Note: Some special logic for both the ports
    // 1. Special logic for x0, x0 always return 0 when reading
    // 2. Internal forwarding data from the WB stage to ID stage
    when(io.rs1_rd_addr === 0) {
        io.rs1_data_out := 0
   }.elsewhen((io.rs1_rd_addr === io.register_wr_addr) && (io.register_wr === True)) {
        io.rs1_data_out := io.rd_wdata
    }.otherwise {
        io.rs1_data_out := rs1_data
    }

    when(io.rs2_rd_addr === 0) {
        io.rs2_data_out := 0
    }.elsewhen((io.rs2_rd_addr === io.register_wr_addr) && (io.register_wr === True)) {
        io.rs2_data_out := io.rd_wdata
    }.otherwise {
        io.rs2_data_out := rs2_data
    }
}
