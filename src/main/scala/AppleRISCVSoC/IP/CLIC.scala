///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: clic
//
// Author: Heqing Huang
// Date Created: 04/19/2021
//
// ================== Description ==================
//
// Core Level Interrupt Controller
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.IP

import AppleRISCVSoC.Bus._
import spinal.core._
import spinal.lib._

case class CLIC(sibCfg: SibConfig, timerWidth: Int) extends Component {

  val io = new Bundle {
    val clic_sib           = slave(Sib(sibCfg))
    val software_interrupt = out Bool
    val timer_interrupt    = out Bool
  }

  noIoPrefix()
  val busCtrl  = SibSlaveFactory(io.clic_sib)


  val msip        = busCtrl.createReadAndWrite(Bool, 0, 0,
      "MSIP Register, used to trigger software interrupt") init False
  val mtime       = busCtrl.createWriteAndReadMultiWord(UInt(timerWidth bits), 4,
      "Free running timer, when value reaches mtimecmp, timer interrupt will be fired") init 0
  val mtimecmp    = busCtrl.createWriteAndReadMultiWord(UInt(timerWidth bits), 0xC,
                  "Timer comparison register") init 0

  // == timer logic == //
  mtime := mtime + 1

  // == interrupt generation logic == //
  io.software_interrupt := msip
  io.timer_interrupt := (mtime >= mtimecmp) & (mtimecmp =/= 0)
}
