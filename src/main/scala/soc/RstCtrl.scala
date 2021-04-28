///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: apple_riscv_soc
//
// Author: Heqing Huang
// Date Created: 04/27/2021
//
// ================== Description ==================
//
// SOC Reset Controller
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package soc

import spinal.core._

case class RstCtrl() extends Component {

  noIoPrefix()

  val io = new Bundle {
    val uart2imem_downloading = in Bool
    val cpu_reset_req         = out Bool
  }

  val cpu_reset = Reg(Bool) init True

  when(io.uart2imem_downloading) {
    cpu_reset := True
  }.otherwise{
    cpu_reset := False
  }
  io.cpu_reset_req := cpu_reset
}
