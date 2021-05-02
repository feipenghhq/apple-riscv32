///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: EX
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Execution Stage
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._

case class Ex2MemBD() extends Bundle with IMasterSlave {
  val pc        = out UInt(AppleRISCVCfg.xlen bits)
  val rdWrCtrl  = master(RdWrStage())
  val rdSelCtrl = out (RdSelEnum())
  val dmemCtrl  = master(DmemCtrlStage())
  val op2Data   = out Bits(AppleRISCVCfg.xlen bits) // depending on Instruction, op2 can be imm, rs2
  val aluOut    = out Bits(AppleRISCVCfg.xlen bits)

  override def asMaster(): Unit = {
    master(rdWrCtrl, dmemCtrl)
    out(pc, aluOut)
  }
}

case class EX() extends Component {

  val io = new Bundle {
    // input
    val id2ex = slave(Id2ExBD())
    val exStageCtrl = slave(StageCtrlBD())
    val rs1DataMem = in Bits(AppleRISCVCfg.xlen bits)
    val rs2DataMem = in Bits(AppleRISCVCfg.xlen bits)
    val rs1DataWb = in Bits(AppleRISCVCfg.xlen bits)
    val rs2DataWb = in Bits(AppleRISCVCfg.xlen bits)
    // output
    val bu2pc  = master(Bu2pcBD())
    val ex2mem = master(Ex2MemBD())
  }
  noIoPrefix()

  //=========================
  // Data Bypassing MUX
  //=========================
  val op1BypassMux = new Area {
    val op1final = Bits(AppleRISCVCfg.xlen bits)
    when(io.id2ex.rs1BypassCtrl === BypassCtrlEnum.MEM) {
      op1final := io.rs1DataMem
    }.elsewhen(io.id2ex.rs1BypassCtrl === BypassCtrlEnum.WB) {
      op1final := io.rs1DataWb
    }.otherwise{
      op1final := io.id2ex.op1Data
    }
  }
  val op2BypassMux = new Area {
    val op2final = Bits(AppleRISCVCfg.xlen bits)
    when(io.id2ex.rs2BypassCtrl === BypassCtrlEnum.MEM) {
      op2final := io.rs2DataMem
    }.elsewhen(io.id2ex.rs2BypassCtrl === BypassCtrlEnum.WB) {
      op2final := io.rs2DataWb
    }.otherwise{
      op2final := io.id2ex.op2Data
    }
  }

  //=========================
  // ALU & BU
  //=========================
  val alu = ALU()
  val bu  = BU()

  alu.io.aluCtrl <> io.id2ex.aluCtrl
  alu.io.operand1 <> op1BypassMux.op1final
  alu.io.operand2 <> op2BypassMux.op2final

  bu.io.buCtrl <> io.id2ex.buCtrl
  bu.io.exPc <> io.id2ex.pc
  bu.io.immValue <> io.id2ex.immValue(20 downto 0)
  bu.io.rs1Value <> io.id2ex.op1Data
  bu.io.rs2Value <> io.id2ex.op2Data
  bu.io.exStageCtrl <> io.exStageCtrl
  bu.io.bu2pc <> io.bu2pc

  //==========================
  // Pipeline Stage
  //==========================
  ccPipeStage(io.id2ex.pc, io.ex2mem.pc)(io.exStageCtrl)
  ccPipeStage(alu.io.aluOut, io.ex2mem.aluOut)(io.exStageCtrl)
  ccPipeStage(op2BypassMux.op2final, io.ex2mem.op2Data)(io.exStageCtrl)
  ccPipeStage(io.id2ex.dmemCtrl, io.ex2mem.dmemCtrl)(io.exStageCtrl)
  ccPipeStage(io.id2ex.rdWrCtrl, io.ex2mem.rdWrCtrl)(io.exStageCtrl)
  ccPipeStage(io.id2ex.rdSelCtrl, io.ex2mem.rdSelCtrl)(io.exStageCtrl)
}

object EXMain {
  def main(args: Array[String]) {
    SpinalVerilog(new EX).printPruned()
  }
}