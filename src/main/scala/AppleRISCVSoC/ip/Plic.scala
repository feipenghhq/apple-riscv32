///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: plic
//
// Author: Heqing Huang
// Date Created: 04/19/2021
//
// ================== Description ==================
//
// Platform Level Interrupt Controller
//
// A very basic and simple PLIC, not interrupt priority and arbitration.
// Simply oring all the interrupt and generate the external interrupt
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._

case class plic_io(sibCfg: SibConfig) extends Bundle {
  val plic_sib  = slave(Sib(sibCfg))
  val timer_int = in Bool
  val uart_int  = in Bool
  val gpio0_int = in Bool
  val gpio1_int = in Bool
  val external_interrupt = out Bool
}

case class plic_interrupt_gen(busCtrl: SibSlaveFactory, width: Int, addr: Int, interrupt: Bits, intName: String) extends Area {

  // Interrupt Ctrl and Status Register
  val int_en = busCtrl.createReadAndWrite(Bits(width bits), addr    , 0, "PLIC" + intName + "Interrupt Enable")
  val int_pe = busCtrl.createReadAndWrite(Bits(width bits), addr + 4, 0, "PLIC" + intName + "Interrupt Pending")
  // Interrupt Logic
  int_pe := interrupt
  val int_aggr = int_pe.orR

}

case class Plic(sibCfg: SibConfig) extends Component {

  noIoPrefix()

  val io = plic_io(sibCfg)
  val busCtrl  = SibSlaveFactory(io.plic_sib)

  val timer_int = plic_interrupt_gen(busCtrl, 1, 0, io.timer_int.asBits, "Timer")
  val uart_int  = plic_interrupt_gen(busCtrl, 1, 8, io.uart_int.asBits,   "Uart")
  val gpio0_int = plic_interrupt_gen(busCtrl, 1, 16, io.gpio0_int.asBits, "GPIO0")
  val gpio1_int = plic_interrupt_gen(busCtrl, 1, 24, io.gpio1_int.asBits, "GPIO1")

  io.external_interrupt := gpio0_int.int_aggr | gpio1_int.int_aggr | timer_int.int_aggr | uart_int.int_aggr
}
