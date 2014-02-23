package hwacha

import Chisel._
import Node._
import Constants._
import Commands._
import rocket.Instructions._
import Instructions._
import uncore.constants.MemoryOpConstants._

class io_vf extends Bundle
{
  val active = Bool(OUTPUT)
  val fire = Bool(OUTPUT)
  val stop = Bool(INPUT)
  val pc = UInt(OUTPUT, SZ_ADDR)
  val imm1_rtag = Bits(OUTPUT, SZ_AIW_IMM1)
  val numCnt_rtag = Bits(OUTPUT, SZ_AIW_NUMCNT)
  val vlen = Bits(OUTPUT, SZ_VLEN)
}

class io_issue_vt_to_irq_handler extends Bundle
{
  val ma_inst = Bool(OUTPUT)
  val fault_inst = Bool(OUTPUT)
  val illegal = Bool(OUTPUT)
  val pc = Bits(OUTPUT, SZ_ADDR)
}

object VTDecodeTable
{
                //   vd vs vt vr i     VIUfn                   VAU0fn         VAU1fn         VAU2fn         VMUfn                   decode_stop
                //   |  |  |  |  |     |                       |              |              |              |                       |
  val default = List(R_,R_,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F)
  val table = Array(
    UTIDX->     List(RX,R_,R_,R_,IMM_X,T,M0,M0,DW64,FP_,I_IDX, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    MOVZ->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_MOVZ,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    MOVN->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_MOVN,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMOVZ->     List(RF,RX,RF,R_,IMM_X,T,ML,MR,DW64,FP_,I_MOVZ,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMOVN->     List(RF,RX,RF,R_,IMM_X,T,ML,MR,DW64,FP_,I_MOVN,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    LUI->       List(RX,R_,R_,R_,IMM_U,T,M0,MI,DW64,FP_,I_MOV, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    ADDI->      List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_ADD, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLLI->      List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_SLL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLTI->      List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_SLT, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLTIU->     List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_SLTU,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    XORI->      List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_XOR, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRLI->      List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_SRL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRAI->      List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_SRA, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    ORI->       List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_OR,  F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    ANDI->      List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW64,FP_,I_AND, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    ADD->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_ADD, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SUB->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_SUB, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLL->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_SLL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLT->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_SLT, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLTU->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_SLTU,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    XOR->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_XOR, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRL->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_SRL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRA->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_SRA, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    OR->        List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_OR,  F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    AND->       List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW64,FP_,I_AND, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    ADDIW->     List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW32,FP_,I_ADD, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLLIW->     List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW32,FP_,I_SLL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRLIW->     List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW32,FP_,I_SRL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRAIW->     List(RX,RX,R_,R_,IMM_I,T,MR,MI,DW32,FP_,I_SRA, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    ADDW->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW32,FP_,I_ADD, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SUBW->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW32,FP_,I_SUB, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SLLW->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW32,FP_,I_SLL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRLW->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW32,FP_,I_SRL, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    SRAW->      List(RX,RX,RX,R_,IMM_X,T,ML,MR,DW32,FP_,I_SRA, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    FSGNJ_S->   List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FSJ, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSGNJN_S->  List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FSJN,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSGNJX_S->  List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FSJX,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FEQ_S->     List(RX,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FEQ, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FLT_S->     List(RX,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FLT, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FLE_S->     List(RX,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FLE, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMIN_S->    List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FMIN,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMAX_S->    List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPS,I_FMAX,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSGNJ_D->   List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FSJ, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSGNJN_D->  List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FSJN,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSGNJX_D->  List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FSJX,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FEQ_D->     List(RX,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FEQ, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FLT_D->     List(RX,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FLT, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FLE_D->     List(RX,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FLE, F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMIN_D->    List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FMIN,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMAX_D->    List(RF,RF,RF,R_,IMM_X,T,ML,MR,DW64,FPD,I_FMAX,F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    MUL->       List(RX,RX,RX,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   T,DW64,A0_M,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    MULH->      List(RX,RX,RX,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   T,DW64,A0_MH,  F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    MULHU->     List(RX,RX,RX,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   T,DW64,A0_MHU, F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    MULHSU->    List(RX,RX,RX,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   T,DW64,A0_MHSU,F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    MULW->      List(RX,RX,RX,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   T,DW32,A0_M,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    FADD_H->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPH,A1_ADD,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSUB_H->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPH,A1_SUB,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMUL_H->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPH,A1_MUL,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FADD_S->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPS,A1_ADD,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSUB_S->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPS,A1_SUB,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMUL_S->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPS,A1_MUL,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMADD_S->   List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPS,A1_MADD, F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMSUB_S->   List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPS,A1_MSUB, F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FNMSUB_S->  List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPS,A1_NMSUB,F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FNMADD_S->  List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPS,A1_NMADD,F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FADD_D->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPD,A1_ADD,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FSUB_D->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPD,A1_SUB,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMUL_D->    List(RF,RF,RF,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPD,A1_MUL,  F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMADD_D->   List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPD,A1_MADD, F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FMSUB_D->   List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPD,A1_MSUB, F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FNMSUB_D->  List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPD,A1_NMSUB,F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),
    FNMADD_D->  List(RF,RF,RF,RF,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   T,FPD,A1_NMADD,F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  F),

    FCVT_S_D->  List(RF,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CDTS, F,MT_X,M_X,      VM_X,  F),
    FCVT_D_S->  List(RF,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CSTD, F,MT_X,M_X,      VM_X,  F),
    FCVT_L_S->  List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CFTL, F,MT_X,M_X,      VM_X,  F),
    FCVT_LU_S-> List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CFTLU,F,MT_X,M_X,      VM_X,  F),
    FCVT_W_S->  List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CFTW, F,MT_X,M_X,      VM_X,  F),
    FCVT_WU_S-> List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CFTWU,F,MT_X,M_X,      VM_X,  F),
    FCVT_S_L->  List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CLTF, F,MT_X,M_X,      VM_X,  F),
    FCVT_S_LU-> List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CLUTF,F,MT_X,M_X,      VM_X,  F),
    FCVT_S_W->  List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CWTF, F,MT_X,M_X,      VM_X,  F),
    FCVT_S_WU-> List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_CWUTF,F,MT_X,M_X,      VM_X,  F),
    FMV_S_X->   List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_MXTF, F,MT_X,M_X,      VM_X,  F),
    FMV_X_S->   List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPS,A2_MFTX, F,MT_X,M_X,      VM_X,  F),
    FCVT_L_D->  List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CFTL, F,MT_X,M_X,      VM_X,  F),
    FCVT_LU_D-> List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CFTLU,F,MT_X,M_X,      VM_X,  F),
    FCVT_W_D->  List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CFTW, F,MT_X,M_X,      VM_X,  F),
    FCVT_WU_D-> List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CFTWU,F,MT_X,M_X,      VM_X,  F),
    FCVT_D_L->  List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CLTF, F,MT_X,M_X,      VM_X,  F),
    FCVT_D_LU-> List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CLUTF,F,MT_X,M_X,      VM_X,  F),
    FCVT_D_W->  List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CWTF, F,MT_X,M_X,      VM_X,  F),
    FCVT_D_WU-> List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_CWUTF,F,MT_X,M_X,      VM_X,  F),
    FMV_D_X->   List(RF,RX,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_MXTF, F,MT_X,M_X,      VM_X,  F),
    FMV_X_D->   List(RX,RF,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    T,FPD,A2_MFTX, F,MT_X,M_X,      VM_X,  F),

    LB->        List(RX,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_B,M_XRD,    VM_ULD,F),
    LH->        List(RX,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_H,M_XRD,    VM_ULD,F),
    LW->        List(RX,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XRD,    VM_ULD,F),
    LD->        List(RX,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XRD,    VM_ULD,F),
    LBU->       List(RX,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_BU,M_XRD,   VM_ULD,F),
    LHU->       List(RX,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_HU,M_XRD,   VM_ULD,F),
    LWU->       List(RX,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_WU,M_XRD,   VM_ULD,F),
    SB->        List(R_,RX,RX,R_,IMM_S,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_B,M_XWR,    VM_UST,F),
    SH->        List(R_,RX,RX,R_,IMM_S,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_H,M_XWR,    VM_UST,F),
    SW->        List(R_,RX,RX,R_,IMM_S,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XWR,    VM_UST,F),
    SD->        List(R_,RX,RX,R_,IMM_S,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XWR,    VM_UST,F),
    AMOADD_W->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_ADD, VM_AMO,F),
    AMOXOR_W->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_XOR, VM_AMO,F),
    AMOOR_W->   List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_OR,  VM_AMO,F),
    AMOAND_W->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_AND, VM_AMO,F),
    AMOMIN_W->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_MIN, VM_AMO,F),
    AMOMAX_W->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_MAX, VM_AMO,F),
    AMOMINU_W-> List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_MINU,VM_AMO,F),
    AMOMAXU_W-> List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_MAXU,VM_AMO,F),
    AMOSWAP_W-> List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XA_SWAP,VM_AMO,F),
    AMOADD_D->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_ADD, VM_AMO,F),
    AMOXOR_D->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_XOR, VM_AMO,F),
    AMOOR_D->   List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_OR,  VM_AMO,F),
    AMOAND_D->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_AND, VM_AMO,F),
    AMOMIN_D->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_MIN, VM_AMO,F),
    AMOMAX_D->  List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_MAX, VM_AMO,F),
    AMOMINU_D-> List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_MINU,VM_AMO,F),
    AMOMAXU_D-> List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_MAXU,VM_AMO,F),
    AMOSWAP_D-> List(RX,RX,RX,R_,IMM_0,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XA_SWAP,VM_AMO,F),
    FLH->       List(RF,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_H,M_XRD,    VM_ULD,F),
    FLW->       List(RF,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XRD,    VM_ULD,F),
    FLD->       List(RF,RX,R_,R_,IMM_I,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XRD,    VM_ULD,F),
    FSH->       List(R_,RX,RF,R_,IMM_S,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_H,M_XWR,    VM_UST,F),
    FSW->       List(R_,RX,RF,R_,IMM_S,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_W,M_XWR,    VM_UST,F),
    FSD->       List(R_,RX,RF,R_,IMM_S,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    T,MT_D,M_XWR,    VM_UST,F),

    STOP->      List(R_,R_,R_,R_,IMM_X,F,M0,M0,DW__,FP_,I_X,   F,DW__,A0_X,   F,FP_,A1_X,    F,FP_,A2_X,    F,MT_X,M_X,      VM_X,  T)
  )
}

