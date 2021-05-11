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

case class BlockRAM(usePort2: Boolean = false, p1Cfg: SibConfig, p2Cfg: SibConfig = null) extends Component {

  val io = new Bundle{
    val port1 = slave(Sib(p1Cfg))
    val port2 = if (usePort2) slave(Sib(p2Cfg)) else null
  }

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
