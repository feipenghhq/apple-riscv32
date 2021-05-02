package AppleRISCVSoC.ip

import spinal.core._

case class RstCtrl() extends Component {

  noIoPrefix()

  val io = new Bundle {
    val uart2imem_downloading = in Bool
    val cpu_reset_req = out Bool
  }

  val cpu_reset = Reg(Bool) init True

  when(io.uart2imem_downloading) {
    cpu_reset := True
  }.otherwise {
    cpu_reset := False
  }
  io.cpu_reset_req := cpu_reset
}
