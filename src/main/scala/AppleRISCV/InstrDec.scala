///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: instr_dec
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

case class InstrDecIO() extends Bundle{
    val instr     = in Bits(AppleRISCVCfg.XLEN bits)   // instruction input
    val instr_vld = in Bool

    // Instruction field
    val rd_idx  = out UInt(5 bits)
    val rs1_idx = out UInt(5 bits)
    val rs2_idx = out UInt(5 bits)
    val imm_value = out SInt(AppleRISCVCfg.XLEN bits)   // Immediate value

    // Register file control
    val rd_wr  = out Bool
    val rs1_rd = out Bool
    val rs2_rd = out Bool

    // Memory control signal
    val dmem_wr         = out Bool
    val dmem_rd         = out Bool
    val dmem_ld_byte    = out Bool         // load/store byte
    val dmem_ld_hword   = out Bool         // load/store half word
    val dmem_ld_unsign  = out Bool         // load unsigned

    // ALU control
    val alu_opcode = out(AluOpcodeEnum())

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

    // data selection
    val wb_sel = out(WbSelEnum())
    val csr_sel  = out(CsrSelEnum())

    // Other control signal
    val op2_sel_imm  = out Bool
    val op1_sel_zero = out Bool
    val op1_sel_pc   = out Bool

    // Exception
    val exc_ill_instr  = out Bool
}

