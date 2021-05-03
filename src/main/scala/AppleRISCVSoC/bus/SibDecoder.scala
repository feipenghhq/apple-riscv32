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

package AppleRISCVSoC.bus

import spinal.core._
import spinal.lib._

/**
 * SIB 1 to n decoder
 */
case class Sib_decoder(mainSibCfg: SibConfig, targetSibCfg: Array[SibConfig], pipeline: Boolean = false) extends Component {

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

  val hostSib_1 = Sib(mainSibCfg)

  if (pipeline) {
    val hostSib_s1 = Reg(hostSib)
    hostSib_s1.sel   init False
    hostSib_s1.ready init False
    hostSib_s1.resp  init False
    hostSib_s1.sel    := hostSib.sel
    hostSib_s1.enable := hostSib.enable
    hostSib_s1.write  := hostSib.write
    hostSib_s1.addr   := hostSib.addr
    hostSib_s1.wdata  := hostSib.wdata
    hostSib_s1.mask   := hostSib.mask

    hostSib_s1.ready := hostSib.sel
    hostSib_s1.resp  := hostSib.sel

    hostSib_1.sel    := hostSib_s1.sel
    hostSib_1.enable := hostSib_s1.enable
    hostSib_1.write  := hostSib_s1.write
    hostSib_1.addr   := hostSib_s1.addr
    hostSib_1.wdata  := hostSib_s1.wdata
    hostSib_1.mask   := hostSib_s1.mask

    hostSib.rdata := hostSib_1.rdata
    hostSib.resp  := hostSib_1.resp  & hostSib_s1.resp
    hostSib.ready := hostSib_1.ready & hostSib_s1.ready
  } else {
    hostSib_1 <> hostSib
  }

  for (i <- 0 until num) {
    dec_sel(i) := (hostSib_1.addr >= clientSib(i).config.addr_lo) & (hostSib_1.addr <= clientSib(i).config.addr_hi)
    clientSib(i).write  := hostSib_1.write
    clientSib(i).addr   := hostSib_1.addr(clientSib(i).config.addressWidth - 1 downto 0)
    clientSib(i).wdata  := hostSib_1.wdata
    clientSib(i).enable := hostSib_1.enable
    clientSib(i).mask   := hostSib_1.mask
    clientSib(i).sel    := hostSib_1.sel & dec_sel(i)
  }

  // need to delay the sel for one cycle as data come at the next cycle
  hostSib_1.rdata := MuxOH(dec_sel_ff, clientSib.map(_.rdata))
  hostSib_1.resp  := MuxOH(dec_sel, clientSib.map(_.resp)) & dec_good
  hostSib_1.ready := MuxOH(dec_sel, clientSib.map(_.ready))
}

// Timing Diagram
//                           ┌────┐
//                           │    │
//   hostSib In         ─────┘    └─────────────────
//                                ┌────┐
//                                │    │
//   hostSib S1         ──────────┘    └────────────
//                      ───────────────┬─────┬──────
//   hostSib S1 RDATA                  │     │
//                      ───────────────┴─────┴───────
//                                ┌────┐
//   hostSib Ready                │    │
//                      ──────────┘    └─────────────