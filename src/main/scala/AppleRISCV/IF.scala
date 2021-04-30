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
  val instr = master(ImemInstrStage())
  val pc = master(PcStage())
  override def asMaster(): Unit = {
    master(instr, pc)
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

  val pc = PC()
  val imemCtrl = ImemCtrl()
  imemCtrl.io.imemSib <> io.imemSib
  imemCtrl.io.pc2imemCtrl <> pc.io.pc2imemCtrl
  imemCtrl.io.ifStageCtrl <> io.ifStageCtrl

  pc.io.bu2pc <> io.bu2pc
  pc.io.trapCtrl2pc <> io.trapCtrl2pc
  pc.io.ifStageCtrl <> io.ifStageCtrl

  // Pipeline stage
  ccPipeStage(pc.io.pcOut, io.if2id.pc)
  ccPipeStage(imemCtrl.io.imemInstr, io.if2id.instr)

}

object IFMain {
  def main(args: Array[String]) {
    SpinalVerilog(new IF).printPruned()
  }
}