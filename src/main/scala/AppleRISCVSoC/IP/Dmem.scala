///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: apple_riscv_soc
//
// Author: Heqing Huang
// Date Created: 05/01/2021
//
// ================== Description ==================
//
// The Data memory
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.IP

import AppleRISCVSoC._
import spinal.core._
import spinal.lib._
import _root_.AppleRISCVSoC.Bus._

case class Dmem() extends Component {

  val dmemSib = slave(Sib(AppleRISCVSoCCfg.dmemSibCfg))

  // We fetch  a word (4 bytes) at a time so reduce the size by 4
  val SIZE = 1 << (AppleRISCVSoCCfg.DATA_RAM_ADDR_WIDTH - 2)
  val ram = new Mem(Bits(AppleRISCVSoCCfg.INSTR_RAM_DATA_WIDTH bits), SIZE)

  // == CPU side == //
  val word_addr = dmemSib.addr(AppleRISCVSoCCfg.DATA_RAM_ADDR_WIDTH - 1 downto 2)
  // Here we use HSEL to indicate we want to stall the data
  val read_en = dmemSib.sel

  // Decode logic for write byte enable
  val byte_sel = dmemSib.addr(1 downto 0)
  val word_sel = dmemSib.addr(1)

  dmemSib.ready := True
  dmemSib.resp := True

  ram.write(
    address = word_addr,
    data = dmemSib.wdata,
    enable = dmemSib.write,
    mask = dmemSib.mask
  )

  dmemSib.rdata := ram.readSync(
    address = word_addr,
    enable = ~dmemSib.write & read_en
  )
}
