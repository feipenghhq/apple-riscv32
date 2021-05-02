///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: RegFile
//
// Author: Heqing Huang
// Date Created: 04/30/2021
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
import spinal.lib._

/**
 * Rs Read Port input
 */
case class RsCtrlStage() extends Bundle with IMasterSlave{
  val addr = in UInt(5 bits)
  val rd   = in Bool
  override def asMaster(): Unit = {
    out(addr, rd)
  }
}

/**
 * Register file module
 */
case class RegFile() extends Component {

  val io = new Bundle {
    val rs1RdCtrl = slave(RsCtrlStage())
    val rs2RdCtrl = slave(RsCtrlStage())
    val rs1Data   = out Bits(AppleRISCVCfg.xlen bits)
    val rs2Data   = out Bits(AppleRISCVCfg.xlen bits)
    val rdWrCtrl  = slave(RdWrStage())
    val rdWdata   = in Bits(AppleRISCVCfg.xlen bits)
  }
  noIoPrefix()

  val rs1Data: Bits = Bits(AppleRISCVCfg.xlen bits)
  val rs2Data: Bits = Bits(AppleRISCVCfg.xlen bits)
  val regFileRam: Mem[Bits] = Mem(Bits(AppleRISCVCfg.xlen bits), wordCount = 32)

  regFileRam.write(
    enable  = io.rdWrCtrl.wr,
    address = io.rdWrCtrl.addr,
    data    = io.rdWdata
  )
  rs1Data := regFileRam.readAsync(
    address = io.rs1RdCtrl.addr
  )
  rs2Data := regFileRam.readAsync(
    address = io.rs2RdCtrl.addr
  )

  // Internal forwarding logic
  when(io.rs1RdCtrl.addr === 0) {
    io.rs1Data:= 0
  }.elsewhen((io.rs1RdCtrl.addr === io.rdWrCtrl.addr) && io.rdWrCtrl.wr && io.rs1RdCtrl.rd) {
    io.rs1Data := io.rdWdata
  }.otherwise {
    io.rs1Data := rs1Data
  }

  // Internal forwarding logic
  when(io.rs2RdCtrl.addr === 0) {
    io.rs2Data := 0
  }.elsewhen((io.rs2RdCtrl.addr === io.rdWrCtrl.addr) && io.rdWrCtrl.wr && io.rs2RdCtrl.rd) {
    io.rs2Data := io.rdWdata
  }.otherwise {
    io.rs2Data := rs2Data
  }
}
