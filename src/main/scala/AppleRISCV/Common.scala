///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: Common
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Common Stuff for the CPU
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._

//========================================================

/**
 * Pipeline stage control bundle
 */
case class StageCtrlBD() extends Bundle with IMasterSlave {
  val enable = Bool
  val flush = Bool
  override def asMaster(): Unit = {
    out(enable, flush)
  }
}

//========================================================

object ALUCtrlEnum extends SpinalEnum(){
  val NOP, ADD, SUB, AND, OR, XOR, SRA, SRL, SLL, SLT, SLTU = newElement()
}

//========================================================

object DmemTypeEnum extends SpinalEnum(){
  val BY, HW, WD = newElement() // byte, halfword, word
}

//========================================================

object BranchCtrlEnum extends SpinalEnum(){
  val NONE, BEQ, BNE, BLT, BGE, BLTU, BGEU = newElement()
}

//========================================================

object ImmCtrlEnum extends SpinalEnum(){
  val I, S, B, U, J = newElement()
}

//========================================================

object Op1CtrlEnum extends SpinalEnum(){
  val RS1, PC, ZERO = newElement()
}

//========================================================

object RdSelEnum extends SpinalEnum(){
  val ALU, MEM, CSR = newElement()
}

//========================================================

object BypassCtrlEnum extends SpinalEnum(){
  val NONE, WB, MEM = newElement()
}

//========================================================

/** Create and connect Pipeline Stage to output */
object ccPipeStage {

  def initOrClear(a: => Data, clear: Boolean = false) = a match {
    case a: Bool => if(clear) a.clear() else a.init(False)
    case a: Bits => if(clear) a.clearAll() else a.init(0)
    case a: UInt => if(clear) a.clearAll() else a.init(0)
    case a: SInt => if(clear) a.clearAll() else a.init(0)
    case a: SpinalEnumCraft[ALUCtrlEnum.type] => if(!clear) {a.init(ALUCtrlEnum.NOP)}
    case a: SpinalEnumCraft[BranchCtrlEnum.type] => if(!clear) {a.init(BranchCtrlEnum.NONE)}
  }

  def isCtrl(s: Data): Boolean = {
    s.instanceAttributes.map(_.getName == "CTRL").fold(false)(_ || _)
  }

  def apply(_in: Bundle, _out: => Bundle)(ctrl: StageCtrlBD): Unit = {
    for (i <- _in.elements.zipWithIndex;
         o <- _out.elements.zipWithIndex;
        if(i._2 == o._2)
         ) {
      val a = Reg(cloneOf(i._1._2))
      a.setName(o._1._2.getName() + "_s")
      initOrClear(a)
      when (ctrl.flush) {
        initOrClear(a, clear = true)
      }.elsewhen(ctrl.enable) {
        a := i._1._2
      }
      o._1._2 := a
    }
  }

  def apply[T<:BaseType](_in: T, _out: => T)(ctrl: StageCtrlBD): Unit = {
    val a = Reg(cloneOf(_in))
    a.setName(_out.getName() + "_s")
    initOrClear(a)
    a := _in
    _out := a
  }
}

//========================================================
