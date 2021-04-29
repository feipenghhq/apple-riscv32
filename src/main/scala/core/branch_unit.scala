///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: branch unit
//
// Author: Heqing Huang
// Date Created: 04/03/2021
//
// ================== Description ==================
//
// Branch unit.
//
// - Calculate the branch address
// - Determine if we branch or not for conditional branch
// - Branch unit is in EX stage
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package core

import spinal.core._

case class branch_unit_io(param: CPU_PARAM) extends Bundle {
  val branch_result = in Bool                         // Branch result. from alu_out(0)
  val current_pc    = in UInt(param.PC_WIDTH bits)    // pc value for the branch instruction
  val imm_value     = in SInt(21 bits)                // immediate value
  val rs1_value     = in Bits(param.DATA_WIDTH bits)  // register rs1 value
  val br_op         = in Bool                         // We get branch instruction
  val jal_op        = in Bool                         // We get jump instruction
  val jalr_op       = in Bool
  val ex_stage_valid = in Bool
  val target_pc     = out UInt(param.PC_WIDTH bits)
  val branch_taken  = out Bool
  val instr_addr_misalign_exception = out Bool

}

case class branch_unit(param: CPU_PARAM) extends Component {

  noIoPrefix()

  val io = branch_unit_io(param)

  io.branch_taken := io.ex_stage_valid & (io.jal_op | io.jalr_op | (io.br_op & io.branch_result))
  // Note: JALR instruction needs to set the target address lsb to zero.
  // Here we just blindly set the lsb to zero for the following reason:
  // 1. We only support RV32 so our PC should be aligned to word boundary.
  // 2. The immediate value for branch and jal instruction has its lsb already set to zero.
  val addr_in = Mux(io.jalr_op, io.rs1_value.asUInt, io.current_pc)  // For jalr, rs1 value is used, others use pc
  io.target_pc := addr_in + io.imm_value.resize(param.DATA_WIDTH bits).asUInt
  io.target_pc(0) := False

  // Check address misaligned exception
  val pc_1_0 = io.target_pc(1 downto 0)
  io.instr_addr_misalign_exception := (io.branch_taken & (pc_1_0 =/= 0))
}