case class InstrDec() extends Component {

    noIoPrefix()

    val io = InstrDecIO()

    // ============================================
    // Extract each field from the instruction
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

    object alu_imm_type_e extends SpinalEnum {
        val ITYPE, STYPE, UTYPE, JTYPE, BTYPE = newElement()
    }

    val alu_imm_type = alu_imm_type_e()
    alu_imm_type := alu_imm_type_e.ITYPE

    // intermediate logic
    val func7_not_all_zero  = func7 =/= 0
    val rd_isnot_x0         = io.rd_idx =/= 0
    val rs1_isnot_x0        = io.rs1_idx =/= 0
    val ill_instr           = False
    
    io.exc_ill_instr := ill_instr & io.instr_vld
    
    // default value
    io.rd_wr          := False
    io.rs1_rd         := False
    io.rs2_rd         := False
    io.dmem_wr        := False
    io.dmem_rd        := False
    io.dmem_ld_byte   := False
    io.dmem_ld_hword  := False
    io.dmem_ld_unsign := False
    io.alu_opcode     := AluOpcodeEnum.AND
    io.bu_opcode      := BranchOpcodeEnum.BEQ
    io.wb_sel         := WbSelEnum.ALU
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

    // The big switch for the opcode decode
    switch(opcode) {
        is(InstrDefine.OP_LUI) {
            io.alu_opcode  := AluOpcodeEnum.ADD
            io.op2_sel_imm := True
            io.op1_sel_zero := True
            io.rd_wr       := rd_isnot_x0
            alu_imm_type   := alu_imm_type_e.UTYPE
        }
        is(InstrDefine.OP_AUIPC) {
            io.alu_opcode  := AluOpcodeEnum.ADD
            io.op2_sel_imm := True
            io.op1_sel_pc  := True
            io.rd_wr       := rd_isnot_x0
            alu_imm_type   := alu_imm_type_e.UTYPE
        }
        is(InstrDefine.OP_JAL) {
            io.alu_opcode  := AluOpcodeEnum.PC4
            io.op2_sel_imm := True
            io.rd_wr       := rd_isnot_x0
            io.jal_op      := True
            alu_imm_type   := alu_imm_type_e.JTYPE
        }
        is(InstrDefine.OP_JALR) {
            io.jalr_op      := True
            io.alu_opcode   := AluOpcodeEnum.PC4
            io.op2_sel_imm  := True
            io.rd_wr        := rd_isnot_x0
            io.rs1_rd       := True
            alu_imm_type    := alu_imm_type_e.ITYPE
        }
        // Branch Instruction
        is(InstrDefine.OP_BRANCH) {
            io.branch_op    := True
            io.rs1_rd       := True
            io.rs2_rd       := True
            alu_imm_type    := alu_imm_type_e.BTYPE
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
            io.dmem_rd     := True
            io.rs1_rd      := True
            io.rd_wr       := rd_isnot_x0
            io.alu_opcode  := AluOpcodeEnum.ADD
            io.wb_sel      := WbSelEnum.MEM
            alu_imm_type   := alu_imm_type_e.ITYPE
            switch(func3) {
                is(InstrDefine.LW_F3_LB) {io.dmem_ld_byte := True}
                is(InstrDefine.LW_F3_LH) {io.dmem_ld_hword := True}
                is(InstrDefine.LW_F3_LW) {}
                is(InstrDefine.LW_F3_LBU) {
                    io.dmem_ld_byte := True
                    io.dmem_ld_unsign := True
                }
                is(InstrDefine.LW_F3_LHU) {
                    io.dmem_ld_hword := True
                    io.dmem_ld_unsign := True
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
            io.dmem_wr      := True
            alu_imm_type    := alu_imm_type_e.STYPE
            switch(func3) {
                is(InstrDefine.SW_F3_SB) {io.dmem_ld_byte := True}
                is(InstrDefine.SW_F3_SH) {io.dmem_ld_hword := True}
                is(InstrDefine.SW_F3_SW) {}
                default {ill_instr := True}
            }
        }
        // End of Memory Store Instruction
        // I-Type Logic/Arithmetic  Instruction
        is(InstrDefine.OP_LOGIC_ARITH_IMM) {
            io.op2_sel_imm  := True
            alu_imm_type    := alu_imm_type_e.ITYPE
            io.rs1_rd       := True
            io.rd_wr        := rd_isnot_x0
            switch(func3) {
                is(InstrDefine.LA_F3_ADD_SUB) {io.alu_opcode := AluOpcodeEnum.ADD} // ADDI
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
            switch(func3) {
                is(InstrDefine.LA_F3_ADD_SUB) {
                    switch(func7) {
                        is(InstrDefine.LA_F7_ADD) {io.alu_opcode  := AluOpcodeEnum.ADD} // ADD
                        is(InstrDefine.LA_F7_SUB) {io.alu_opcode  := AluOpcodeEnum.SUB} // SUB
                        default {ill_instr := True}
                    }
                }
                is(InstrDefine.LA_F3_SLL) {
                    io.alu_opcode  := AluOpcodeEnum.SLL
                    ill_instr  := func7_not_all_zero
                }
                is(InstrDefine.LA_F3_SLT) {
                    io.alu_opcode  := AluOpcodeEnum.SLT
                    ill_instr  := func7_not_all_zero
                }
                is(InstrDefine.LA_F3_SLTU) {
                    io.alu_opcode  := AluOpcodeEnum.SLTU
                    ill_instr  := func7_not_all_zero
                }
                is(InstrDefine.LA_F3_XOR) {
                    io.alu_opcode   := AluOpcodeEnum.XOR
                    ill_instr := func7_not_all_zero
                }
                is(InstrDefine.LA_F3_SR) {
                    switch(func7) {
                        is(InstrDefine.LA_F7_SRL) {io.alu_opcode  := AluOpcodeEnum.SRL}
                        is(InstrDefine.LA_F7_SRA) {io.alu_opcode  := AluOpcodeEnum.SRA}
                        default {ill_instr := True}
                    }
                }
                // ORI
                is(InstrDefine.LA_F3_OR) {
                    io.alu_opcode := AluOpcodeEnum.OR
                    ill_instr  := func7_not_all_zero
                }
                // AND
                is(InstrDefine.LA_F3_AND) {
                    io.alu_opcode := AluOpcodeEnum.AND
                    ill_instr := func7_not_all_zero
                }
                // No default required. Complete switch statement
            }
        }
        // End of R-Type Logic/Arithmetic Instruction
        // SYSTEM Instruction
        is(InstrDefine.OP_SYS) {
            io.wb_sel := WbSelEnum.CSR
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
        is(alu_imm_type_e.ITYPE) {io.imm_value := i_type_imm}
        is(alu_imm_type_e.STYPE) {io.imm_value := s_type_imm}
        is(alu_imm_type_e.UTYPE) {io.imm_value := u_type_imm}
        is(alu_imm_type_e.JTYPE) {io.imm_value := j_type_imm}
        is(alu_imm_type_e.BTYPE) {io.imm_value := b_type_imm}
    }
}