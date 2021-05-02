///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: IF
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Instruction Fetch Stage
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import AppleRISCVSoC.Bus._
import spinal.core._
import spinal.lib._

case class If2IdBD() extends Bundle with IMasterSlave {
  val valid = out Bool
  val instr = out Bits(AppleRISCVCfg.xlen bits)
  val pc = out UInt(AppleRISCVCfg.xlen bits)
  override def asMaster(): Unit = {
    out(instr, pc)
  }
}

case class IF() extends Component {

  val io = new Bundle {
    // input from other stage
    val trapCtrl2pc  = slave(TrapCtrl2pcBD())
    val bu2pc        = slave(Bu2pcBD())
    val ifStageCtrl = slave(StageCtrlBD())

    // output
    val if2id   = master(If2IdBD())
    val imemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
  }
  noIoPrefix()

  //=================================
  // PC & Instruction RAM controller
  //=================================
  val pc = PC()
  val imemCtrl = ImemCtrl()
  imemCtrl.io.imemSib <> io.imemSib
  imemCtrl.io.pc2imemAddr <> pc.io.pc2imemAddr
  imemCtrl.io.ifStageCtrl <> io.ifStageCtrl

  pc.io.bu2pc <> io.bu2pc
  pc.io.trapCtrl2pc <> io.trapCtrl2pc
  pc.io.ifStageCtrl <> io.ifStageCtrl

  //==========================
  // Pipeline stage
  //==========================
  ccPipeStage(pc.io.pcOut, io.if2id.pc)(io.ifStageCtrl)
  ccPipeStage(!io.ifStageCtrl.flush, io.if2id.valid)(io.ifStageCtrl)
  io.if2id.instr := imemCtrl.io.imemInstr // No need pipeline for instruction
}

object IFMain {
  def main(args: Array[String]) {
    SpinalVerilog(new IF).printPruned()
  }
}