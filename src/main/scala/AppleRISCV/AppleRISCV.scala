///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: AppleRISCV
//
// Author: Heqing Huang
// Date Created: 05/01/2021
//
// ================== Description ==================
//
// AppleRISCV top level
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import AppleRISCVSoC.Bus._
import spinal.core._
import spinal.lib._

case class AppleRISCV() extends Component {

  val io = new Bundle {
    val imemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
    val dmemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
  }

  // ====================================
  // Instantiate all the modules
  // ====================================

  val ifStage = IF()
  val idStage = ID()
  val exStage = EX()
  val memWbStage = MEMWB()

  val hdu = HDU()
  val trapCtrl = TrapCtrl()

  // To Top Level IO
  ifStage.io.imemSib <> io.imemSib
  memWbStage.io.dmemSib <> io.dmemSib

  // IF
  ifStage.io.if2id <> idStage.io.if2id
  ifStage.io.bu2pc <> exStage.io.bu2pc
  ifStage.io.trapCtrl2pc <> trapCtrl.io.trapCtrl2pc
  ifStage.io.ifStageCtrl <> hdu.io.ifStageCtrl


  // ID
  idStage.io.id2ex <> exStage.io.id2ex
  idStage.io.exRdWrCtrl := exStage.io.id2ex.rdWrCtrl
  idStage.io.memRdWrCtrl := memWbStage.io.ex2mem.rdWrCtrl
  idStage.io.rdWdata  <> memWbStage.io.rdWdata
  idStage.io.rdWrCtrl <> memWbStage.io.wb_rdWrCtrl
  idStage.io.idStageCtrl <> hdu.io.idStageCtrl

  // EX
  exStage.io.ex2mem <> memWbStage.io.ex2mem
  exStage.io.rs1DataMem := memWbStage.io.ex2mem.aluOut
  exStage.io.rs2DataMem := memWbStage.io.ex2mem.aluOut
  exStage.io.rs1DataWb := memWbStage.io.rdWdata
  exStage.io.rs2DataWb := memWbStage.io.rdWdata
  exStage.io.exStageCtrl <> hdu.io.exStageCtrl

  // MEM
  memWbStage.io.memStageCtrl <> hdu.io.memStageCtrl


}


object AppleRISCVMain {
  def main(args: Array[String]) {
    SpinalVerilog(new AppleRISCV).printPruned()
  }
}