class IssueVT(implicit conf: HwachaConfiguration) extends Module
{
  val io = new Bundle {
    val cfg = new HwachaConfigIO().flip
    val vf = new io_vf().flip

    val irq = new io_issue_vt_to_irq_handler()
    val vcmdq = new VCMDQIO().flip
    val imem = new rocket.CPUFrontendIO()(conf.vicache)

    val ready = Bool(INPUT)
    val op = new IssueOpIO

    val aiw_cntb = new io_vxu_cntq()
    val issue_to_aiw = new io_issue_to_aiw()
    val aiw_to_issue = new io_aiw_to_issue().flip

    val xcpt_to_issue = new io_xcpt_handler_to_issue().flip()
  }

  val stall_sticky = Reg(init=Bool(false))
  val mask_stall = Bool()

  val stall_issue = stall_sticky || io.irq.illegal || io.xcpt_to_issue.stall
  val stall_frontend = stall_issue || !(io.ready && (mask_stall || io.aiw_cntb.ready)) || io.irq.ma_inst || io.irq.fault_inst

  when (io.irq.ma_inst || io.irq.fault_inst || io.irq.illegal) { stall_sticky := Bool(true) }


//-------------------------------------------------------------------------\\
// DECODE                                                                  \\
//-------------------------------------------------------------------------\\

