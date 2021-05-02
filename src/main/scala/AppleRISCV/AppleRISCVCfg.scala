package AppleRISCV

import AppleRISCVSoC.Bus._

object AppleRISCVCfg {

  val xlen = 32

  val ImemSibCfg = SibConfig(
    addressWidth = xlen,
    dataWidth    = xlen
  )

  val DmemSibCfg = SibConfig(
    addressWidth = xlen,
    dataWidth    = xlen
  )

}
