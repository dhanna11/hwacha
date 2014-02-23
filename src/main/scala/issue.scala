package hwacha

import Chisel._
import Node._
import Constants._

class io_vxu_cnt_valid extends ValidIO(Bits(width = SZ_VLEN) )

class io_vxu_issue_to_seq extends Bundle
{
  val vlen = Bits(width = SZ_VLEN)
  val xf_split = Bits(width = SZ_BANK)
  val xstride = Bits(width = SZ_REGLEN)
  val fstride = Bits(width = SZ_REGLEN)
  val bcnt = Bits(width = SZ_BCNT)
}

class io_issue_to_aiw extends Bundle
{
  val markLast = Bool(OUTPUT)
  val update_numCnt = new io_update_num_cnt()
}

class io_issue_to_irq_handler extends Bundle
{
  val tvec = new io_issue_tvec_to_irq_handler()
  val vt = new io_issue_vt_to_irq_handler()
}

class IssueOpIO extends ValidIO(new IssueOp)

class Issue(resetSignal: Bool = null)(implicit conf: HwachaConfiguration) extends Module(_reset = resetSignal)
{
  val io = new Bundle {
    val cfg = new HwachaConfigIO

    val irq = new io_issue_to_irq_handler()
    val vcmdq = new VCMDQIO().flip
    val imem = new rocket.CPUFrontendIO()(conf.vicache)

    val tvec = new Bundle {
      val active = Bool(OUTPUT)
      val ready = Bool(INPUT)
      val op = new IssueOpIO
    }

    val vt = new Bundle {
      val ready = Bool(INPUT)
      val op = new IssueOpIO
    }
    
    val pending_vf = Bool(OUTPUT)

    val aiw_cmdb = new io_vxu_cmdq()
    val aiw_imm1b = new io_vxu_immq()
    val aiw_imm2b = new io_vxu_imm2q()
    val aiw_cntb = new io_vxu_cntq()
    val aiw_numCntB = new io_vxu_numcntq()

    val issue_to_aiw = new io_issue_to_aiw()
    val aiw_to_issue = new io_aiw_to_issue().flip

    val xcpt_to_issue = new io_xcpt_handler_to_issue().flip()
  }

  val tvec = Module(new IssueTVEC)
  val vt = Module(new IssueVT)

  io.cfg <> tvec.io.cfg
  vt.io.cfg <> tvec.io.cfg
  io.irq.tvec := tvec.io.irq
  io.irq.vt := vt.io.irq
  vt.io.vf <> tvec.io.vf
  io.pending_vf := tvec.io.vf.active

  // vcmdq
  io.vcmdq.cnt.ready := tvec.io.vcmdq.cnt.ready || vt.io.vcmdq.cnt.ready
  tvec.io.vcmdq.cmd <> io.vcmdq.cmd
  tvec.io.vcmdq.imm1 <> io.vcmdq.imm1
  tvec.io.vcmdq.imm2 <> io.vcmdq.imm2
  tvec.io.vcmdq.cnt.valid := io.vcmdq.cnt.valid
  tvec.io.vcmdq.cnt.bits := io.vcmdq.cnt.bits
  vt.io.vcmdq.cnt.valid := io.vcmdq.cnt.valid
  vt.io.vcmdq.cnt.bits := io.vcmdq.cnt.bits

  // imem
  vt.io.imem <> io.imem

  // issue op
  io.tvec.active := tvec.io.active
  io.tvec.op <> tvec.io.op
  tvec.io.ready <> io.tvec.ready
  io.vt.op <> vt.io.op
  vt.io.ready <> io.vt.ready

  // aiw
  tvec.io.aiw_cmdb <> io.aiw_cmdb
  tvec.io.aiw_imm1b <> io.aiw_imm1b
  tvec.io.aiw_imm2b <> io.aiw_imm2b
  tvec.io.aiw_cntb.ready := io.aiw_cntb.ready
  tvec.io.aiw_numCntB <> io.aiw_numCntB
  tvec.io.aiw_to_issue <> io.aiw_to_issue
  vt.io.aiw_cntb.ready := io.aiw_cntb.ready
  vt.io.aiw_to_issue <> io.aiw_to_issue
  vt.io.issue_to_aiw.update_numCnt <> io.issue_to_aiw.update_numCnt
  io.issue_to_aiw.markLast := Mux(tvec.io.active, tvec.io.issue_to_aiw.markLast, vt.io.issue_to_aiw.markLast)
  io.aiw_cntb.valid := Mux(tvec.io.active, tvec.io.aiw_cntb.valid, vt.io.aiw_cntb.valid)
  io.aiw_cntb.bits := Mux(tvec.io.active, tvec.io.aiw_cntb.bits, vt.io.aiw_cntb.bits)

  // xcpt
  tvec.io.xcpt_to_issue <> io.xcpt_to_issue
  vt.io.xcpt_to_issue <> io.xcpt_to_issue
}
