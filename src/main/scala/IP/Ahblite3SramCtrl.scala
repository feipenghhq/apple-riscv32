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

case class SibSramCtrl(ahblite3Cfg: AhbLite3Config) extends Component {

  val io = new Bundle{
    val ahblite3 = slave(AhbLite3(ahblite3Cfg))
    val sram     = master(Sram())
  }
  noIoPrefix()

  // Register Input
  val ce_ff   = RegNext(io.ahblite3.HTRANS(1)) init False
  val write_ff = RegNextWhen(io.ahblite3.HWRITE,      io.ahblite3.HTRANS(1))
  val mask_ff  = RegNextWhen(io.ahblite3.writeMask(), io.ahblite3.HTRANS(1))
  val wdata_ff = RegNextWhen(io.ahblite3.HWDATA,      io.ahblite3.HTRANS(1))
  val addr_ff  = RegNextWhen(io.ahblite3.HADDR,       io.ahblite3.HTRANS(1))

  val wen = write_ff & (mask_ff.orR)

  // Sram I/O
  io.sram.ce_n := ~ce_ff
  io.sram.lb_n := ~mask_ff(0)
  io.sram.ub_n := ~mask_ff(1)
  io.sram.we_n := ~wen
  io.sram.oe_n := ~ce_ff
  // The Ahblite3 address is byte address, but the sram address is half-word address.
  io.sram.addr := addr_ff(SramCfg.AW downto 1)
  io.sram.data.write := wdata_ff
  io.sram.data.writeEnable.setAllTo(wen)

  io.ahblite3.HREADYOUT := True
  io.ahblite3.HRESP  := addr_ff(0)  // the LSb should be zero. 0 means OK.
  io.ahblite3.HRDATA := io.sram.data.read
}
