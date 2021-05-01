///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: PC
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// PC register
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import spinal.core._
import spinal.lib._

//=============================
// Port Declaration
//=============================
case class Pc2imemCtrlBD() extends Bundle with IMasterSlave {
  val addr  = out UInt(AppleRISCVCfg.xlen bits)
  override def asMaster(): Unit = {
    out(addr)
  }
}

case class PcStage() extends Bundle with IMasterSlave {
  val va = out UInt(AppleRISCVCfg.xlen bits)
  override def asMaster(): Unit = {
    out(va)
  }
}

//=============================
// Module
//=============================

case class PC() extends Component {

  val io = new Bundle {
    // input interface
    val trapCtrl2pc  = slave(TrapCtrl2pcBD())
    val bu2pc        = slave(Bu2pcBD())
    val ifStageCtrl  = slave(StageCtrlBD())
    // output interface
    val pc2imemAddr  = out UInt(AppleRISCVCfg.xlen bits)
    val pcOut        = out UInt(AppleRISCVCfg.xlen bits)
  }
  noIoPrefix()

  val pcValue: UInt = Reg(UInt(AppleRISCVCfg.xlen bits)) init 0

  when(io.ifStageCtrl.status =/= StageCtrlEnum.STALL) {
    when(io.bu2pc.branch) {   // stall has higher priority then branch
      pcValue := io.bu2pc.pc
    }.elsewhen(io.trapCtrl2pc.trap) {
      pcValue := io.trapCtrl2pc.pc
    }.otherwise {
      pcValue := pcValue + 4
    }
  }
  io.pc2imemAddr := pcValue
  io.pcOut       := pcValue
}
