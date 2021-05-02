///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: BU
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Branch Unit
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._

/**
 * branch unit to PC
 */
case class Bu2pcBD() extends Bundle with IMasterSlave {
  val branch = in Bool
  val pc     = in UInt(AppleRISCVCfg.xlen bits)
  override def asMaster(): Unit = {
    out(branch, pc)
  }
}

case class BU() extends Component {

  val io = new Bundle {
    val exPc      = in UInt(AppleRISCVCfg.xlen bits)    // pc value for the branch instruction
    val immValue  = in SInt(21 bits)                    // immediate value
    val rs1Value  = in Bits(AppleRISCVCfg.xlen bits)    // register rs1 value
    val rs2Value  = in Bits(AppleRISCVCfg.xlen bits)    // register rs2 value
    val buCtrl    = slave(BuCtrlStage())                // branch/jump control input
    val exStageCtrl = slave(new StageCtrlBD)            // pipeline control signal
    val bu2pc  = master(Bu2pcBD())
    val excInstrAddrMisalign = out Bool
  }
  noIoPrefix()

  // check the branch result
  val takeBranch: Bool = False
  val beq: Bool = io.rs1Value === io.rs2Value
  val bge: Bool = io.rs1Value.asSInt >= io.rs2Value.asSInt
  val bgeu: Bool = io.rs1Value.asUInt >= io.rs2Value.asUInt
  switch(io.buCtrl.branchOp) {
    is(BranchCtrlEnum.BEQ) {takeBranch := beq}
    is(BranchCtrlEnum.BNE) {takeBranch := !beq}
    is(BranchCtrlEnum.BGE) {takeBranch := bge}
    is(BranchCtrlEnum.BLT) {takeBranch := !bge}
    is(BranchCtrlEnum.BGEU) {takeBranch := bgeu}
    is(BranchCtrlEnum.BLTU) {takeBranch := !bgeu}
  }

  val stageValid = io.exStageCtrl.status =/= StageCtrlEnum.FLUSH
  io.bu2pc.branch := stageValid & (io.buCtrl.jal | io.buCtrl.jalr | (io.buCtrl.branch & takeBranch))

  // Address Calculation
  // Note: JALR instruction needs to set the target address lsb to zero.
  // Here we just blindly set the lsb to zero for the following reason:
  // 1. We only support RV32 so our PC should be aligned to word boundary.
  // 2. The immediate value for branch and jal instruction has its lsb already set to zero.
  val preAddr = Mux(io.buCtrl.jalr, io.rs1Value.asUInt, io.exPc)  // For jalr, rs1 value is used, others use pc
  io.bu2pc.pc := preAddr + io.immValue.resize(AppleRISCVCfg.xlen bits).asUInt
  io.bu2pc.pc(0) := False

  // Check address misaligned exception
  io.excInstrAddrMisalign := (io.bu2pc.branch & (io.bu2pc.pc(1 downto 0) =/= 0))
}
