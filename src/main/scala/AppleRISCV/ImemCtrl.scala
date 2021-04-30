package AppleRISCV

import AppleRISCVSoC.Bus._

import spinal.core._
import spinal.lib._


case class ImemInstrStage() extends Bundle with IMasterSlave {
  val va = out Bits(AppleRISCVCfg.xlen bits)
  override def asMaster(): Unit = {out(va)}
}

case class ImemCtrl() extends Component {

  val io = new Bundle {
    // input interface
    val pc2imemCtrl = slave(Pc2imemCtrlBD())
    val ifStageCtrl = slave(StageCtrlBD())
    // output interface
    val imemInstr = master(ImemInstrStage())
    // bus interface
    val imemSib = master(Sib(AppleRISCVCfg.ImemSibCfg))
  }
  noIoPrefix()

  // Master signals
  io.imemSib.sel       := True           // We always want to read instruction memory
  io.imemSib.enable    := io.ifStageCtrl.status === StageCtrlEnum.ENABLE
  io.imemSib.addr      := io.pc2imemCtrl.addr
  io.imemSib.wdata     := 0              // Fixed to zero, We are not writing to I-mem through this port
  io.imemSib.write     := False          // Fixed to zero, We are not writing to I-mem through this port
  io.imemSib.mask      := 0

  // Slave signals
  io.imemInstr.va      := io.imemSib.rdata

  // FIXME: Added exception for those situation
  //val imem_ready      = imemSib.ready    // This should always 1
  //val imem_resp       = imemSib.resp     // This should always 1
  //val imem_data_vld   = imem_ready & imem_resp
}
