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

package AppleRISCV

import spinal.core._

case class BU() extends Component {

  val io = new Bundle {
    val current_pc = in UInt(AppleRISCVCfg.XLEN bits)  // pc value for the branch instruction
    val imm_value  = in SInt(21 bits)                  // immediate value
    val rs1_value  = in Bits(AppleRISCVCfg.XLEN bits)  // register rs1 value
    val rs2_value  = in Bits(AppleRISCVCfg.XLEN bits)  // register rs1 value
    val bu_opcode  = in (BranchOpcodeEnum())           // branch/jump control input
    val br_op      = in Bool                           // We get branch instruction
    val jal_op     = in Bool                           // We get jump instruction
    val jalr_op    = in Bool
    val is_branch  = out Bool
    val take_branch = out Bool
    val target_pc   = out UInt(AppleRISCVCfg.XLEN bits)
    val ex_stage_valid = in Bool
    val exc_instr_addr_ma = out Bool
    val branch_should_take = out Bool
    val pred_take  = if (AppleRISCVCfg.USE_BPU) in Bool else null
    val pred_pc    = if (AppleRISCVCfg.USE_BPU) in UInt(AppleRISCVCfg.XLEN bits) else null
  }
  noIoPrefix()

  // Address Calculation
  // Note: JALR instruction needs to set the target address lsb to zero.
  // Here we just blindly set the lsb to zero for the following reason:
  // 1. We only support RV32 so our PC should be aligned to word boundary.
  // 2. The immediate value for branch and jal instruction has its lsb already set to zero.
  // For jalr, rs1 value is used, others use pc
  val real_target_pc = Mux(io.jalr_op, io.rs1_value.asUInt, io.current_pc) + io.imm_value.resize(AppleRISCVCfg.XLEN bits).asUInt
  real_target_pc(0) := False
  val current_pc_plus4 = io.current_pc + 4

  // check the branch result
  val take_branch = False
  val beq  = io.rs1_value === io.rs2_value
  val bge  = io.rs1_value.asSInt >= io.rs2_value.asSInt
  val bgeu = io.rs1_value.asUInt >= io.rs2_value.asUInt
  switch(io.bu_opcode) {
    is(BranchOpcodeEnum.BEQ)  {take_branch := beq}
    is(BranchOpcodeEnum.BNE)  {take_branch := !beq}
    is(BranchOpcodeEnum.BGE)  {take_branch := bge}
    is(BranchOpcodeEnum.BLT)  {take_branch := !bge}
    is(BranchOpcodeEnum.BGEU) {take_branch := bgeu}
    is(BranchOpcodeEnum.BLTU) {take_branch := !bgeu}
  }

  io.is_branch := io.ex_stage_valid & (io.jal_op | io.jalr_op | io.br_op)
  io.exc_instr_addr_ma := (io.take_branch  & (io.target_pc(1 downto 0) =/= 0))

  // Branch Prediction Check:
  // Predict wrong includes both the direction and the address.
  // For direction, we check against the predicted result and calculated result.
  // For address:
  //     If the branch should take and the address is wrong, then we also predict wrong.
  //     If the branch should not take then we don't need to look at the address because.
  io.branch_should_take := io.jal_op | io.jalr_op | (io.br_op & take_branch)
  val pred_wrong = if (AppleRISCVCfg.USE_BPU) (io.branch_should_take ^ io.pred_take) | (io.branch_should_take & (io.pred_pc =/= real_target_pc)) else null

  // We only need to take the branch/jump if we predict wrong
  // because we would been already jumped if we predict correct.
  val branch_taken_final = if (AppleRISCVCfg.USE_BPU) pred_wrong & io.is_branch else io.branch_should_take & io.ex_stage_valid
  io.take_branch  := branch_taken_final

  // For the target PC address if the branch should take, then we should use the target pc address
  // if the branch should not take, then we should use the current pc + 4
  val target_pc_final = if (AppleRISCVCfg.USE_BPU) Mux(io.branch_should_take, real_target_pc, current_pc_plus4) else real_target_pc
  io.target_pc := target_pc_final
}
