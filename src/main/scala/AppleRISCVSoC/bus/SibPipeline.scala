///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/05/2021
//
// ================== Description ==================
//
// Sib Pipeline. Pipeline stage for the Sib Decoder.
// For read request, it can only take 1 read request at a given transaction and it will blocks
// all the upcoming read request until this one is completed.
//
// For write request, since it is posted, it can still be a pipelined read request.
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.bus

import spinal.core._
import spinal.lib._

// Potential issue on the enable signal

/**
 * SIB Pipeline stage
 */
case class SibPipeline(sibCfg: SibConfig) extends Component {

  // defining the interface
  val inSib = slave(Sib(sibCfg))
  val outSib = master(Sib(sibCfg))

  // flop stages
  val busy      = Reg(Bool) init False
  val sel_s1    = RegNextWhen(inSib.sel,    ~busy) init False
  val enable_s1 = RegNextWhen(inSib.enable, ~busy) init False
  val addr_s1   = RegNextWhen(inSib.addr,   ~busy)
  val write_s1  = RegNextWhen(inSib.write,  ~busy)
  val mask_s1   = RegNextWhen(inSib.mask,   ~busy)
  val wdata_s1  = RegNextWhen(inSib.wdata,  ~busy) 

  outSib.sel    := sel_s1
  outSib.enable := enable_s1
  outSib.addr   := addr_s1
  outSib.write  := write_s1
  outSib.mask   := mask_s1
  outSib.wdata  := wdata_s1


  val is_read       = inSib.sel & ~inSib.write
  val new_read      =  is_read & !busy
  val read_complete = sel_s1 & outSib.ready

  inSib.rdata   := outSib.rdata
  inSib.resp    := outSib.resp
  inSib.ready   := !new_read & outSib.ready

  when(!busy & is_read) {
    busy := True
  }.elsewhen(read_complete) {
    busy := False
  }
}