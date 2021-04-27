///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: timer
//
// Author: Heqing Huang
// Date Created: 04/21/2021
//
// ================== Description ==================
//
// Timer
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package ip

import soc._
import bus.sib._
import spinal.core._
import spinal.lib._

case class timer_io(sibCfg: SibConfig) extends Bundle {
  val timer_sib = slave(Sib(sibCfg))
  val timer_interrupt = out Bool
}

case class timer(sibCfg: SibConfig) extends Component {

  noIoPrefix()

  val TimerWidth    = 64
  val io = timer_io(sibCfg)
  val busCtrl  = SibSlaveFactory(io.timer_sib)

  val en       = busCtrl.createReadAndWrite(Bool, 0, 0, "Timer enable") init False
  val int_en   = busCtrl.createReadAndWrite(Bool, 0, 1, "Timer interrupt enable") init False
  val int_pe   = busCtrl.createReadOnly    (Bool, 0, 4, "Timer interrupt pending")
  val timerval = busCtrl.createWriteAndReadMultiWord(UInt(TimerWidth bits), 0x4, "timer value") init 0
  val timercmp = busCtrl.createWriteAndReadMultiWord(UInt(TimerWidth bits), 0xc, "timer comparison value")

  // == timer logic == //
  when(en) {
    timerval := timerval + 1
  }
  // == interrupt generation logic == //
  int_pe := (timerval >= timercmp) & (timercmp =/= 0) & int_en
  io.timer_interrupt := int_pe
}
