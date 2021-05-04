///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/03/2021
//
// ================== Description ==================
//
// Sib Mux. The user should make sure that only one input is used at a given time. Otherwise an
// error will be generated. (resp = 0)
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.bus

import spinal.core._
import spinal.lib._

/**
 * SIB n to 1 mux
 */
case class SibMux(inSibCfg: Array[SibConfig], outSibCfg: SibConfig) extends Component {

  // defining the interface
  val inputSib = inSibCfg map (x => slave(Sib(x)))
  val outputSib = master(Sib(outSibCfg))

  outputSib.sel    := inputSib.map(_.sel).reduce(_ | _)
  outputSib.write  := MuxOH(inputSib.map(_.sel), inputSib.map(_.write))
  outputSib.enable := MuxOH(inputSib.map(_.sel), inputSib.map(_.enable))
  outputSib.addr   := MuxOH(inputSib.map(_.sel), inputSib.map(_.addr))
  outputSib.mask   := MuxOH(inputSib.map(_.sel), inputSib.map(_.mask))
  outputSib.wdata  := MuxOH(inputSib.map(_.sel), inputSib.map(_.wdata))
  
  for (sib <- inputSib) {
    sib.rdata := outputSib.rdata
    sib.resp  := outputSib.resp
    sib.ready := outputSib.ready
  }
}