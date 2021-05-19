///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: NA
//
// Author: Heqing Huang
// Date Created: 03/27/2021
//
// ================== Description ==================
//
// Define basic parameter for the CPU
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import AppleRISCVSoC.bus._
import spinal.core._

object AppleRISCVCfg {

    val NAME            = "AppleRISCV"

    val XLEN            = 32
    val RF_SIZE         = 32
    val RF_ADDR_WDITH   = log2Up(RF_SIZE)
    val MXLEN           = XLEN
    val CSR_ADDR_WIDTH  = 12

    val PC_RESET_VAL    = 0x20000000L

    val sibCfg = SibConfig(
        addressWidth = XLEN,
        dataWidth = XLEN
    )

    // RV32M Extension Configuration
    var USE_RV32M       = true
    var MUL_TYPE        = "DSP"
    var MUL_STAGE       = 3

    // Branch Prediction
    var USE_BPU         = true
    var BPU_DEPTH       = 32    // need to be power of 2
}

object CsrCfg {
    // Machine Counter Setup
    val USE_MCOUNTINHIBIT = true

    // Machine Counter/Timers
    val USE_MCYCLE      = true
    val USE_MINSTRET    = true
    var USE_MHPMC3      = true
    var USE_MHPMC4      = true
}


