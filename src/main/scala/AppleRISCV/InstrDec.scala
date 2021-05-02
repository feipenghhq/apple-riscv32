///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: InstrDec
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Instruction decoder module
//
///////////////////////////////////////////////////////////////////////////////////////////////////


package AppleRISCV

import spinal.core._
import spinal.lib._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** ALU IO bundle */
case class AluCtrlStage() extends Bundle with IMasterSlave {
  val aluOp = out (ALUCtrlEnum)
  override def asMaster(): Unit = {
    out(aluOp)
  }
}

/** Branch Unit IO bundle */
case class BuCtrlStage() extends Bundle with IMasterSlave {
  val branch   = out Bool
  val jal      = out Bool
  val jalr     = out Bool
  val branchOp = out (BranchCtrlEnum)
  override def asMaster(): Unit = {
    out(branch, jal, jalr, branchOp)
  }
}

/** Memory Control IO bundle */
case class DmemCtrlStage() extends Bundle with IMasterSlave {
  val read  = out Bool
  val write = out Bool
  val unsigned = out Bool
  val types = out (DmemTypeEnum)
  override def asMaster(): Unit = {
    out(read, write, unsigned, types)
  }
}


/**
 * Rd write Stage
 */
case class RdWrStage() extends Bundle with IMasterSlave{
  val wr    = Bool
  val addr  = UInt(5 bits)
  override def asMaster(): Unit = {
    out(wr, addr)
  }
}


