///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Generator
//
// Author: Heqing Huang
// Date Created: 05/23/2021
//
// ================== Description ==================
//
// Apple RISCV SoC Generator
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC

import spinal.core._
import spinal.lib._
import IP._
import spinal.lib.bus.amba3.ahblite._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.uart.Uart
import spinal.lib.io.TriStateArray

import scala.collection.mutable.ArrayBuffer

object SoCCfg {
  var gpio0Width = 12
  def gpio0Cfg(): GpioCfg = GpioCfg(HI_INT = true, LO_INT = true, RISE_INT = true, FALL_INT = true, gpio0Width)

  var uartDbgBaudRate = 115200

  var USE_UART0 = true
  var USE_GPIO = true
  var USE_PWM0 = true
}

/** The Basic SoC
 *  The basic SoC consist of the following component and bus
 *  1. CPU core,                      2 AHB Master
 *  2. Uart Debug Module,             1 AHB Master
 *  3. Instruction Memory Subsystem,  1 AHB Slave
 *  4. Data Memory Subsystem,         1 AHB Slave
 *  5. AHB Crossbar
 *  6. AHB2APB Slave
 *  7. APB Decoder
 */
case class Ahblite3crossbar(cfg: AhbLite3Config) extends Component {

  val io = new Bundle {
    val dbg_ahb  = slave(AhbLite3Master(cfg))
    val ibus_ahb = slave(AhbLite3Master(cfg))
    val dbus_ahb = slave(AhbLite3Master(cfg))
    val imem_ahb = master(AhbLite3(cfg))
    val dmem_ahb = master(AhbLite3(cfg))
    val ahb2apb_ahb = master(AhbLite3(cfg))
  }

  val crossbar = AhbLite3CrossbarFactory(cfg)

  crossbar.addSlaves(
    io.imem_ahb -> SoCAddrMapping.IMEM.sizeMapping(),
    io.dmem_ahb -> SoCAddrMapping.DMEM.sizeMapping(),
    io.ahb2apb_ahb -> SoCAddrMapping.PERIP.sizeMapping()
  )

  crossbar.addConnections(
    io.dbg_ahb.toAhbLite3() -> List(io.imem_ahb),
    io.ibus_ahb.toAhbLite3() -> List(io.imem_ahb),
    io.dbus_ahb.toAhbLite3() -> List(io.imem_ahb, io.dmem_ahb, io.ahb2apb_ahb)
  )

  crossbar.build()
}

case class Peripherals(cpu_rst: Bool,
                       uart_en: Bool,
                       uartdbgrst_req: Bool,
                       external_interrupt: Bool,
                       timer_interrupt: Bool,
                       software_interrupt: Bool,
                       _uart0: Uart,
                       _pwm0: Bits,
                       _gpio: TriStateArray,
                       ahb2apb_ahb: AhbLite3
                      ) extends Area {

  val apbDecList  = ArrayBuffer[(Apb3, SizeMapping)]()

  // CLIC
  val clic = ApbClic(ApbCfg.clicApbCfg())
  apbDecList.append((clic.io.apb, SoCAddrMapping.CLIC.sizeMapping()))
  timer_interrupt     := clic.io.timer_irq
  software_interrupt  := clic.io.software_irq

  // PLIC
  val plic = ApbPlic(ApbCfg.plicApbCfg())
  apbDecList.append((plic.io.apb, SoCAddrMapping.PLIC.sizeMapping()))
  plic.io.plic_irq_in := 0
  external_interrupt  := plic.io.external_irq

  val rtc_irq_base   = 2
  val uart0_irq_base = 3
  val gpio_irq_base  = 8
  val pwm0_irq_base  = 40

  // AON
  val aon = ApbAON(ApbCfg.aonApbCfg())
  apbDecList.append((aon.io.apb, SoCAddrMapping.AON.sizeMapping()))
  cpu_rst := aon.io.corerst
  aon.io.uartdbgrst_req := uartdbgrst_req
  plic.io.plic_irq_in(rtc_irq_base) := aon.io.rtc_irq

  // UART0
  if (SoCCfg.USE_UART0) {
    val uart0 = SibUart(ApbCfg.uart0ApbCfg())
    apbDecList.append((uart0.io.apb, SoCAddrMapping.UART0.sizeMapping()))
    uart0.io.en := uart_en
    uart0.io.uart <> _uart0
    val uart_irq = uart0.io.rxwm | uart0.io.txwm
    plic.io.plic_irq_in(uart0_irq_base) := uart_irq
  }

  // GPIO0
  if (SoCCfg.USE_GPIO) {
    val gpio = Gpio(SoCCfg.gpio0Cfg(), ApbCfg.gpioApbCfg())
    apbDecList.append((gpio.io.apb, SoCAddrMapping.GPIO.sizeMapping()))
    gpio.io.gpio <> _gpio
    for (idx <- 0 until gpio.io.gpio_irq.getBitsWidth) {
      plic.io.plic_irq_in(gpio_irq_base+idx) := gpio.io.gpio_irq(idx)
    }
  }

  // PWM0
  if (SoCCfg.USE_PWM0) {
    val pwm0 = ApbPWM(8, ApbCfg.pwm0ApbCfg())
    apbDecList.append((pwm0.io.apb, SoCAddrMapping.PWM0.sizeMapping()))
    pwm0.io.pwmcmpgpio <> _pwm0
    for (idx <- 0 until pwm0.io.pwmcmpip.getBitsWidth)
      plic.io.plic_irq_in(pwm0_irq_base+idx) := pwm0.io.pwmcmpip(idx)
  }

  // AHBLite3 to APB Bridge
  val ahb2apb = AhbLite3ToApb3Bridge(
    ahbConfig = AhbLite3Cfg.peripAhblite3Cfg(),
    apbConfig = ApbCfg.peripApbCfg()
  )
  ahb2apb.io.ahb <> ahb2apb_ahb.remapAddress(addr => addr.resize(ahb2apb.ahbConfig.addressWidth))

  // APB Decoder
  val apb_decoder = Apb3Decoder(
    master = ahb2apb.io.apb,
    slaves = apbDecList
  )

}
