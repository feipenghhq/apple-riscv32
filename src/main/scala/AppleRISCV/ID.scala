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
  val valid     = out Bool
  val pc        = out UInt(AppleRISCVCfg.xlen bits)
  val rdWrCtrl  = master(RdWrStage())
  val rdSelCtrl = out (RdSelEnum())
  val aluCtrl   = master(AluCtrlStage())
  val buCtrl    = master(BuCtrlStage())
  val dmemCtrl  = master(DmemCtrlStage())
  val op1Data   = out Bits(AppleRISCVCfg.xlen bits) // depending on Instruction, op1 can be 0, pc, rs1
  val rs2Data   = out Bits(AppleRISCVCfg.xlen bits)
  val immValue  = out SInt(AppleRISCVCfg.xlen bits)
  val immSel    = out Bool
  val op1BypassCtrl = out (BypassCtrlEnum)
  val rs2BypassCtrl = out (BypassCtrlEnum)
  val excIllegalInstr = out Bool
  override def asMaster(): Unit = {
    master(rdWrCtrl, aluCtrl, buCtrl, dmemCtrl)
    out(pc, excIllegalInstr)
  }
}

case class ID() extends Component {

  val io = new Bundle {
    // input
    val if2id = slave(If2IdBD())
    val idStageCtrl = slave(StageCtrlBD())
    val rdWrCtrl  = slave(RdWrStage())
    val rdWdata   = in Bits(AppleRISCVCfg.xlen bits)
    val exRdWrCtrl = slave(RdWrStage())
    val memRdWrCtrl = slave(RdWrStage())
    // output
    val id2ex = master(Id2ExBD())
    val rsDepEx = out Bool
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
  regFile.io.rdWdata   <> io.rdWdata

  //====================
  // Forwarding Unit
  //====================
  val FU = new Area {
    // Note: we are checking the dependency at ID stage to save time on EX stage
    // So when we match with the current Ex stage, we need to forward to EX stage from MEM stage in next cycle
    // And when we match with the current Mem stage, we need to forward to EX stage from WB stage in next cycle
    val rs1DepEx = (instrDec.io.rs1RdCtrl.addr === io.exRdWrCtrl.addr) & instrDec.io.rs1RdCtrl.rd & io.exRdWrCtrl.wr
    val rs1DepMem = (instrDec.io.rs1RdCtrl.addr === io.memRdWrCtrl.addr) & instrDec.io.rs1RdCtrl.rd & io.memRdWrCtrl.wr
    val op1BypassCtrl = BypassCtrlEnum()
    when(rs1DepEx) {
      op1BypassCtrl := BypassCtrlEnum.MEM
    }.elsewhen(rs1DepMem) {
      op1BypassCtrl := BypassCtrlEnum.WB
    }.otherwise{
      op1BypassCtrl := BypassCtrlEnum.NONE
    }
    val rs2DepEx = (instrDec.io.rs2RdCtrl.addr === io.exRdWrCtrl.addr) & instrDec.io.rs2RdCtrl.rd & io.exRdWrCtrl.wr
    val rs2DepMem = (instrDec.io.rs2RdCtrl.addr === io.memRdWrCtrl.addr) & instrDec.io.rs2RdCtrl.rd & io.memRdWrCtrl.wr
    val rs2BypassCtrl = BypassCtrlEnum()
    when(rs2DepEx) {
      rs2BypassCtrl := BypassCtrlEnum.MEM
    }.elsewhen(rs2DepMem) {
      rs2BypassCtrl := BypassCtrlEnum.WB
    }.otherwise{
      rs2BypassCtrl := BypassCtrlEnum.NONE
    }
  }

  //====================
  // ALU Operand 1 Mux
  //====================
  // We use ALU to calculate result for LUI (OP1 = 0, OP2 = imm value) and AUIPC (OP1 = PC, OP2 = imm value)
  // So there are three possible cases for ALU operand 1: rs1Data, 0, and AUIPC.
  // In order to save timing in EX stage, we do the muxing logic between rs1Data, 0, and AUIPC value here.
  // And when we select 0, or AUIPC as op1, the rs1_read signal will not be set, so the forwarding logic in
  // EX stage will not override the data we have selected here.
  val op1Mux = new Area {
    val op1Data = Bits(AppleRISCVCfg.xlen bits)
    when(instrDec.io.op1Ctrl === Op1CtrlEnum.ZERO) {
      op1Data := 0
    }.elsewhen(instrDec.io.op1Ctrl === Op1CtrlEnum.PC) {
      op1Data :=io.if2id.pc.asBits
    }.otherwise{
      op1Data := regFile.io.rs1Data
    }
  }

  // check if rs1 or rs2 depends with ex
  io.rsDepEx := FU.rs1DepEx | FU.rs2DepEx

  //==========================
  // Pipeline Stage
  //==========================
  ccPipeStage(!io.idStageCtrl.flush, io.id2ex.valid)(io.idStageCtrl)
  ccPipeStage(io.if2id.pc, io.id2ex.pc)(io.idStageCtrl)
  ccPipeStage(op1Mux.op1Data, io.id2ex.op1Data)(io.idStageCtrl)
  ccPipeStage(regFile.io.rs2Data, io.id2ex.rs2Data)(io.idStageCtrl)
  ccPipeStage(instrDec.io.immValue, io.id2ex.immValue)(io.idStageCtrl)
  ccPipeStage(instrDec.io.immSel, io.id2ex.immSel)(io.idStageCtrl)
  ccPipeStage(instrDec.io.rdWrCtrl, io.id2ex.rdWrCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.rdSelCtrl, io.id2ex.rdSelCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.aluCtrl, io.id2ex.aluCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.buCtrl, io.id2ex.buCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.dmemCtrl, io.id2ex.dmemCtrl)(io.idStageCtrl)
  ccPipeStage(FU.op1BypassCtrl, io.id2ex.op1BypassCtrl)(io.idStageCtrl)
  ccPipeStage(FU.rs2BypassCtrl, io.id2ex.rs2BypassCtrl)(io.idStageCtrl)
  ccPipeStage(instrDec.io.excIllegalInstr, io.id2ex.excIllegalInstr)(io.idStageCtrl)
}

object IDMain {
  def main(args: Array[String]) {
    SpinalVerilog(new ID).printPruned()
  }
}