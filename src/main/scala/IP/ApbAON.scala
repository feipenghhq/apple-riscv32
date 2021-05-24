///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: AON
//
// Author: Heqing Huang
// Date Created: 05/10/2021
// Version 1: 05/23/2021
//
// ================== Description ==================
//
// AON - Always-On Domain
//
// Revision 1:
//  - Use APB as bus interface
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._

case class ApbAON(apbCfg: Apb3Config) extends Component {

  val io = new Bundle {
    val apb = slave(Apb3(apbCfg))
    // reset
    val uartdbgrst_req = in Bool
    val corerst = out Bool
    // interrupt
    val rtc_irq = out Bool
  }
  noIoPrefix()
  val busCtrl = Apb3SlaveFactory(io.apb)

  // =============================================
  // Reset Unit
  // =============================================
  val RU = new Area {
    io.corerst := RegNext(io.uartdbgrst_req) init True
  }

  // =============================================
  // Real Time Clock
  // =============================================
  val RTC = new Area {

    // ===================
    // Clock Divider
    // ===================
    // Divide the clock to get 32.768khz clock for the RTC
    val rtc_clk_freq = 32.768 kHz
    val cnt_value = (clockDomain.frequency.getValue / rtc_clk_freq).toInt
    val clk_div = Reg(UInt(log2Up(cnt_value) bits)) init 0
    val clk_div_tick = clk_div === cnt_value
    when(clk_div_tick) {clk_div := 0}.otherwise {clk_div := clk_div + 1}

    val rtcs = UInt(32 bits)
    val rtcfull = Reg(UInt(48 bits))

    // ===================
    // Register
    // ===================
    // RTC Configuration Register (rtccfg) - 0x40
    val rtcscale = busCtrl.createReadAndWrite(UInt(4 bits), 0x040, 0, "RTC clock rate scale")
    val rtcenalways = busCtrl.createReadAndWrite(Bool, 0x040, 12, "RTC counter enable") init False
    val rtcmpip = RegInit(False)
    busCtrl.read(rtcmpip, 0x040, 28, "RTC comparator interrupt pending")
    // RTC Counter Register Low (rtclo) - 0x48
    busCtrl.readAndWrite(rtcfull(31 downto 0), 0x048, 0, "RTC counter register, low bits")
    // RTC Counter Register High (rtchi) - 0x4C
    busCtrl.readAndWrite(rtcfull(47 downto 32), 0x04C, 0, "RTC counter register, high bits")
    // RTC Counter Register Selected - 0x50
    busCtrl.read(rtcs, 0x050, 0, "RTC Counter Register Selected")
    // RTC Counter Compare Register (rtccmp) - 0x60
    val rtccmp = busCtrl.createReadAndWrite(UInt(32 bits), 0x060, 0, "RTC counter comparison value")

    // ===================
    // Logic
    // ===================
    rtcs    := (rtcfull >> rtcscale).resized
    rtcmpip := (rtcs > rtccmp) & rtcenalways
    when(rtcenalways & clk_div_tick) {rtcfull := rtcfull + 1}
    io.rtc_irq := rtcmpip & rtcenalways
  }

  // =============================================
  // Watchdog Timer (WDT)
  // =============================================
  // TBD
}
