///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: ArtyA7_top
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// The ScC top level for Arty A7 FPGA Board
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package Board.de2

import AppleRISCV._
import AppleRISCVSoC._
import AppleRISCVSoC.ip._
import spinal.core._
import spinal.lib._
import spinal.lib.com.uart.Uart
import spinal.lib.io._

case class DE2_top() extends Component {

  val cfg = AppleSoCCfg_de2()
  val io = new Bundle {
    val clk = in Bool
    val reset = in Bool
    val sram  = master(SRAMIO())
    val KEY   = in Bits(4 bits)     // GPIO 0-3
    val SW    = in Bits(16 bits)    // GPIO 4-19
    val LEDR  = out Bits(12 bits)   // GPIO 20-31
    val LEDG  = out Bits(4 bits)    // PWM
    val LD    = in Bool
    val uart0 = master(Uart())
  }
  noIoPrefix()

  // Clock domain and PLL
  val clkCtrl = new Area {
    //Create a new clock domain named 'core'
    val coreClockDomain = ClockDomain.internal(
      name = "soc",
      frequency = FixedFrequency(50 MHz),
      config = ClockDomainConfig(
        clockEdge        = RISING,
        resetKind        = SYNC,
        resetActiveLevel = HIGH
      )
    )

    coreClockDomain.clock := io.clk
    // button create low level when pressed
    coreClockDomain.reset := ResetCtrl.asyncAssertSyncDeassert(~io.reset, coreClockDomain)
  }


  val top = new ClockingArea(clkCtrl.coreClockDomain) {

    val AppleSoC_de2_inst = AppleSoC_de2()
    AppleSoC_de2_inst.io.clk    := clkCtrl.coreClockDomain.clock
    AppleSoC_de2_inst.io.reset  := clkCtrl.coreClockDomain.reset
    AppleSoC_de2_inst.io.sram   <> io.sram
    AppleSoC_de2_inst.io.uart0  <> io.uart0
    AppleSoC_de2_inst.io.load_imem  <>  io.LD
    if(cfg.USE_PWM0)  AppleSoC_de2_inst.io.pwm0cmpgpio <> io.LEDG

    // GPIO connection
    for (i <- 0 until 4) {
      if(cfg.USE_GPIO0) AppleSoC_de2_inst.io.gpio0(i).read := io.KEY(i)
    }
    val SW_base = 4
    for (i <- 0 until 16) {
      if(cfg.USE_GPIO0) AppleSoC_de2_inst.io.gpio0(i+SW_base).read := io.SW(i)
    }
    val LEDR_base = 20
    for (i <- 0 until 12) {
      if(cfg.USE_GPIO0) io.LEDR(i) := AppleSoC_de2_inst.io.gpio0(i+LEDR_base).write
      if(cfg.USE_GPIO0) AppleSoC_de2_inst.io.gpio0(i+LEDR_base).read := False
    }
  }
}

object DE2_AppleRISCVSoCMain{
  def main(args: Array[String]) {
    // CPU Configuration
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = false
    CsrCfg.USE_MHPMC3         = true
    CsrCfg.USE_MHPMC4         = true
    SpinalVerilog(InOutWrapper(DE2_top()))
  }
}