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
    val gpio0 = master(TriStateArray(12 bits))
    val uart0 = master(Uart())  // this is needed for debug
    val pwm0cmpgpio = out Bits(4 bits)
  }
  noIoPrefix()

  // Clock domain and PLL
  val clkCtrl = new Area {
    //Create a new clock domain named 'core'
    val coreClockDomain = ClockDomain.internal(
      name = "soc",
      frequency = FixedFrequency(100 MHz),
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
    val AooleSoC_de2_inst = AppleSoC_de2()
    AooleSoC_de2_inst.io.clk    := clkCtrl.coreClockDomain.clock
    AooleSoC_de2_inst.io.reset  := clkCtrl.coreClockDomain.reset
    AooleSoC_de2_inst.io.sram   <> io.sram
    AooleSoC_de2_inst.io.uart0  <> io.uart0
    if(cfg.USE_GPIO0) AooleSoC_de2_inst.io.gpio0  <> io.gpio0
    if(cfg.USE_PWM0) AooleSoC_de2_inst.io.pwm0cmpgpio <> io.pwm0cmpgpio
    AooleSoC_de2_inst.io.load_imem  :=  io.gpio0(8).read
  }
}

object DE2_AppleRISCVSoCMain{
  def main(args: Array[String]) {
    // CPU Configuration
    AppleRISCVCfg.USE_RV32M   = true
    AppleRISCVCfg.USE_BPU     = true
    CsrCfg.USE_MHPMC3         = true
    CsrCfg.USE_MHPMC4         = true
    SpinalVerilog(InOutWrapper(DE2_top())).printPruned()
  }
}