///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: TrapCtrl
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Trap Controller
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._

//=============================
// Port Declaration
//=============================


/**
 * trap unit to PC
 */
case class TrapCtrl2pcBD() extends Bundle with IMasterSlave {
  val trap = in Bool
  val pc   = in UInt(AppleRISCVCfg.xlen bits)
  override def asMaster(): Unit = {
    out(trap, pc)
  }
}

//=============================
// Module
//=============================

case class TrapCtrl() extends Component {

}
