///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: De2_top
//
// Author: Heqing Huang
// Date Created: 06/08/2021
//
// ================== Description ==================
//
// The ScC top level for DE2 FPGA Board
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package Board

import AppleRISCV._
import AppleRISCVSoC._
import IP.Sram
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.Uart
import spinal.lib.io._

case class De2_top() extends Component {

  val io = new Bundle {
    val CLK   = in Bool
    val RESET = in Bool
    val UART0 = master(Uart())
    val SRAM  = master(Sram())
    val KEY   = in Bits(4 bits)     // GPIO 0-3
    val SW    = in Bits(16 bits)    // GPIO 4-19
    val LEDR  = out Bits(12 bits)   // GPIO 20-31
    val LEDG  = out Bits(4 bits)    // PWM
    val LD    = in Bool
    //val GPIO = master(TriStateArray(SoCCfg.gpio0Width bits))
    val PWM0 = out Bits(4 bits)
    val TD_RESET = out Bool
  }
  noIoPrefix()

  // Clock domain and PLL
  val clkCtrl = new Area {
    //Create a new clock domain named 'core'
    val socClockDomain = ClockDomain.internal(
      name = "soc",
      frequency = FixedFrequency(27 MHz),
      config = ClockDomainConfig(
        clockEdge        = RISING,
        resetKind        = SYNC,
        resetActiveLevel = HIGH
      )
    )

    socClockDomain.clock := io.CLK
    // button create low level when pressed
    socClockDomain.reset := ResetCtrl.asyncAssertSyncDeassert(~io.RESET, socClockDomain)
  }

  val top = new ClockingArea(clkCtrl.socClockDomain) {
    val de2SoC = De2SoC(this.clockDomain.frequency.getValue)
    de2SoC.io.clk := clkCtrl.socClockDomain.clock
    de2SoC.io.reset := clkCtrl.socClockDomain.reset
    de2SoC.io.uart0 <> io.UART0
    de2SoC.io.sram <> io.SRAM
    de2SoC.io.load_imem <> io.LD
    if(SoCCfg.USE_PWM0) de2SoC.io.pwm0 <> io.PWM0
    if(SoCCfg.USE_PWM0)  de2SoC.io.pwm0 <> io.LEDG

    // GPIO connection
    for (i <- 0 until 4) {
      if(SoCCfg.USE_GPIO) de2SoC.io.gpio(i).read := io.KEY(i)
    }
    val SW_base = 4
    for (i <- 0 until 16) {
      if(SoCCfg.USE_GPIO) de2SoC.io.gpio(i+SW_base).read := io.SW(i)
    }
    val LEDR_base = 20
    for (i <- 0 until 12) {
      if(SoCCfg.USE_GPIO) io.LEDR(i) := de2SoC.io.gpio(i+LEDR_base).write
      if(SoCCfg.USE_GPIO) de2SoC.io.gpio(i+LEDR_base).read := False
    }

    io.TD_RESET := True

  }
}

object De2_topMain{
  def main(args: Array[String]) {
    // CPU Configuration
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = false
    CsrCfg.USE_MHPMC3         = true
    CsrCfg.USE_MHPMC4         = true
    SpinalVerilog(InOutWrapper(De2_top()))
  }
}