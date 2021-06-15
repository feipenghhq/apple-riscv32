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
// Revision 1: 05/19/2021
// Revision 2: 05/23/2021
//
// ================== Description ==================
//
// SRAM Controller for IS61LV25616 SRAM Chip
//
// Revision 1:
//  - Use 16 bit data width instead of 32 bits.
//  - A Bridge is required to make a 32 bits data access.
//
// Revision 2:
//  - Use Ahblite3 as bus interface
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite._
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

case class Ahblite3SramCtrl(ahblite3Cfg: AhbLite3Config) extends Component {
  require(ahblite3Cfg.dataWidth == 16)
  val io = new Bundle {
    val ahblite3 = slave(AhbLite3(ahblite3Cfg))
    val sram     = master(Sram())
  }
  noIoPrefix()

  // Register Input
  val enable = io.ahblite3.HTRANS(1) & io.ahblite3.HSEL
  val pending_rw = RegNext(enable) init False
  val pending_write = RegNext(enable & io.ahblite3.HWRITE) init False
  val pending_mask = RegNextWhen(io.ahblite3.writeMask(), enable)
  val pending_addr = RegNextWhen(io.ahblite3.HADDR,       enable)

  val wen = pending_write & (pending_mask.orR)

  // Sram I/O
  io.sram.ce_n := ~pending_rw
  io.sram.lb_n := ~pending_mask(0)
  io.sram.ub_n := ~pending_mask(1)
  io.sram.we_n := ~wen
  io.sram.oe_n := ~pending_rw
  // The Ahblite3 address is byte address, but the sram address is half-word address.
  io.sram.addr := pending_addr(SramCfg.AW downto 1)
  io.sram.data.write := io.ahblite3.HWDATA  // write data come 1 cycle after the address phase
  io.sram.data.writeEnable.setAllTo(wen)

  // avoid read after write same address hazard
  io.ahblite3.HREADYOUT := !(io.ahblite3.HSEL && io.ahblite3.HTRANS(1) && !io.ahblite3.HWRITE && pending_write && io.ahblite3.HADDR === pending_addr)
  io.ahblite3.HRESP := pending_addr(0)  // the LSb should be zero and 0 means OK.
  io.ahblite3.HRDATA := io.sram.data.read
}
