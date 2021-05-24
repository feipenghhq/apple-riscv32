///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: InstrDec
//
// Author: Heqing Huang
// Date Created: 03/27/2021
//
// ================== Description ==================
//
// Instruction decoder module
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCV

import spinal.core._

case class InstrDecIO() extends Bundle {
    // instruction input
    val instr     = in Bits(AppleRISCVCfg.XLEN bits)
    val instr_vld = in Bool

    // Instruction field output
    val rd_idx  = out UInt(5 bits)
    val rs1_idx = out UInt(5 bits)
    val rs2_idx = out UInt(5 bits)
    val imm_value = out SInt(AppleRISCVCfg.XLEN bits)

    // Register file control signal
    val rd_wr  = out Bool
    val rs1_rd = out Bool
    val rs2_rd = out Bool

    // Memory control signal
    val lsu_wr         = out Bool
    val lsu_rd         = out Bool
    val lsu_ld_byte    = out Bool         // load/store byte
    val lsu_ld_hword   = out Bool         // load/store half word
    val lsu_ld_unsign  = out Bool         // load unsigned

    // ALU control
    val alu_opcode = out(AluOpcodeEnum())

    // MUL/DIV module control
    val mul_op     = if (AppleRISCVCfg.USE_RV32M) out Bool else null
    val div_op     = if (AppleRISCVCfg.USE_RV32M) out Bool else null
    val mul_opcode = if (AppleRISCVCfg.USE_RV32M) out(MulOpcodeEnum()) else null
    val div_opcode = if (AppleRISCVCfg.USE_RV32M) out(DivOpcodeEnum()) else null

    // Branch Unit control
    val bu_opcode = out(BranchOpcodeEnum())
    val branch_op = out Bool
    val jal_op    = out Bool
    val jalr_op   = out Bool

    // CSR control
    val csr_rd = out Bool
    val csr_wr = out Bool
    val csr_sel_imm = out Bool
    val csr_idx = out Bits(AppleRISCVCfg.CSR_ADDR_WIDTH bits)

    // System Instruction
    val mret         = out Bool
    val ecall        = out Bool
    val ebreak       = out Bool

    // data selection for register write back and CSR
    val rd_sel = out(RdSelEnum())
    val csr_sel = out(CsrSelEnum())

    // Other control signal
    val op2_sel_imm  = out Bool
    val op1_sel_zero = out Bool
    val op1_sel_pc   = out Bool

    // Exception
    val exc_ill_instr  = out Bool
}

