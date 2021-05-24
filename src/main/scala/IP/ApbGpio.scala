///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: ApbGpio
//
// Author: Heqing Huang
// Date Created: 04/21/2021
// Version 1: 05/10/2021
// Version 2: 05/23/2021
//
// ================== Description ==================
//
// General Purpose IO. Compatible with SiFive Freedom E310 SoC
//
// Notes: Not all the registers are implemented
//
//  ---   GPIO Peripheral Offset Registers   ---
// Offset   Name        Description                    Implemented
// 0x000    value       pin value                           Y
// 0x004    input_en    ∗ pin input enable                 N/A
// 0x008    output_en   ∗ pin output enable                 Y
// 0x00C    port        output port value                   Y
// 0x010    pue         ∗ internal pull-up enable          N/A
// 0x014    ds          Pin Drive Strength                 N/A
// 0x018    rise_ie     rise interrupt enable            cfg-able
// 0x01C    rise_ip     rise interrupt pending           cfg-able
// 0x020    fall_ie     fall interrupt enable            cfg-able
// 0x024    fall_ip     fall interrupt pending           cfg-able
// 0x028    high_ie     high interrupt enable            cfg-able
// 0x02C    high_ip     high interrupt pending           cfg-able
// 0x030    low_ie      low interrupt enable             cfg-able
// 0x034    low_ip      low interrupt pending            cfg-able
// 0x038    iof_en      ∗ HW I/O Function enable           N/A
// 0x03C    iof_sel     HW I/O Function select             N/A
//
// Revision 1:
//  - Match registers with SiFive Freedom E310 SoC
//
// Revision 2:
//  - Use APB as bus interface
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.io.TriStateArray


// GPIO config
case class GpioCfg(
  val HI_INT: Boolean = false,
  val LO_INT: Boolean = false,
  val RISE_INT: Boolean = false,
  val FALL_INT: Boolean = false,
  val GPIO_WIDTH: Int = 32)

case class gpio_interrupt(busCtrl: Apb3SlaveFactory, width: Int, enable: Boolean, addr: Int,
                          intName: String, gpio_value: Vec[Bool], int_op: Bool => Bool) extends Area {
    // Interrupt Ctrl and Status Register
    val ie = if (enable) busCtrl.createReadAndWrite(Bits(width bits), addr    , 0, "GPIO" + intName + "Interrupt Enable") init 0 else null
    val ip = if (enable) busCtrl.createReadAndWrite(Bits(width bits), addr + 4, 0, "GPIO" + intName + "Interrupt Pending") init 0 else null
    // Interrupt Logic
    val irq = if (enable) gpio_value.map(int_op).asBits & ie else B(0, width bits)
    for (i <- 0 until ip.getBitsWidth) {
      when (~ip(i) & irq(i)) {ip(i) := True}
    }
}


case class Gpio(cfg: GpioCfg, apbCfg: Apb3Config) extends Component {
  
  val io = new Bundle {
    val gpio     = master(TriStateArray(cfg.GPIO_WIDTH bits))
    val apb      = slave(Apb3(apbCfg))
    val gpio_irq = out Bits(cfg.GPIO_WIDTH bits)
  }
  val busCtrl = Apb3SlaveFactory(io.apb)
  val gpio_value = io.gpio.read.asBools

  // 0x000    value       pin value
  busCtrl.read(io.gpio.read, 0x0, 0, "GPIO pin Value")
  // 0x008    output_en   pin output enable
  busCtrl.driveAndRead(io.gpio.writeEnable, 0x8, 0, "GPIO Output Enable")
  // 0x00C    port        output port value
  busCtrl.drive(io.gpio.write, 0xC, 0, "GPIO Output Port Value")

  // 0x018    rise_ie     rise interrupt enable
  // 0x01C    rise_ip     rise interrupt pending
  val rise = gpio_interrupt(busCtrl, cfg.GPIO_WIDTH, cfg.RISE_INT, 0x018,  "Rise", gpio_value, x => x.rise(False))

  // 0x020    fall_ie     fall interrupt enable            cfg-able
  // 0x024    fall_ip     fall interrupt pending           cfg-able
  val fall = gpio_interrupt(busCtrl, cfg.GPIO_WIDTH, cfg.FALL_INT, 0x020, "Fall",    gpio_value, x => x.fall(False))

  // 0x028    high_ie     high interrupt enable            cfg-able
  // 0x02C    high_ip     high interrupt pending           cfg-able
  val high = gpio_interrupt(busCtrl, cfg.GPIO_WIDTH, cfg.HI_INT, 0x028, "High", gpio_value, x => x)

  // 0x030    low_ie      low interrupt enable             cfg-able
  // 0x034    low_ip      low interrupt pending            cfg-able
  val low  = gpio_interrupt(busCtrl, cfg.GPIO_WIDTH, cfg.LO_INT, 0x030, "Low", gpio_value, x => ~x)

  io.gpio_irq := rise.ip | fall.ip | high.ip | low.ip
}
