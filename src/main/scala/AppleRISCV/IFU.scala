///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: IFU
//
// Author: Heqing Huang
// Date Created: 04/07/2021
// Revision 1.0: 05/23/2021
//
// ================== Description ==================
//
// Instruction Fetch Unit
//
// Revision 1.0:
//  - Renamed to IFU and Changed to AHB bus
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite.AhbLite3._
import spinal.lib.bus.amba3.ahblite._

case class IFU() extends Component {

  val io = new Bundle {
    val stage_valid = in Bool
    val stage_enable = in Bool
    val pc = in UInt(AppleRISCVCfg.XLEN bits)
    val instruction = out Bits(AppleRISCVCfg.XLEN bits)
    val ibus_ahb = master(AhbLite3Master(AppleRISCVCfg.ibusAhbCfg))
    val ifu_stall_req = out Bool
    val exc_instr_acc_flt = out Bool
  }

  io.ibus_ahb.HADDR     := io.pc
  io.ibus_ahb.HBURST    := B"3'b000"    // Single burst
  io.ibus_ahb.HMASTLOCK := False        // Not locked
  io.ibus_ahb.HPROT(0)  := False        // Opcode fetch
  io.ibus_ahb.HPROT(1)  := True         // Privileged access (We only have machine mode right now)
  io.ibus_ahb.HPROT(2)  := True         // Buffer-able
  io.ibus_ahb.HPROT(3)  := True         // Cache-able
  io.ibus_ahb.HSIZE     := B"3'b010"    // Word Access
  io.ibus_ahb.HTRANS    := io.stage_valid ? NONSEQ | IDLE
  io.ibus_ahb.HWDATA    := 0
  io.ibus_ahb.HWRITE    := False


  val addr_phase = io.stage_valid
  val data_phase = RegNext(addr_phase)

  // a shadow copy of the instruction for the stall
  val shadow = RegNextWhen(io.ibus_ahb.HRDATA, io.stage_enable)
  val stage_enable_ff = RegNext(io.stage_enable) init False

  io.instruction := stage_enable_ff ? shadow | io.ibus_ahb.HRDATA
  io.ifu_stall_req := data_phase & ~io.ibus_ahb.HREADY
  io.exc_instr_acc_flt := data_phase & io.ibus_ahb.HREADY & io.ibus_ahb.HRESP
}