  val imm1_rtag = Reg(init=Bits(0,SZ_AIW_IMM1))
  val numCnt_rtag = Reg(init=Bits(0,SZ_AIW_CMD))

  when (io.vf.fire) { 
    imm1_rtag := io.vf.imm1_rtag
    numCnt_rtag := io.vf.numCnt_rtag
  }

  io.imem.req.valid := io.vf.fire
  io.imem.req.bits.pc := io.vf.pc
  io.imem.req.bits.mispredict := Bool(false)
  io.imem.req.bits.taken := Bool(false)
  io.imem.invalidate := Bool(false)
  io.imem.resp.ready := io.vf.active && !stall_frontend
  val inst = io.imem.resp.bits.data

  val cs = rocket.DecodeLogic(inst, VTDecodeTable.default, VTDecodeTable.table)

  val vdi :: vsi :: vti :: vri :: immi :: cs0 = cs
  val (viu_val: Bool) :: viu_t0 :: viu_t1 :: viu_dw :: viu_fp :: viu_op :: cs1 = cs0
  val (vau0_val: Bool) :: vau0_dw :: vau0_op :: cs2 = cs1
  val (vau1_val: Bool) :: vau1_fp :: vau1_op :: cs3 = cs2
  val (vau2_val: Bool) :: vau2_fp :: vau2_op :: cs4 = cs3
  val (vmu_val: Bool) :: vmu_type :: vmu_cmd :: vmu_op :: cs5 = cs4
  val (decode_stop: Bool) :: Nil = cs5

