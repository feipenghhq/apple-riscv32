///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 04/21/2021
//
// ================== Description ==================
//
// Sib Decoder. Decode the Sib source to different sink
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package bus.sib

import spinal.core._
import spinal.lib._

/**
 * SIB 1 to n decoder
 */
case class Sib_decoder(mainSibCfg: SibConfig, targetSibCfg: Array[SibConfig]) extends Component {

  // defining the interface
  val hostSib = slave(Sib(mainSibCfg))
  val clientSib = targetSibCfg map (x => master(Sib(x)))

  // check if the switch is valid
  assert(mainSibCfg.addressWidth >= targetSibCfg.map(_.addressWidth).max,
    "SIB: The main module address range is smaller then the target module")
  assert(mainSibCfg.dataWidth >= targetSibCfg.map(_.dataWidth).max,
    "SIB: The main module data range is smaller then the target module")

  val num         = targetSibCfg.length
  val dec_sel     = Bits(num bits)
  val dec_sel_ff  = RegNext(dec_sel)
  val dec_good    = dec_sel.orR

  for (i <- 0 until num) {
    dec_sel(i) := (hostSib.addr >= clientSib(i).config.addr_lo) & (hostSib.addr <= clientSib(i).config.addr_hi)
    clientSib(i).write  := hostSib.write
    clientSib(i).addr   := hostSib.addr(clientSib(i).config.addressWidth - 1 downto 0)
    clientSib(i).wdata  := hostSib.wdata
    clientSib(i).enable := hostSib.enable
    clientSib(i).mask   := hostSib.mask
    clientSib(i).sel    := hostSib.sel & dec_sel(i)
  }

  // need to delay the sel for one cycle as data come at the next cycle
  hostSib.rdata := MuxOH(dec_sel_ff, clientSib.map(_.rdata))
  hostSib.resp  := MuxOH(dec_sel, clientSib.map(_.resp)) & dec_good
  hostSib.ready := MuxOH(dec_sel, clientSib.map(_.ready))

}