///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: MEMWB
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Memory and Write Back Stage
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import AppleRISCVSoC.Bus._
import spinal.core._
import spinal.lib._


case class MEMWB() extends Component {
  val io = new Bundle {
    val ex2mem = slave(Ex2MemBD())
    val memStageCtrl = slave(StageCtrlBD())
    val dmemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
    val wb_rdWrCtrl  = master(RdWrStage())
    val rdWdata = out Bits(AppleRISCVCfg.xlen bits)
  }

  //=====================================
  // Memory Stage
  //=====================================

  val dmemCtrl = DmemCtrl()
  dmemCtrl.io.dmemCtrl <> io.ex2mem.dmemCtrl
  dmemCtrl.io.cpu2mcAddr <> io.ex2mem.aluOut.asUInt
  dmemCtrl.io.cpu2mcData <> io.ex2mem.op2Data
  dmemCtrl.io.dmemSib <> io.dmemSib

  val mem2wb_rdWrCtrl  = RdWrStage()
  val mem2wb_rdSelCtrl  = RdSelEnum()
  val mem2wb_aluOut    = Bits(AppleRISCVCfg.xlen bits)
  val mem2wb_pc        = UInt(AppleRISCVCfg.xlen bits)

  ccPipeStage(io.ex2mem.rdWrCtrl, mem2wb_rdWrCtrl)(io.memStageCtrl)
  ccPipeStage(io.ex2mem.rdSelCtrl, mem2wb_rdSelCtrl)(io.memStageCtrl)
  ccPipeStage(io.ex2mem.aluOut, mem2wb_aluOut)(io.memStageCtrl)
  ccPipeStage(io.ex2mem.pc, mem2wb_pc)(io.memStageCtrl)

  //=====================================
  // WB Stage
  //=====================================

  val rdMux = new Area {
    val finalRdData = Bits(AppleRISCVCfg.xlen bits)
    when (mem2wb_rdSelCtrl === RdSelEnum.MEM) {
      finalRdData := mem2wb_aluOut
    }.otherwise{
      finalRdData <> dmemCtrl.io.mc2cpuData
    }
  }

  io.wb_rdWrCtrl.wr := mem2wb_rdWrCtrl.wr
  io.rdWdata := rdMux.finalRdData
  io.wb_rdWrCtrl.addr := mem2wb_rdWrCtrl.addr
}

object MEMWBMain {
  def main(args: Array[String]) {
    SpinalVerilog(new MEMWB).printPruned()
  }
}