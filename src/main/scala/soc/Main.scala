///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Main
//
// Author: Heqing Huang
// Date Created: 03/27/2021
//
// ================== Description ==================
//
// Main function for the CPU soc
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package soc

import spinal.core._
import core._
import ip._
import spinal.lib.io.InOutWrapper
import spinal.lib.com.uart.UartCtrlGenerics


// Run this main to generate the RTL

object Main{
  def main(args: Array[String]) {
    val cfg = soc_cfg (
      cpu_param = new CPU_PARAM(),
      soc_param = new SOC_PARAM(),
      gpio0Cfg   = new GpioCfg(false, false, false, false),
      gpio1Cfg   = new GpioCfg(false, false, false, false),
      uartCfg   = new UartCfg(UartCtrlGenerics(), 8, 8)
    )
    val spinalCfg = SpinalConfig(
      targetDirectory = "src/rtl/gen/generic"
    ).generateVerilog(InOutWrapper(apple_riscv_soc(cfg)))
  }
}