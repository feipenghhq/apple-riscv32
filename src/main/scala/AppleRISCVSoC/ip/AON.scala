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
//
// ================== Description ==================
//
// AON - Always-On Domain
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._

case class AON(sibCfg: SibConfig) extends Component {

  val io = new Bundle {
    val aon_sib = slave(Sib(sibCfg))
    // reset
    val uart2imem_rst_req = in Bool
    val cpu_rst_out       = out Bool
    // interrupt
    val rtc_irq = out Bool
  }
  noIoPrefix()
  val busCtrl = SibSlaveFactory(io.aon_sib)

  // =============================================
  // Reset Unit
  // =============================================
  val RU = new Area {
    val cpu_rst_req = io.uart2imem_rst_req
    val cpu_reset = RegNext(cpu_rst_req) init True
    io.cpu_rst_out := cpu_reset
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
    val rtcmpip = busCtrl.createReadOnly(Bool, 0x040, 28, "RTC comparator interrupt pending") init False
    // RTC Counter Register Low (rtclo) - 0x48
    busCtrl.read(rtcfull(31 downto 0), 0x048, 0, "RTC counter register, low bits")
    // RTC Counter Register High (rtchi) - 0x4C
    busCtrl.read(rtcfull(47 downto 32), 0x04C, 0, "RTC counter register, high bits")
    // RTC Counter Register Selected - 0x50
    busCtrl.read(rtcs, 0x050, 0, "RTC Counter Register Selected")
    // RTC Counter Compare Register (rtccmp) - 0x60
    val rtccmp = busCtrl.createReadAndWrite(UInt(32 bits), 0x060, 0, "RTC counter comparison value")

    // ===================
    // Logic
    // ===================
    rtcs    := (rtcfull >> rtcscale).resized
    rtcmpip := rtcs > rtccmp
    when(rtcenalways & clk_div_tick) {rtcfull := rtcfull + 1}
    io.rtc_irq := rtcmpip & rtcenalways
  }

  // =============================================
  // Watchdog Timer (WDT)
  // =============================================
  /*
  val WDT = new Area {

    val wdogcount = Reg(UInt(31 bits))
    val wdogcount_inc = Bool
    val wdogcount_clr = Bool

    // ===================
    // Register
    // ===================
    // Watchdog Configuration Register (wdogcfg) - 0x000
    val wdogscale    = busCtrl.createReadAndWrite(UInt(4 bits), 0x000, 0, "Watchdog counter scale")
    val wdogrsten    = busCtrl.createReadAndWrite(Bool, 0x000, 8, "Watchdog full reset enable") init False
    val wdogzerocmp  = busCtrl.createReadAndWrite(Bool, 0x000, 9, "Watchdog zero on comparator")
    val wdogenalwasy = busCtrl.createReadAndWrite(Bool, 0x000, 12, "Watchdog enable couter always") init False
    val wdogencoreawake  = busCtrl.createReadAndWrite(Bool, 0x000, 13, "Watchdog counter only when awake") init False
    val wdogzerocmp  = busCtrl.createReadAndWrite(Bool, 0x000, 28, "Watchdog interrupt pending") init 0

    //  Watchdog Compare Register (wdogcmp)
    val wdogcmp = busCtrl.createReadAndWrite(UInt(16 bits), 0x020, 0, "Watchdog compare value")

    // Watchdog Key Register (wdogkey)
    // Watchdog Feed Address (wdogfeed)

    // ===================
    // Logic
    // ===================
    wdogcount_clr := False
    wdogcount_inc := wdogenalwasy | (wdogencoreawake & ~io.cpu_rst_out)
    when(wdogcount_clr) {wdogcount := 0}.elsewhen(wdogcount_inc) {wdogcount := wdogcount+1}
  }
 */
}