case class InstrDec() extends Component {

  val io = new Bundle {
    // input
    val instr = in Bits(AppleRISCVCfg.xlen bits)

    // output
    val rs1RdCtrl = master(RsCtrlStage())
    val rs2RdCtrl = master(RsCtrlStage())
    val rdWrCtrl  = master(RdWrStage())
    val rdSelCtrl = out (RdSelEnum())
    val aluCtrl   = master(AluCtrlStage())
    val buCtrl    = master(BuCtrlStage())
    val dmemCtrl  = master(DmemCtrlStage())
    val op1Ctrl   = out (Op1CtrlEnum)
    val immValue  = out SInt(AppleRISCVCfg.xlen bits)
    val immSel    = out Bool
    val excIllegalInstr = out Bool
  }
  noIoPrefix()

  // ============================================
  // Extract each field from the instruction
  // ============================================

  io.rs1RdCtrl.addr  := io.instr(19 downto 15).asUInt
  io.rs2RdCtrl.addr  := io.instr(24 downto 20).asUInt
  io.rdWrCtrl.addr   := io.instr(11 downto 7).asUInt

  val opcode  = io.instr(6 downto 0).asBits
  val func3   = io.instr(14 downto 12)
  val func7   = io.instr(31 downto 25)
  //val func12  = io.instr(31 downto 20)

  io.rdWrCtrl.wr   := False
  io.rdSelCtrl     := RdSelEnum.ALU
  io.rs1RdCtrl.rd  := False
  io.rs2RdCtrl.rd  := False

  io.buCtrl.branchOp := BranchCtrlEnum.NONE
  io.buCtrl.branch   := False
  io.buCtrl.jal      := False
  io.buCtrl.jalr     := False
  io.aluCtrl.aluOp   := ALUCtrlEnum.NOP
  io.dmemCtrl.write  := False
  io.dmemCtrl.read   := False
  io.dmemCtrl.types  := DmemTypeEnum.WD
  io.dmemCtrl.unsigned  := False
  io.op1Ctrl         := Op1CtrlEnum.RS1
  io.immSel          := False
  io.excIllegalInstr := False

  val immType = ImmCtrlEnum()
  immType := ImmCtrlEnum.I

  val rdIsNotZero = io.rdWrCtrl.addr =/= 0

  def doGenericAction(act: ActionMap): Unit = {
    for((action, execute) <- act.act) {
      if (execute) {
        action match {
          case READ_RS1   => io.rs1RdCtrl.rd := True
          case READ_RS2   => io.rs2RdCtrl.rd := True
          case WRITE_RD   => io.rdWrCtrl.wr  := rdIsNotZero // Do not write to RD if RD is zero
          case OP2_IMM    => io.immSel       := True
          case WRITE_DMEM => io.dmemCtrl.write := True
          case READ_DMEM  => io.dmemCtrl.read  := True
          case BRANCH     => io.buCtrl.branch  := True
          case AJAL       => io.buCtrl.jal     := True
          case AJALR      => io.buCtrl.jalr    := True
        }
      }
    }
  }

  def doDec(ty: Instruction, useF3: Boolean): Unit = {
    doGenericAction(ty.action)
    immType := ty.immSel
    if (useF3) {
      val rst = InstrCodeMapList().get(ty)
      val instrList = rst match {
        case Some(x) => x
        case None => null
      }
      // Instruction pre-check
      // 1. Check if we have the same func3 number (for example for SRA and SRL have same func3)
      //    Need a special logic to take care of this
      // 2. Check if the switch is a full case, if not, need to add default
      val repeatedFunc7 = mutable.Set[Int]()
      val opInstrMap = mutable.Map[Int, ArrayBuffer[Instruction]]()
      for (instr <- instrList) {
        val rst = opInstrMap.get(instr.f3)
        rst match {
          case Some(arr) => {
            repeatedFunc7 += instr.f3
            arr += instr
          }
          case None => opInstrMap += ((instr.f3, ArrayBuffer[Instruction](instr)))
        }
      }

      // Decode the func3
      switch(func3) {
        // process the instruction with unique func3 code
        for (instr <- instrList) {
          if (!repeatedFunc7.contains(instr.f3)) {
            is(instr.f3) {
              io.aluCtrl.aluOp := instr.aluOp
              io.buCtrl.branchOp := instr.branchOp
              instr match {
                case sd: SDInstr => io.dmemCtrl.types := sd.types
                case ld: LDInstr => {
                  io.dmemCtrl.types := ld.types
                  io.dmemCtrl.unsigned := Bool(ld.unsigned)
                }
                case _ =>
              }
            }
          }
        }
        // process the instruction with same func3 code
        // => Use func7 to distinguish
        for (f3 <- repeatedFunc7) {
          is(f3) {
            val rst = opInstrMap.get(f3)
            val instrList = rst match {
              case Some(x) => x
              case None => null
            }
            switch(func7) {
              for (instr <- instrList){
                is(instr.f7) {
                  io.aluCtrl.aluOp := instr.aluOp
                }
              }
              default {io.excIllegalInstr := True}
            }
          }
        }
        // func3 can contain 8 cases
        if (opInstrMap.size != 8) {
          default {io.excIllegalInstr := True}
        }
      }
    } else {
      io.aluCtrl.aluOp   := ty.aluOp
    }
  }

  switch(opcode) {
    for (instr <- InstrCodeMapList()) {
      is(instr._1.opcode) {
        instr._1 match {
          case lui:   LUIInstr   => {
            doDec(lui, useF3 = false)
            io.op1Ctrl := Op1CtrlEnum.ZERO
          }
          case auipc: AUIPCInstr => {
            doDec(auipc, useF3 = false)
            io.op1Ctrl := Op1CtrlEnum.PC
          }
          case jal:   JALInstr   => doDec(jal, useF3 = false)
          case jalr:  JALRInstr  => doDec(jalr, useF3 = true)
          case br:    BRInstr    => doDec(br, useF3 = true)
          case ld:    LDInstr    => {
            doDec(ld, useF3 = true)
            io.rdSelCtrl := RdSelEnum.MEM
          }
          case sd:    SDInstr    => {doDec(sd, useF3 = true)}
          case lai:   LAIInstr   => doDec(lai, useF3 = true)
          case lar:   LARInstr   => doDec(lar, useF3 = true)
          case _ => ???
        }
      }
    }
    default {io.excIllegalInstr := True}
  }

  // Immediate value decode
  val i_type_imm = io.instr(31 downto 20).asSInt.resize(AppleRISCVCfg.xlen)
  val s_type_imm = (io.instr(31 downto 25) ## io.instr(11 downto 7)).asSInt.resize(AppleRISCVCfg.xlen)
  val u_type_imm = (io.instr(31 downto 12) ## U"12'h0").asSInt
  val b_type_imm = (io.instr(31) ## io.instr(7) ## io.instr(30 downto 25) ## io.instr(11 downto 8) ## False).asSInt.resize(AppleRISCVCfg.xlen)
  val j_type_imm = (io.instr(31) ## io.instr(19 downto 12) ## io.instr(20) ## io.instr(30 downto 21) ## False).asSInt.resize(AppleRISCVCfg.xlen)

  switch(immType) {
    is(ImmCtrlEnum.I) {
      io.immValue := i_type_imm
    }
    is(ImmCtrlEnum.S) {
      io.immValue := s_type_imm
    }
    is(ImmCtrlEnum.U) {
      io.immValue := u_type_imm
    }
    is(ImmCtrlEnum.B) {
      io.immValue := b_type_imm
    }
    is(ImmCtrlEnum.J) {
      io.immValue := j_type_imm
    }
  }
}


object InstrDecMain {
  def main(args: Array[String]) {
    SpinalVerilog(new InstrDec).printPruned()
  }
}