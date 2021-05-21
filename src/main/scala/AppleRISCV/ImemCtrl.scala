///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: ImemCtrl
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
import AppleRISCVSoC.bus._
import spinal.lib.master

case class ImemCtrl() extends Component {

  val io = new Bundle {
    val stage_valid = in Bool
    val cpu2mc_addr = in UInt(AppleRISCVCfg.XLEN bits)
    val cpu2mc_en   = in Bool
    val mc2cpu_data = out Bits(AppleRISCVCfg.XLEN bits)
    val imem_sib    = master(Sib(AppleRISCVCfg.sibCfg))
    val exc_instr_acc_flt = out Bool
  }
  noIoPrefix()

  // Master signals
  io.imem_sib.sel    := io.stage_valid // We always want to read instruction memory
  io.imem_sib.enable := io.cpu2mc_en
  io.imem_sib.addr   := io.cpu2mc_addr
  io.imem_sib.wdata  := 0
  io.imem_sib.write  := False
  io.imem_sib.mask   := 0

  // Slave signals
  io.mc2cpu_data       := io.imem_sib.rdata
  io.exc_instr_acc_flt := io.stage_valid & io.imem_sib.sel & io.imem_sib.ready & ~io.imem_sib.resp
}
