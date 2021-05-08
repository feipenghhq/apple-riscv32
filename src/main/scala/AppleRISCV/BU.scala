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
    val pred_take  = if (AppleRISCVCfg.USE_BPB) in Bool else null
    val pred_pc    = if (AppleRISCVCfg.USE_BPB) in UInt(AppleRISCVCfg.XLEN bits) else null
    val bu_opcode  = in (BranchOpcodeEnum())           // branch/jump control input
    val br_op      = in Bool                           // We get branch instruction
    val jal_op     = in Bool                           // We get jump instruction
    val jalr_op    = in Bool
    val ex_stage_valid = in Bool
    val is_branch     = out Bool
    val target_pc     = out UInt(AppleRISCVCfg.XLEN bits)
    val branch_taken  = out Bool
    val exc_instr_addr_ma = out Bool
  }
  noIoPrefix()

  // Address Calculation
  // Note: JALR instruction needs to set the target address lsb to zero.
  // Here we just blindly set the lsb to zero for the following reason:
  // 1. We only support RV32 so our PC should be aligned to word boundary.
  // 2. The immediate value for branch and jal instruction has its lsb already set to zero.
  val preAddr = Mux(io.jalr_op, io.rs1_value.asUInt, io.current_pc)  // For jalr, rs1 value is used, others use pc
  io.target_pc := preAddr + io.imm_value.resize(AppleRISCVCfg.XLEN bits).asUInt
  io.target_pc(0) := False

  // check the branch result
  val takeBranch: Bool = False
  val beq: Bool = io.rs1_value === io.rs2_value
  val bge: Bool = io.rs1_value.asSInt >= io.rs2_value.asSInt
  val bgeu: Bool = io.rs1_value.asUInt >= io.rs2_value.asUInt
  switch(io.bu_opcode) {
    is(BranchOpcodeEnum.BEQ) {takeBranch := beq}
    is(BranchOpcodeEnum.BNE) {takeBranch := !beq}
    is(BranchOpcodeEnum.BGE) {takeBranch := bge}
    is(BranchOpcodeEnum.BLT) {takeBranch := !bge}
    is(BranchOpcodeEnum.BGEU) {takeBranch := bgeu}
    is(BranchOpcodeEnum.BLTU) {takeBranch := !bgeu}
  }

  val branch_should_take = io.ex_stage_valid & (io.jal_op | io.jalr_op | (io.br_op & takeBranch))
  val pred_wrong   = if (AppleRISCVCfg.USE_BPB) (branch_should_take ^ io.pred_take) | (io.pred_pc =/= io.target_pc) else null
  // we only need to branch if we predict wrong.
  val branch_taken = if (AppleRISCVCfg.USE_BPB) branch_should_take & pred_wrong else branch_should_take
  io.branch_taken := branch_taken
  io.is_branch    := io.ex_stage_valid & (io.jal_op | io.jalr_op | io.br_op)

  // Check address misaligned exception
  io.exc_instr_addr_ma := (io.branch_taken & (io.target_pc(1 downto 0) =/= 0))
}