case class InstrDec() extends Component {

    val io = InstrDecIO()
    noIoPrefix()

    // ============================================
    // Extract fields from the instruction
    // ============================================
    val opcode  = io.instr(6 downto 0)
    val func3   = io.instr(14 downto 12)
    val func7   = io.instr(31 downto 25)
    val func12  = io.instr(31 downto 20)

    io.rd_idx   := io.instr(11 downto 7).asUInt
    io.rs1_idx  := io.instr(19 downto 15).asUInt
    io.rs2_idx  := io.instr(24 downto 20).asUInt
    io.csr_idx  := io.instr(31 downto 20)

    // ============================================
    // Main Decoder Logic
    // ============================================

    object AluImmTypeE extends SpinalEnum {
        val ITYPE, STYPE, UTYPE, JTYPE, BTYPE = newElement()
    }

    val alu_imm_type = AluImmTypeE()
    alu_imm_type := AluImmTypeE.ITYPE

    // intermediate logic
    val func7_0000000 = func7 === 0
    val func7_0000001 = func7 === 1
    val func7_0100000 = func7 === B"7'b0100000"
    val rd_isnot_x0   = io.rd_idx =/= 0
    val rs1_isnot_x0  = io.rs1_idx =/= 0
    val ill_instr     = False

    io.exc_ill_instr := ill_instr & io.instr_vld

    // default value
    io.rd_wr          := False
    io.rs1_rd         := False
    io.rs2_rd         := False
    io.lsu_wr        := False
    io.lsu_rd        := False
    io.lsu_ld_byte   := False
    io.lsu_ld_hword  := False
    io.lsu_ld_unsign := False
    io.alu_opcode     := AluOpcodeEnum.AND
    io.bu_opcode      := BranchOpcodeEnum.BEQ
    io.rd_sel         := RdSelEnum.ALU
    io.csr_sel        := CsrSelEnum.DATA
    io.op2_sel_imm    := False
    io.branch_op      := False
    io.jal_op         := False
    io.jalr_op        := False
    io.mret           := False
    io.ecall          := False
    io.ebreak         := False
    io.csr_rd         := False
    io.csr_wr         := False
    io.csr_sel_imm    := False
    io.op1_sel_zero   := False
    io.op1_sel_pc     := False

    if (AppleRISCVCfg.USE_RV32M) {
        io.mul_op         := False
        io.div_op         := False
        io.mul_opcode     := MulOpcodeEnum.MUL
        io.div_opcode     := DivOpcodeEnum.DIV            
    }

    // The big switch for the opcode decode
    switch(opcode) {
        is(InstrDefine.OP_LUI) {
            io.alu_opcode  := AluOpcodeEnum.ADD
            io.op2_sel_imm := True
            io.op1_sel_zero := True
            io.rd_wr       := rd_isnot_x0
            alu_imm_type   := AluImmTypeE.UTYPE
        }
        is(InstrDefine.OP_AUIPC) {
            io.alu_opcode  := AluOpcodeEnum.ADD
            io.op2_sel_imm := True
            io.op1_sel_pc  := True
            io.rd_wr       := rd_isnot_x0
            alu_imm_type   := AluImmTypeE.UTYPE
        }
        is(InstrDefine.OP_JAL) {
            io.alu_opcode  := AluOpcodeEnum.PC4
            io.op2_sel_imm := True
            io.rd_wr       := rd_isnot_x0
            io.jal_op      := True
            alu_imm_type   := AluImmTypeE.JTYPE
        }
        is(InstrDefine.OP_JALR) {
            io.jalr_op      := True
            io.alu_opcode   := AluOpcodeEnum.PC4
            io.op2_sel_imm  := True
            io.rd_wr        := rd_isnot_x0
            io.rs1_rd       := True
            alu_imm_type    := AluImmTypeE.ITYPE
        }
        // Branch Instruction
        is(InstrDefine.OP_BRANCH) {
            io.branch_op    := True
            io.rs1_rd       := True
            io.rs2_rd       := True
            alu_imm_type    := AluImmTypeE.BTYPE
            switch(func3) {
                is(InstrDefine.BR_F3_BEQ)  {io.bu_opcode := BranchOpcodeEnum.BEQ}
                is(InstrDefine.BR_F3_BNE)  {io.bu_opcode := BranchOpcodeEnum.BNE}
                is(InstrDefine.BR_F3_BLT)  {io.bu_opcode := BranchOpcodeEnum.BLT}
                is(InstrDefine.BR_F3_BGE)  {io.bu_opcode := BranchOpcodeEnum.BGE}
                is(InstrDefine.BR_F3_BLTU) {io.bu_opcode := BranchOpcodeEnum.BLTU}
                is(InstrDefine.BR_F3_BGEU) {io.bu_opcode := BranchOpcodeEnum.BGEU}
                default {ill_instr := True}
            }
        }
        // End of Branch Instruction
        // Memory Load Instruction
        is(InstrDefine.OP_MEM_LOAD) {
            io.op2_sel_imm := True
            io.lsu_rd     := True
            io.rs1_rd      := True
            io.rd_wr       := rd_isnot_x0
            io.alu_opcode  := AluOpcodeEnum.ADD
            io.rd_sel      := RdSelEnum.MEM
            alu_imm_type   := AluImmTypeE.ITYPE
            switch(func3) {
                is(InstrDefine.LW_F3_LB) {io.lsu_ld_byte := True}
                is(InstrDefine.LW_F3_LH) {io.lsu_ld_hword := True}
                is(InstrDefine.LW_F3_LW) {}
                is(InstrDefine.LW_F3_LBU) {
                    io.lsu_ld_byte := True
                    io.lsu_ld_unsign := True
                }
                is(InstrDefine.LW_F3_LHU) {
                    io.lsu_ld_hword := True
                    io.lsu_ld_unsign := True
                }
                default {ill_instr := True}
            }
        }
        // End of Memory Load Instruction
        // Memory Store Instruction
        is(InstrDefine.OP_MEM_STORE) {
            io.alu_opcode  := AluOpcodeEnum.ADD
            io.op2_sel_imm  := True
            io.rs1_rd       := True
            io.rs2_rd       := True
            io.lsu_wr      := True
            alu_imm_type    := AluImmTypeE.STYPE
            switch(func3) {
                is(InstrDefine.SW_F3_SB) {io.lsu_ld_byte := True}
                is(InstrDefine.SW_F3_SH) {io.lsu_ld_hword := True}
                is(InstrDefine.SW_F3_SW) {}
                default {ill_instr := True}
            }
        }
        // End of Memory Store Instruction
        // I-Type Logic/Arithmetic  Instruction
        is(InstrDefine.OP_LOGIC_ARITH_IMM) {
            io.op2_sel_imm  := True
            alu_imm_type    := AluImmTypeE.ITYPE
            io.rs1_rd       := True
            io.rd_wr        := rd_isnot_x0
            switch(func3) {
                is(InstrDefine.LA_F3_ADD)  {io.alu_opcode := AluOpcodeEnum.ADD} // ADDI
                is(InstrDefine.LA_F3_SLT)  {io.alu_opcode := AluOpcodeEnum.SLT}
                is(InstrDefine.LA_F3_SLTU) {io.alu_opcode := AluOpcodeEnum.SLTU}
                is(InstrDefine.LA_F3_XOR)  {io.alu_opcode := AluOpcodeEnum.XOR}
                is(InstrDefine.LA_F3_OR)   {io.alu_opcode := AluOpcodeEnum.OR}
                is(InstrDefine.LA_F3_AND)  {io.alu_opcode := AluOpcodeEnum.AND}
                is(InstrDefine.LA_F3_SLL)  {io.alu_opcode  := AluOpcodeEnum.SLL}
                is(InstrDefine.LA_F3_SR) {
                    switch(func7) {
                        is(InstrDefine.LA_F7_SRL) {io.alu_opcode := AluOpcodeEnum.SRL} // SRLI
                        is(InstrDefine.LA_F7_SRA) {io.alu_opcode := AluOpcodeEnum.SRA} // SRAI
                        default {ill_instr := True}
                    }
                }
                // No default required. Complete switch statement
            }
        }
        // End of I-Type Logic/Arithmetic Instruction
        // R-Type Logic/Arithmetic Instruction
        is(InstrDefine.OP_LOGIC_ARITH) {
            io.rs1_rd  := True
            io.rs2_rd  := True
            io.rd_wr   := rd_isnot_x0
            when(func7_0000000) {
                switch(func3) {
                    is(InstrDefine.LA_F3_ADD)  {io.alu_opcode  := AluOpcodeEnum.ADD}
                    is(InstrDefine.LA_F3_SLL)  {io.alu_opcode := AluOpcodeEnum.SLL}
                    is(InstrDefine.LA_F3_SLT)  {io.alu_opcode := AluOpcodeEnum.SLT}
                    is(InstrDefine.LA_F3_SLTU) {io.alu_opcode := AluOpcodeEnum.SLTU}
                    is(InstrDefine.LA_F3_XOR)  {io.alu_opcode := AluOpcodeEnum.XOR}
                    is(InstrDefine.LA_F3_SR)   {io.alu_opcode := AluOpcodeEnum.SRL}
                    is(InstrDefine.LA_F3_OR)   {io.alu_opcode := AluOpcodeEnum.OR}
                    is(InstrDefine.LA_F3_AND)  {io.alu_opcode := AluOpcodeEnum.AND}
                }
            }.elsewhen(func7_0100000) {
                switch(func3) {
                    is(InstrDefine.LA_F3_SUB) {io.alu_opcode  := AluOpcodeEnum.SUB}
                    is(InstrDefine.LA_F3_SR)  {io.alu_opcode  := AluOpcodeEnum.SRA}
                    default {ill_instr := True}
                }
            }.elsewhen(func7_0000001) {
                // RV32M Instruction
                if (AppleRISCVCfg.USE_RV32M) {
                    switch(func3) {
                        is(InstrDefine.RV32M_MUL) {
                            io.mul_opcode := MulOpcodeEnum.MUL
                            io.mul_op := True
                            io.rd_sel := RdSelEnum.MUL
                        }
                        is(InstrDefine.RV32M_MULH) {
                            io.mul_opcode := MulOpcodeEnum.MULH
                            io.mul_op := True
                            io.rd_sel := RdSelEnum.MUL
                        }
                        is(InstrDefine.RV32M_MULHSU) {
                            io.mul_opcode := MulOpcodeEnum.MULHSU
                            io.mul_op := True
                            io.rd_sel := RdSelEnum.MUL
                        }
                        is(InstrDefine.RV32M_MULHU) {
                            io.mul_opcode := MulOpcodeEnum.MULHU
                            io.mul_op := True
                            io.rd_sel := RdSelEnum.MUL
                        }
                        is(InstrDefine.RV32M_DIV) {
                            io.div_opcode := DivOpcodeEnum.DIV
                            io.div_op := True
                            io.rd_sel := RdSelEnum.DIV
                        }
                        is(InstrDefine.RV32M_DIVU) {
                            io.div_opcode := DivOpcodeEnum.DIVU
                            io.div_op := True
                            io.rd_sel := RdSelEnum.DIV
                        }
                        is(InstrDefine.RV32M_REM) {
                            io.div_opcode := DivOpcodeEnum.REM
                            io.div_op := True
                            io.rd_sel := RdSelEnum.DIV
                        }
                        is(InstrDefine.RV32M_REMU) {
                            io.div_opcode := DivOpcodeEnum.REMU
                            io.div_op := True
                            io.rd_sel := RdSelEnum.DIV
                        }
                    }
                } else {
                    ill_instr := True
                }
            }.otherwise(ill_instr := True)
        }
        // End of R-Type Logic/Arithmetic Instruction
        // SYSTEM Instruction
        is(InstrDefine.OP_SYS) {
            io.rd_sel := RdSelEnum.CSR
            switch(func3) {
                is(InstrDefine.CSR_F3_RW) {
                    io.rd_wr  := rd_isnot_x0
                    io.csr_rd := rd_isnot_x0
                    io.csr_wr := True
                    io.rs1_rd := True
                    io.csr_sel:= CsrSelEnum.DATA
                }
                is(InstrDefine.CSR_F3_RS) {
                    io.rd_wr  := rd_isnot_x0
                    io.csr_rd := True
                    io.csr_wr := rs1_isnot_x0
                    io.rs1_rd := True
                    io.csr_sel:= CsrSelEnum.SET
                }
                is(InstrDefine.CSR_F3_RC) {
                    io.rd_wr  := rd_isnot_x0
                    io.csr_rd := True
                    io.csr_wr := rs1_isnot_x0
                    io.rs1_rd := True
                    io.csr_sel:= CsrSelEnum.CLEAR
                }
                // Note on Immediate operand for CSR
                // the uimm value is in the same location as rs1 in the instruction
                // so we can reuse rs1_idx value as the uimm value
                is(InstrDefine.CSR_F3_RWI) {
                    io.rd_wr  := rd_isnot_x0
                    io.csr_rd := rd_isnot_x0
                    io.csr_wr := True
                    io.csr_sel_imm := True
                    io.csr_sel:= CsrSelEnum.DATA
                }
                is(InstrDefine.CSR_F3_RSI) {
                    io.rd_wr  := rd_isnot_x0
                    io.csr_rd := True
                    io.csr_wr := rs1_isnot_x0
                    io.csr_sel_imm := True
                    io.csr_sel:= CsrSelEnum.SET
                }
                is(InstrDefine.CSR_F3_RCI) {
                    io.rd_wr  := rd_isnot_x0
                    io.csr_rd := True
                    io.csr_wr := rs1_isnot_x0
                    io.csr_sel_imm := True
                    io.csr_sel:= CsrSelEnum.CLEAR
                }
                // SYSTEM Privileged Instruction
                is(InstrDefine.SYS_F3_PRIV) {
                    when (func12 === InstrDefine.F12_MRET) {
                        io.mret := True
                        ill_instr := rs1_isnot_x0 & rd_isnot_x0
                    }.elsewhen(func12 === InstrDefine.F12_ECALL) {
                        io.ecall := True
                        ill_instr := rs1_isnot_x0 & rd_isnot_x0
                    }.otherwise {ill_instr := True}
                }
                default {ill_instr := True}
            }
        } // End of SYSTEM Instruction
        default {ill_instr := True}
    }

    val i_type_imm =  io.instr(31 downto 20).asSInt.resize(AppleRISCVCfg.XLEN)
    val s_type_imm = (io.instr(31 downto 25) ## io.instr(11 downto 7)).asSInt.resize(AppleRISCVCfg.XLEN)
    val u_type_imm = (io.instr(31 downto 12) ## U"12'h0").asSInt
    val b_type_imm = (io.instr(31) ## io.instr(7) ## io.instr(30 downto 25) ## io.instr(11 downto 8) ## False).asSInt.resize(AppleRISCVCfg.XLEN)
    val j_type_imm = (io.instr(31) ## io.instr(19 downto 12) ## io.instr(20) ## io.instr(30 downto 21) ## False).asSInt.resize(AppleRISCVCfg.XLEN)

    switch(alu_imm_type) {
        is(AluImmTypeE.ITYPE) {io.imm_value := i_type_imm}
        is(AluImmTypeE.STYPE) {io.imm_value := s_type_imm}
        is(AluImmTypeE.UTYPE) {io.imm_value := u_type_imm}
        is(AluImmTypeE.JTYPE) {io.imm_value := j_type_imm}
        is(AluImmTypeE.BTYPE) {io.imm_value := b_type_imm}
    }
}