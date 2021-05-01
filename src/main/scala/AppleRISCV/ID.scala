///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: ID
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Instruction Decode Stage
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._


case class Id2ExBD() extends Bundle with IMasterSlave {
  val pc        = out UInt(AppleRISCVCfg.xlen bits)
  val rs1RdCtrl = master(RsCtrlStage())
  val rs2RdCtrl = master(RsCtrlStage())
  val rdWrCtrl  = master(RdWrStage(hasData = false))
  val aluCtrl   = master(AluCtrlStage())
  val buCtrl    = master(BuCtrlStage())
  val dmemCtrl  = master(DmemCtrlStage())
  val op1Data   = out Bits(AppleRISCVCfg.xlen bits) // depending on Instruction, op1 can be 0, pc, rs1
  val op2Data   = out Bits(AppleRISCVCfg.xlen bits) // depending on Instruction, op2 can be imm, rs2
  val immValue  = out SInt(AppleRISCVCfg.xlen bits)
  val rs1BypassCtrl = out (BypassCtrlEnum)
  val rs2BypassCtrl = out (BypassCtrlEnum)
  val excIllegalInstr = out Bool
  override def asMaster(): Unit = {
    master(rs1RdCtrl, rs2RdCtrl, rdWrCtrl, aluCtrl, buCtrl, dmemCtrl)
    out(pc, excIllegalInstr)
  }
}

case class ID() extends Component {

  val io = new Bundle {
    // input
    val if2id = slave(If2IdBD())
    val idStageCtrl = slave(StageCtrlBD())
    val rdWrCtrl  = slave(RdWrStage(hasData = true))
    val exRdWrCtrl = slave(RdWrStage(hasData = false))
    val memRdWrCtrl = slave(RdWrStage(hasData = false))
    // output
    val id2ex = master(Id2ExBD())
  }
  noIoPrefix()

  //=========================
  // Decoder & Register File
  //=========================
  val instrDec = InstrDec()
  val regFile = RegFile()

  instrDec.io.instr    <> io.if2id.instr
  regFile.io.rs1RdCtrl <> instrDec.io.rs1RdCtrl
  regFile.io.rs2RdCtrl <> instrDec.io.rs2RdCtrl
  regFile.io.rdWrCtrl  <> io.rdWrCtrl

  //====================
  // Forwarding Unit
  //====================
  // Note We de
  val FU = new Area {
    // Note: we are checking the dependency at ID stage to save time on EX stage
    // So when we match with the current Ex stage, we need to forward to EX stage from MEM stage in next cycle
    // And when we match with the current Mem stage, we need to forward to EX stage from WB stage in next cycle
    val rs1MatchEx = (instrDec.io.rs1RdCtrl.addr === io.exRdWrCtrl.addr) & instrDec.io.rs1RdCtrl.rd & io.exRdWrCtrl.wr
    val rs1MatchMem = (instrDec.io.rs1RdCtrl.addr === io.memRdWrCtrl.addr) & instrDec.io.rs1RdCtrl.rd & io.memRdWrCtrl.wr
    val rs1BypassCtrl = BypassCtrlEnum()
    when(rs1MatchEx) {
      rs1BypassCtrl := BypassCtrlEnum.MEM
    }.elsewhen(rs1MatchMem) {
      rs1BypassCtrl := BypassCtrlEnum.WB
    }.otherwise{
      rs1BypassCtrl := BypassCtrlEnum.NONE
    }
    val rs2MatchEx = (instrDec.io.rs2RdCtrl.addr === io.exRdWrCtrl.addr) & instrDec.io.rs2RdCtrl.rd & io.exRdWrCtrl.wr
    val rs2MatchMem = (instrDec.io.rs2RdCtrl.addr === io.memRdWrCtrl.addr) & instrDec.io.rs2RdCtrl.rd & io.memRdWrCtrl.wr
    val rs2BypassCtrl = BypassCtrlEnum()
    when(rs2MatchEx) {
      rs2BypassCtrl := BypassCtrlEnum.MEM
    }.elsewhen(rs2MatchMem) {
      rs2BypassCtrl := BypassCtrlEnum.WB
    }.otherwise{
      rs2BypassCtrl := BypassCtrlEnum.NONE
    }
  }

  //====================
  // Operand Mux
  //====================
  // Note: We do some of the muxing logic for the operand1&2
  // here in the ID stage to improving timing in EX stage.
  val op1Mux = new Area {
    val rs1Data = Bits(AppleRISCVCfg.xlen bits)
    when(instrDec.io.op1Ctrl === Op1CtrlEnum.ZERO) {
      rs1Data := 0
    }.elsewhen(instrDec.io.op1Ctrl === Op1CtrlEnum.PC) {
      rs1Data :=io.if2id.pc.asBits
    }.otherwise{
      rs1Data := regFile.io.rs1Data
    }
  }
  val op2Mux = new Area {
    val rs2Data = Bits(AppleRISCVCfg.xlen bits)
    // when we want to use immediate value and we are not using rs2 data
    // then the opcode 2 will be using the immediate value
    when(instrDec.io.immSel && !instrDec.io.rs2RdCtrl.rd) {
      rs2Data := instrDec.io.immValue.asBits
    }.otherwise{
      rs2Data := regFile.io.rs2Data
    }
  }

  //==========================
  // Pipeline Stage
  //==========================
  ccPipeStage(io.if2id.pc, io.id2ex.pc)(io.idStageCtrl)
  ccPipeStage(op1Mux.rs1Data, io.id2ex.op1Data)(io.idStageCtrl)
  ccPipeStage(op2Mux.rs2Data, io.id2ex.op2Data)(io.idStageCtrl)
  ccPipeStage(instrDec.io.immValue, io.id2ex.immValue)(io.idStageCtrl)
  ccPipeStage(instrDec.io.rs1RdCtrl, io.id2ex.rs1RdCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.rs2RdCtrl, io.id2ex.rs2RdCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.rdWrCtrl, io.id2ex.rdWrCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.aluCtrl, io.id2ex.aluCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.buCtrl, io.id2ex.buCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.dmemCtrl, io.id2ex.dmemCtrl)(io.idStageCtrl)
  ccPipeStage(FU.rs1BypassCtrl, io.id2ex.rs1BypassCtrl)(io.idStageCtrl)
  ccPipeStage(FU.rs2BypassCtrl, io.id2ex.rs2BypassCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.excIllegalInstr, io.id2ex.excIllegalInstr)(io.idStageCtrl)
}

object IDMain {
  def main(args: Array[String]) {
    SpinalVerilog(new ID).printPruned()
  }
}