///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: forwarding unit
//
// Author: Heqing Huang
// Date Created: 04/08/2021
//
// ================== Description ==================
//
// Forwarding/Bypassing Unit
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package core

import spinal.core._

case class fu_io(param: CPU_PARAM) extends Bundle {
  val ex_rs1_idx = in UInt(param.RF_ADDR_WDITH bits)
  val ex_rs2_idx = in UInt(param.RF_ADDR_WDITH bits)
  val mem_rd_idx = in UInt(param.RF_ADDR_WDITH bits)
  val wb_rd_idx  = in UInt(param.RF_ADDR_WDITH bits)
  val ex_rs1_rd  = in Bool
  val ex_rs2_rd  = in Bool
  val mem_rd_wr  = in Bool
  val wb_rd_wr   = in Bool
  val forward_rs1_from_mem = out Bool
  val forward_rs1_from_wb  = out Bool
  val forward_rs2_from_mem = out Bool
  val forward_rs2_from_wb  = out Bool
}

case class fu(param: CPU_PARAM) extends Component {

  noIoPrefix()

  val io = fu_io(param)

  // Note:
  // Need to check if the register is x0 or not. If it is x0, we don't want to forward.
  // since x0 always hold ZERO and but some instruction write to x0 which should be ignored.

  val rs1_nonzero = (io.ex_rs1_idx =/= 0)
  val rs1_match_mem = (io.ex_rs1_idx === io.mem_rd_idx) & rs1_nonzero
  val rs1_match_wb = (io.ex_rs1_idx === io.wb_rd_idx) & rs1_nonzero
  io.forward_rs1_from_mem := io.ex_rs1_rd & rs1_match_mem & io.mem_rd_wr
  io.forward_rs1_from_wb  := io.ex_rs1_rd & rs1_match_wb & io.wb_rd_wr


  val rs2_nonzero = (io.ex_rs2_idx =/= 0)
  val rs2_match_mem = (io.ex_rs2_idx === io.mem_rd_idx) & rs2_nonzero
  val rs2_match_wb = (io.ex_rs2_idx === io.wb_rd_idx) & rs2_nonzero
  io.forward_rs2_from_mem := io.ex_rs2_rd & rs2_match_mem & io.mem_rd_wr
  io.forward_rs2_from_wb  := io.ex_rs2_rd & rs2_match_wb & io.wb_rd_wr
}


