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
    // input
    val loadDependence = in Bool
    val branch = in Bool

    // stage ctrl output
    val ifStageCtrl = master(StageCtrlBD())
    val idStageCtrl = master(StageCtrlBD())
    val exStageCtrl = master(StageCtrlBD())
    val memStageCtrl = master(StageCtrlBD())
  }
  noIoPrefix()

  // Flushing decision
  val ifFlush = io.branch
  val idFlush = io.branch | io.loadDependence
  val exFlush = False
  val memFlush = False

  // stall decision
  val ifStall = io.loadDependence
  val idStall = False
  val exStall = False
  val memStall = False


  // decision logic
  io.ifStageCtrl.flush := ifFlush
  io.ifStageCtrl.enable := !ifStall

  io.idStageCtrl.flush := idFlush
  io.idStageCtrl.enable := !idStall

  io.exStageCtrl.flush := exFlush
  io.exStageCtrl.enable := !exStall

  io.memStageCtrl.flush := memFlush
  io.memStageCtrl.enable := !memStall
}
