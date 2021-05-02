///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: gpio
//
// Author: Heqing Huang
// Date Created: 04/21/2021
//
// ================== Description ==================
//
// General Purpose IO, currently only supporting output signal
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._
import spinal.lib.io.TriStateArray

// GPIO config
class GpioCfg(
  val HI_INT: Boolean = true,
  val LO_INT: Boolean = true,
  val RISE_INT: Boolean = true,
  val FALL_INT: Boolean = true,
  val GPIO_WIDTH : Int  = 32
)

case class gpio_io(cfg: GpioCfg, sibCfg: SibConfig, useInt: Boolean = true) extends Bundle {
  val gpio         = master(TriStateArray(cfg.GPIO_WIDTH bits))
  val gpio_sib     = slave(Sib(sibCfg))
  val gpio_int_pe  = if (useInt) out Bool else null
}

case class gpio_interrupt_gen(busCtrl: SibSlaveFactory, width: Int, enable: Boolean, addr: Int,
                          intName: String, gpio_value: Vec[Bool], int_op: Bool => Bool) extends Area {
  val int_aggr = False
  if (enable) {
    // Interrupt Ctrl and Status Register
    val int_en = busCtrl.createReadAndWrite(Bits(width bits), addr    , 0, "GPIO" + intName + "Interrupt Enable")
    val int_pe = busCtrl.createReadAndWrite(Bits(width bits), addr + 4, 0, "GPIO" + intName + "Interrupt Pending")
    // Interrupt Logic
    int_pe := gpio_value.map(int_op).asBits
    int_aggr := int_pe.orR
  }
}

case class Gpio(cfg: GpioCfg, sibCfg: SibConfig) extends Component {

  noIoPrefix()
  val io        = gpio_io(cfg, sibCfg)
  val busCtrl   = SibSlaveFactory(io.gpio_sib)

  val gpio_value = io.gpio.read.asBools

  // == Register == //
  busCtrl.read        (io.gpio.read       , 0x0, 0, "GPIO Read Value")
  busCtrl.drive       (io.gpio.write      , 0x0, 0, "GPIO Write Value")
  busCtrl.driveAndRead(io.gpio.writeEnable, 0x4, 0, "GPIO Write Enable")

  val hi_int = gpio_interrupt_gen(busCtrl, cfg.GPIO_WIDTH, cfg.HI_INT, 0x8,  "Level HI",    gpio_value, x => x)
  val lo_int = gpio_interrupt_gen(busCtrl, cfg.GPIO_WIDTH, cfg.LO_INT, 0x10, "Level LO",    gpio_value, x => ~x)
  val ri_int = gpio_interrupt_gen(busCtrl, cfg.GPIO_WIDTH, cfg.LO_INT, 0x18, "Rising Edge", gpio_value, x => x.rise(False))
  val fa_int = gpio_interrupt_gen(busCtrl, cfg.GPIO_WIDTH, cfg.LO_INT, 0x20, "Rising Edge", gpio_value, x => x.fall(False))

  io.gpio_int_pe := hi_int.int_aggr | lo_int.int_aggr | ri_int.int_aggr | fa_int.int_aggr
}
