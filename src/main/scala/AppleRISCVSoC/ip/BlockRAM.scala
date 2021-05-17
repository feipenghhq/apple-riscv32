///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: BlockRAM
//
// Author: Heqing Huang
// Date Created: 03/30/2021
// Revision V2: 05/10/2021
//
// ================== Description ==================
//
// FPGA On-chip Ram Model
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._

/** Generic Block Ram */
case class BlockRAM(usePort2: Boolean = false, p1Cfg: SibConfig, p2Cfg: SibConfig = null) extends Component {

  val io = new Bundle{
    val port1 = slave(Sib(p1Cfg))
    val port2 = if (usePort2) slave(Sib(p2Cfg)) else null
  }
  noIoPrefix()

  // We fetch  a word (4 bytes) at a time so reduce the size by 2
  val SIZE = 1 << (p1Cfg.addressWidth - 2)
  val ram = new Mem(Bits(p1Cfg.dataWidth bits), SIZE)

  def port(p: Sib) = new Area {
    val word_addr = p.addr(p1Cfg.addressWidth-1 downto 2)
    val read_en   = p.enable & p.sel & ~p.write
    val write_en  = p.enable & p.sel & p.write
    p.ready := True
    p.resp := True
    ram.write(
      address = word_addr,
      data = p.wdata,
      enable = write_en,
      mask = p.mask

    )
    p.rdata := ram.readSync(
      address = word_addr,
      enable = read_en
    )
  }

  val port1 = port(io.port1)
  val port2 = if (usePort2) port(io.port2) else null
}


/** Block Ram Wrapper with Inter RAM BB */
case class IntelRam_2rw_32kb(p1Cfg: SibConfig, p2Cfg: SibConfig) extends Component {
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

/** Inter RAM BB - 2 RW port with byte enable */
case class IntelRam_2rw_32kb_bb() extends BlackBox {

  val io = new Bundle {
    val clock = in Bool
    val data_a = in Bits(32 bits)
    val data_b = in Bits(32 bits)
    val address_a = in UInt(13 bits)
    val address_b = in UInt(13 bits)
    val wren_a    = in Bool
    val wren_b    = in Bool
    val addressstall_a = in Bool
    val addressstall_b = in Bool
    val byteena_a = in Bits(4 bits)
    val byteena_b = in Bits(4 bits)
    val q_a    = out Bits(32 bits)
    val q_b    = out Bits(32 bits)
  }
  noIoPrefix()

  mapClockDomain(clock=io.clock)
}