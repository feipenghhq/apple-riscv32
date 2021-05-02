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
    // clock and reset
    val clk = in Bool
    val reset = in Bool
    // bus
    val imemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
    val dmemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
    // interrupt
    val external_interrupt = in Bool
    val timer_interrupt = in Bool
    val software_interrupt = in Bool
    val debug_interrupt = in Bool
  }

  // clock domain
  val coreClockDomain = ClockDomain.internal(
    name = "core",
    frequency = FixedFrequency(50 MHz),
    config = ClockDomainConfig(
      clockEdge = RISING,
      resetKind = SYNC,
      resetActiveLevel = HIGH
    )
  )

  coreClockDomain.clock := io.clk
  coreClockDomain.reset := io.reset

  val core = new ClockingArea(coreClockDomain) {




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
    idStage.io.rdWdata <> memWbStage.io.rdWdata
    idStage.io.rdWrCtrl <> memWbStage.io.wb_rdWrCtrl
    val idStageCtrl = StageCtrlBD()
    idStageCtrl.enable := hdu.io.idStageCtrl.enable
    idStageCtrl.flush := hdu.io.idStageCtrl.flush | ~ifStage.io.if2id.valid
    idStage.io.idStageCtrl <> idStageCtrl

    // EX
    exStage.io.ex2mem <> memWbStage.io.ex2mem
    exStage.io.rs1DataMem := memWbStage.io.ex2mem.aluOut
    exStage.io.rs2DataMem := memWbStage.io.ex2mem.aluOut
    exStage.io.rs1DataWb := memWbStage.io.rdWdata
    exStage.io.rs2DataWb := memWbStage.io.rdWdata
    val exStageCtrl = StageCtrlBD()
    exStageCtrl.enable := hdu.io.exStageCtrl.enable
    exStageCtrl.flush := hdu.io.exStageCtrl.flush | ~idStage.io.id2ex.valid
    exStage.io.exStageCtrl <> exStageCtrl

    // MEM
    val memStageCtrl = StageCtrlBD()
    memStageCtrl.enable := hdu.io.memStageCtrl.enable
    memStageCtrl.flush := hdu.io.memStageCtrl.flush | ~memWbStage.io.ex2mem.valid
    memWbStage.io.memStageCtrl <> memStageCtrl

    // HDU
    hdu.io.branch := exStage.io.bu2pc.branch
    hdu.io.loadDependence := idStage.io.rsDepEx & idStage.io.id2ex.dmemCtrl.read
  }
}

object AppleRISCVMain {
  def main(args: Array[String]) {
    SpinalVerilog(new AppleRISCV).printPruned()
  }
}