  val vd_val :: vd_fp :: Nil = parse_rinfo(vdi)
  val vs_val :: vs_fp :: Nil = parse_rinfo(vsi)
  val vt_val :: vt_fp :: Nil = parse_rinfo(vti)
  val vr_val :: vr_fp :: Nil = parse_rinfo(vri)

  val unmasked_valid_viu = viu_val
  val unmasked_valid_vau0 = vau0_val
  val unmasked_valid_vau1 = vau1_val
  val unmasked_valid_vau2 = vau2_val
  val unmasked_valid_amo = vmu_val && (vmu_op === VM_AMO)
  val unmasked_valid_utld = vmu_val && (vmu_op === VM_ULD)
  val unmasked_valid_utst = vmu_val && (vmu_op === VM_UST)

  val unmasked_valid =
    io.imem.resp.valid && (viu_val || vau0_val || vau1_val || vau2_val || vmu_val)

  io.vf.stop := io.vf.active && io.imem.resp.valid && decode_stop

  val rm = Bits(width = 3)
  rm := inst(14,12)
  when (inst(14,12) === Bits("b111",3)) {
    rm := Bits(0,3)
  }

  val vs = inst(19,15) // rs1
  val vt = inst(24,20) // rs2
  val vr = inst(31,27) // rs3
  val vd = inst(11, 7) // rd

  val imm = MuxLookup(
    immi, Bits(0,SZ_DATA), Array(
      IMM_0 -> Bits(0,65),
      IMM_I -> Cat(Bits(0,1),Fill(52,inst(31)),inst(31,20)),
      IMM_S -> Cat(Bits(0,1),Fill(52,inst(31)),inst(31,25),inst(11,7)),
      IMM_U -> Cat(Bits(0,1),Fill(32,inst(31)),inst(31,12),Bits(0,12))
    ))

  val cnt = Mux(io.vcmdq.cnt.valid, io.vcmdq.cnt.bits, UInt(0))
  val regid_xbase = (cnt >> UInt(3)) * io.cfg.xstride
  val regid_fbase = ((cnt >> UInt(3)) * io.cfg.fstride) + io.cfg.xfsplit


//-------------------------------------------------------------------------\\
// FIRE & QUEUE LOGIC                                                      \\
//-------------------------------------------------------------------------\\

  val fire = io.vf.active && io.ready && unmasked_valid && !io.op.bits.reg.vd.zero && !stall_issue

  io.vcmdq.cnt.ready := fire
  io.aiw_cntb.valid := fire
  io.aiw_cntb.bits := cnt

  io.issue_to_aiw.markLast := io.vf.stop
  io.issue_to_aiw.update_numCnt.valid := fire && io.aiw_cntb.ready
  io.issue_to_aiw.update_numCnt.bits := numCnt_rtag


//-------------------------------------------------------------------------\\
// ISSUE                                                                   \\
//-------------------------------------------------------------------------\\

  io.op.valid := io.vf.active && io.imem.resp.valid && io.aiw_cntb.ready && !stall_issue

  io.op.bits.active.viu := viu_val
  io.op.bits.active.vau0 := vau0_val
  io.op.bits.active.vau1 := vau1_val
  io.op.bits.active.vau2 := vau2_val
  io.op.bits.active.amo := vmu_val && (vmu_op === VM_AMO)
  io.op.bits.active.utld := vmu_val && (vmu_op === VM_ULD)
  io.op.bits.active.utst := vmu_val && (vmu_op === VM_UST)
  io.op.bits.active.vld := Bool(false)
  io.op.bits.active.vst := Bool(false)

