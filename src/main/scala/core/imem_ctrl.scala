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

package core

import spinal.core._
import bus.sib._
import spinal.lib.master

case class imem_ctrl_io(param: CPU_PARAM) extends Bundle {

  // CPU side
  val cpu2mc_addr = in UInt(param.XLEN bits)
  val cpu2mc_en   = in Bool
  val mc2cpu_data = out Bits(param.XLEN bits)
}

case class imem_ctrl(param: CPU_PARAM) extends Component {

  noIoPrefix()

  val io = imem_ctrl_io(param)
  val imem_sib = master(Sib(param.sibCfg))

  // Master signals
  imem_sib.sel       := True     // We always want to read instruction memory
  imem_sib.enable    := io.cpu2mc_en
  imem_sib.addr      := io.cpu2mc_addr
  imem_sib.wdata     := 0        // Fixed to zero, We are not writing to I-mem through this port
  imem_sib.write     := False    // Fixed to zero, We are not writing to I-mem through this port
  imem_sib.mask      := 0

  // Slave signals
  io.mc2cpu_data      := imem_sib.rdata

  //val imem_ready      = imem_sib.ready    // This should always 1
  //val imem_resp       = imem_sib.resp     // This should always 1
  //val imem_data_vld   = imem_ready & imem_resp
}
