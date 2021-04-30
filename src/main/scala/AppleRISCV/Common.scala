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

/**
 * Pipeline stage control enumeration
 */
object StageCtrlEnum extends SpinalEnum(binaryOneHot){
  val FLUSH,STALL,ENABLE = newElement()
}

/**
 * Pipeline stage control bundle
 */
case class StageCtrlBD() extends Bundle with IMasterSlave {
  val status = StageCtrlEnum();
  override def asMaster(): Unit = {
    out(status)
  }
}

/** Create and connect Pipeline Stage to output */
object ccPipeStage {
  def apply(_in: Bundle, _out: => Bundle): Unit = {
    for (i <- _in.elements;
         o <- _out.elements) {
      val a = Reg(cloneOf(i._2))
      a.setName(o._2.getName() + "_s")
      a := i._2
      o._2 := a
    }
  }
}