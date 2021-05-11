///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: program counter
//
// Author: Heqing Huang
// Date Created: 03/27/2021
//
// ================== Description ==================
//
// PC register
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._


case class PC() extends Component {
    
    val io  = new Bundle{
        // IO port
        val bpu_pred_take   = in Bool
        val bpu_pc_in       = in UInt(AppleRISCVCfg.XLEN bits)
        val branch_pc_in    = in UInt(AppleRISCVCfg.XLEN bits)
        val branch          = in Bool
        val trap            = in Bool
        val trap_pc_in      = in UInt(AppleRISCVCfg.XLEN bits)
        val stall           = in Bool
        val pc_out          = out UInt(AppleRISCVCfg.XLEN bits)
    }
    noIoPrefix()

    val pc_value = Reg(UInt(AppleRISCVCfg.XLEN bits)) init AppleRISCVCfg.PC_RESET_VAL
    when(!io.stall) {
        when( io.trap) {
            pc_value := io.trap_pc_in
        }.elsewhen(io.branch) {
            pc_value := io.branch_pc_in
        }.elsewhen(io.bpu_pred_take) {
            pc_value := io.bpu_pc_in
        }.otherwise {
            pc_value := pc_value + 4
        }
    }
    io.pc_out := pc_value
}
