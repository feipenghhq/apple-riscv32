///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: HDU
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Hazard Detection Unit
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import spinal.core._
import spinal.lib._

case class HDU() extends Component {
  val io = new Bundle {
    val ifStageCtrl = master(StageCtrlBD())
    val idStageCtrl = master(StageCtrlBD())
    val exStageCtrl = master(StageCtrlBD())
    val memStageCtrl = master(StageCtrlBD())
  }
  noIoPrefix()

  io.ifStageCtrl.status := StageCtrlEnum.ENABLE
  io.idStageCtrl.status := StageCtrlEnum.ENABLE
  io.exStageCtrl.status := StageCtrlEnum.ENABLE
  io.memStageCtrl.status := StageCtrlEnum.ENABLE
}
