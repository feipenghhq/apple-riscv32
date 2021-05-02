///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: apple_riscv
//
// Author: Heqing Huang
// Date Created: 04/07/2021
//
// ================== Description ==================
//
// Instruction Memory Controller
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import AppleRISCVSoC.Bus._
import spinal.lib.master

case class ImemCtrl() extends Component {

  val io = new Bundle {
    // CPU side
    val cpu2mc_addr = in UInt(AppleRISCVCfg.XLEN bits)
    val cpu2mc_en   = in Bool
    val mc2cpu_data = out Bits(AppleRISCVCfg.XLEN bits)
    val imem_sib = master(Sib(AppleRISCVCfg.sibCfg))
  }
  noIoPrefix()

  // Master signals
  io.imem_sib.sel    := True     // We always want to read instruction memory
  io.imem_sib.enable := io.cpu2mc_en
  io.imem_sib.addr   := io.cpu2mc_addr
  io.imem_sib.wdata  := 0        // Fixed to zero, We are not writing to I-mem through this port
  io.imem_sib.write  := False    // Fixed to zero, We are not writing to I-mem through this port
  io.imem_sib.mask   := 0

  // Slave signals
  io.mc2cpu_data     := io.imem_sib.rdata

  val imem_ready = io.imem_sib.ready    // This should always 1
  val imem_resp  = io.imem_sib.resp     // This should always 1
  //val imem_data_vld   = imem_ready & imem_resp
}
