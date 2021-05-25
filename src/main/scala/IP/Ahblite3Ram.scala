///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Ahblite3Ram
//
// Author: Heqing Huang
// Date Created: 03/30/2021
// Revision 1: 05/23/2021
//
// ================== Description ==================
//
// FPGA On-chip Ram Model
//
// Revision 1:
//  - Redesigned the ram model
//  - Use Ahblite3 as bus interface
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package IP

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite._

// ===========================================
// Generic AHB RAM
// ===========================================

/** Generic Memory Trait */
trait Ahblite3RamGeneric {

  // define the memory port using spinal mem
  def port_generic(ahblite3Cfg: AhbLite3Config, p: AhbLite3, ram: Mem[Bits]): Area = new Area {

    val wordRange = ahblite3Cfg.addressWidth-1 downto log2Up(ahblite3Cfg.bytePerWord)

    val word_addr = p.HADDR(wordRange)
    val pending_write = RegNext(p.HTRANS(1) & p.HWRITE & p.HSEL)
    val pending_addr = RegNext(word_addr)
    val pending_mask = RegNext(p.writeMask())

    ram.write(
      address = pending_addr,
      data    = p.HWDATA,
      enable  = pending_write,
      mask    = pending_mask
    )
    p.HRDATA := ram.readSync(
      address = word_addr
    )
    // avoid read after write same address hazard
    p.HREADYOUT := !(p.HSEL && p.HTRANS(1) && !p.HWRITE && pending_write && p.HADDR(wordRange) === pending_addr)
    p.HRESP     := False  // 0 means OK
  }
}

// =====================================
// RAM 1RW Port
// =====================================

/** Basic 1 rw port memory */
abstract class Ahblite3Ram_1rw(ahblite3Cfg: AhbLite3Config) extends Component with Ahblite3RamGeneric {
  val io = new Bundle {
    val port1 = slave(AhbLite3(ahblite3Cfg))
  }
  noIoPrefix()
}

/** Xilinx Block Ram model */
case class Ahblite3Bram_1rw(ahblite3Cfg: AhbLite3Config) extends Ahblite3Ram_1rw(ahblite3Cfg) {
  val SIZE = 1 << (ahblite3Cfg.addressWidth - 2) // We fetch a word (4 bytes) at a time
  val ram = new Mem(Bits(ahblite3Cfg.dataWidth bits), SIZE)
  port_generic(ahblite3Cfg, io.port1, ram)
}

// =====================================
// RAM 2RW Port
// =====================================

/** Basic 2 rw port memory */
abstract class Ahblite3Ram_2rw(ahblite3Cfg: AhbLite3Config) extends Component with Ahblite3RamGeneric {
  val io = new Bundle {
    val ports = Vec(slave(AhbLite3(ahblite3Cfg)), 2)
  }
  noIoPrefix()
}

/** Xilinx Block Ram model */
case class Ahblite3Bram_2rw(ahblite3Cfg: AhbLite3Config) extends Ahblite3Ram_2rw(ahblite3Cfg) {
  val SIZE = 1 << (ahblite3Cfg.addressWidth - 2) // We fetch a word (4 bytes) at a time
  val ram = new Mem(Bits(ahblite3Cfg.dataWidth bits), SIZE)
  port_generic(ahblite3Cfg, io.ports(0), ram)
  port_generic(ahblite3Cfg, io.ports(1), ram)
}

/** Block Ram Wrapper with Intel RAM BB */
/*
case class IntelRam_2rw_32kb(p1Cfg: Apb3Config, p2Cfg: Apb3Config) extends Component {
  require(p1Cfg.dataWidth == 32)
  require(p1Cfg.addressWidth == 15)
  val io = new Bundle{
    val port1 = slave(Sib(p1Cfg))
    val port2 = slave(Sib(p2Cfg))
  }
  noIoPrefix()
    // We fetch  a word (4 bytes) at a time so reduce the address size by 2
  val ram = IntelRam_2rw_32kb_bb()
  ram.setDefinitionName("intelram_2rw_32kb")

  ram.io.data_a    := io.port1.wdata
  ram.io.data_b    := io.port2.wdata
  ram.io.address_a := io.port1.addr(p1Cfg.addressWidth-1 downto 2)
  ram.io.address_b := io.port2.addr(p1Cfg.addressWidth-1 downto 2)
  ram.io.wren_a    := io.port1.enable & io.port1.sel & io.port1.write
  ram.io.wren_b    := io.port2.enable & io.port2.sel & io.port2.write
  ram.io.addressstall_a := ~io.port1.enable & io.port1.sel
  ram.io.addressstall_b := ~io.port2.enable & io.port2.sel
  ram.io.byteena_a := io.port1.mask
  ram.io.byteena_b := io.port2.mask
  io.port1.rdata   := ram.io.q_a
  io.port2.rdata   := ram.io.q_b

  io.port1.ready   := True
  io.port2.ready   := True
  io.port1.resp    := True
  io.port2.resp    := True
}
*/

// =====================================
// Intel RAM BlackBox
// =====================================

/** Inter RAM BB - 2 RW port with byte enable */
case class IntelRam_2rw_32kb_bb() extends BlackBox {
  val io = new Bundle {
    val clock           = in Bool
    val data_a          = in Bits(32 bits)
    val data_b          = in Bits(32 bits)
    val address_a       = in UInt(13 bits)
    val address_b       = in UInt(13 bits)
    val wren_a          = in Bool
    val wren_b          = in Bool
    val addressstall_a  = in Bool
    val addressstall_b  = in Bool
    val byteena_a       = in Bits(4 bits)
    val byteena_b       = in Bits(4 bits)
    val q_a             = out Bits(32 bits)
    val q_b             = out Bits(32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock=io.clock)
}