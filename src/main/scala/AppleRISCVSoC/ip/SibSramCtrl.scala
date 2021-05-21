///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: SibSramCtrl
//
// Author: Heqing Huang
// Date Created: 05/14/2021
// Revision 1.0: 05/19/2021
//
// ================== Description ==================
//
// SRAM Controller for IS61LV25616 SRAM Chip
//
// Revision 1.0:
//  - Use 16 bit data width instead of 32 bits.
//  - A Bridge is required to make a 32 bits data access.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._
import spinal.lib.fsm._
import spinal.lib.io._

object SramCfg {
  val AW = 18
  val DW = 16
}

case class Sram() extends Bundle with IMasterSlave {
  val addr = UInt(SramCfg.AW bits)
  val data = TriStateArray(SramCfg.DW bits)
  val we_n = Bool
  val oe_n = Bool
  val ub_n = Bool
  val lb_n = Bool
  val ce_n = Bool

  override def asMaster(): Unit = {
    out(addr, we_n, oe_n, ub_n, lb_n, ce_n)
    master(data)
  }
}

case class SibSramCtrl(sibCfg: SibConfig) extends Component {

  val io = new Bundle{
    val sram_sib = slave(Sib(sibCfg))
    val sram     = master(Sram())
  }
  noIoPrefix()

  // Register Input
  val sel_ff   = RegNext(io.sram_sib.sel) init False
  val write_ff = RegNextWhen(io.sram_sib.write, io.sram_sib.enable & io.sram_sib.sel)
  val mask_ff  = RegNextWhen(io.sram_sib.mask,  io.sram_sib.enable & io.sram_sib.sel)
  // The sib address is the byte address, but the sram address is half-word address.
  val addr_ff  = RegNextWhen(io.sram_sib.addr(SramCfg.AW downto 1), io.sram_sib.enable & io.sram_sib.sel)
  val wdata_ff = RegNextWhen(io.sram_sib.wdata, io.sram_sib.enable & io.sram_sib.sel)

  val wen = write_ff & (mask_ff.orR)

  // Sram I/O
  io.sram.ce_n := ~sel_ff
  io.sram.lb_n := ~mask_ff(0)
  io.sram.ub_n := ~mask_ff(1)
  io.sram.we_n := ~wen
  io.sram.oe_n := ~sel_ff
  io.sram.addr := addr_ff
  io.sram.data.write := wdata_ff
  io.sram.data.writeEnable.setAllTo(wen)

  // Sib output and response
  io.sram_sib.ready := True
  io.sram_sib.resp  := True
  io.sram_sib.rdata := io.sram.data.read
}
