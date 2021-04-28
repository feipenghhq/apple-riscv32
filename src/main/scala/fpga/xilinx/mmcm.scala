///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: PLL
//
// Author: Heqing Huang
// Date Created: 04/26/2021
//
// ================== Description ==================
//
// The PLL black box for FPGA
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package fpga.xilinx

import spinal.core._

class mmcm extends BlackBox{
  val io = new Bundle{
    val clk_in1    = in Bool
    val clk_out1   = out Bool
  }

  noIoPrefix()
}