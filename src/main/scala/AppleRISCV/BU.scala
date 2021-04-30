///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: BU
//
// Author: Heqing Huang
// Date Created: 04/30/2021
//
// ================== Description ==================
//
// Branch Unit
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._
import spinal.lib._

/**
 * branch unit to PC
 */
case class Bu2pcBD() extends Bundle with IMasterSlave {
  val branch = in Bool
  val pc     = in UInt(AppleRISCVCfg.xlen bits)
  override def asMaster(): Unit = {
    out(branch, pc)
  }
}

case class BU() extends Component {

}
