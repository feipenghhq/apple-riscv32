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
    val rtc_irq = out Bool
  }
  noIoPrefix()
  val busCtrl = SibSlaveFactory(io.aon_sib)

  // =============================================
  // Real Time Clock
  // =============================================
  val RTC = new Area {
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
    rtcs := (rtcfull >> rtcscale).resized
    rtcmpip := rtcs > rtccmp
    when(rtcenalways) {rtcfull := rtcfull + 1}
    io.rtc_irq := rtcmpip & rtcenalways
  }

}