  io.op.bits.vlen := io.vf.vlen - cnt
  io.op.bits.utidx := cnt

  val vmu_float = vmu_op === VM_ULD && vd_fp || vmu_op === VM_UST && vt_fp

  io.op.bits.fn.viu := new VIUFn().fromBits(Cat(viu_t0, viu_t1, viu_dw, viu_fp, viu_op))
  io.op.bits.fn.vau0 := new VAU0Fn().fromBits(Cat(vau0_dw, vau0_op))
  io.op.bits.fn.vau1 := new VAU1Fn().fromBits(Cat(vau1_fp, rm, vau1_op))
  io.op.bits.fn.vau2 := new VAU2Fn().fromBits(Cat(vau2_fp, rm, vau2_op))
  io.op.bits.fn.vmu := new VMUFn().fromBits(Cat(vmu_float, vmu_type, vmu_cmd, vmu_op))

  val vs_m1 = vs - UInt(1)
  val vt_m1 = vt - UInt(1)
  val vr_m1 = vr - UInt(1)
  val vd_m1 = vd - UInt(1)

  mask_stall := decode_stop

  io.op.bits.reg.vs.active := vs_val
  io.op.bits.reg.vt.active := vt_val
  io.op.bits.reg.vr.active := vr_val
  io.op.bits.reg.vd.active := vd_val
  io.op.bits.reg.vs.zero := !vs_fp && vs === UInt(0)
  io.op.bits.reg.vt.zero := !vt_fp && vt === UInt(0)
  io.op.bits.reg.vr.zero := !vr_fp && vr === UInt(0)
  io.op.bits.reg.vd.zero := !vd_fp && vd === UInt(0) && vd_val || mask_stall
  io.op.bits.reg.vs.float := vs_fp
  io.op.bits.reg.vt.float := vt_fp
  io.op.bits.reg.vr.float := vr_fp
  io.op.bits.reg.vd.float := vd_fp
  io.op.bits.reg.vs.id := Mux(vs_fp, vs + regid_fbase, vs_m1 + regid_xbase)
  io.op.bits.reg.vt.id := Mux(vt_fp, vt + regid_fbase, vt_m1 + regid_xbase)
  io.op.bits.reg.vr.id := Mux(vr_fp, vr + regid_fbase, vr_m1 + regid_xbase)
  io.op.bits.reg.vd.id := Mux(vd_fp, vd + regid_fbase, vd_m1 + regid_xbase)

  io.op.bits.imm.imm := imm

  io.op.bits.aiw.active.imm1 := Bool(true)
  io.op.bits.aiw.active.cnt := Bool(true)
  io.op.bits.aiw.imm1.rtag := imm1_rtag
  io.op.bits.aiw.imm1.pc_next := io.imem.resp.bits.pc + UInt(4)
  io.op.bits.aiw.cnt.rtag := io.aiw_to_issue.cnt_rtag
  io.op.bits.aiw.cnt.utidx := cnt
  io.op.bits.aiw.numcnt.rtag := numCnt_rtag


//-------------------------------------------------------------------------\\
// IRQ                                                                     \\
//-------------------------------------------------------------------------\\

  val illegal_vd = vd_val && (vd >= io.cfg.nfregs && vd_fp || vd >= io.cfg.nxregs && !vd_fp)
  val illegal_vt = vt_val && (vt >= io.cfg.nfregs && vt_fp || vt >= io.cfg.nxregs && !vt_fp)
  val illegal_vs = vs_val && (vs >= io.cfg.nfregs && vs_fp || vs >= io.cfg.nxregs && !vs_fp)
  val illegal_vr = vr_val && (vr >= io.cfg.nfregs && vr_fp || vr >= io.cfg.nxregs && !vr_fp)

  io.irq.ma_inst := io.vf.active && io.imem.resp.valid && io.imem.resp.bits.xcpt_ma
  io.irq.fault_inst := io.vf.active && io.imem.resp.valid && io.imem.resp.bits.xcpt_if
  io.irq.illegal := io.vf.active && io.imem.resp.valid && (!unmasked_valid && !mask_stall || illegal_vd || illegal_vt || illegal_vs || illegal_vr)
  io.irq.pc := io.imem.resp.bits.pc
}
