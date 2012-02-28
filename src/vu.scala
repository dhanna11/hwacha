package hwacha

import Chisel._
import Node._
import Constants._
import queues._

class io_vu extends Bundle 
{
  val illegal = Bool(OUTPUT)

  val vec_cmdq = new io_vec_cmdq().flip()
  val vec_ximm1q = new io_vec_ximm1q().flip()
  val vec_ximm2q = new io_vec_ximm2q().flip()
  val vec_cntq = new io_vec_cntq().flip()
  val vec_ackq = new io_vec_ackq()

  val vec_pfcmdq = new io_vec_cmdq().flip()
  val vec_pfximm1q = new io_vec_ximm1q().flip()
  val vec_pfximm2q = new io_vec_ximm2q().flip()

  val cp_imul_req = new io_imul_req().flip()
  val cp_imul_resp = Bits(SZ_XLEN, OUTPUT)
  val cp_dfma = new io_cp_dfma()
  val cp_sfma = new io_cp_sfma()
  
  val imem_req = new io_imem_req()
  val imem_resp = new io_imem_resp().flip()

  val dmem_req = new io_dmem_req()
  val dmem_resp = new io_dmem_resp().flip()

  val cpu_exception = new io_cpu_exception().flip()

  val vec_tlb_req = new ioDTLB_CPU_req()
  val vec_tlb_resp = new ioDTLB_CPU_resp().flip()

  val vec_pftlb_req = new ioDTLB_CPU_req()
  val vec_pftlb_resp = new ioDTLB_CPU_resp().flip()
}

class vu extends Component
{
  val io = new io_vu()

  val vcmdq = new queueSimplePF(16)({Bits(width=SZ_VCMD)})
  val vximm1q = new queueSimplePF(16)({Bits(width=SZ_VIMM)})
  val vximm2q = new queueSimplePF(16)({Bits(width=SZ_VSTRIDE)})
  val vxcntq = new queueSimplePF(16)( Bits(width=SZ_VLEN) )

  vcmdq.io.enq <> io.vec_cmdq
  vximm1q.io.enq <> io.vec_ximm1q
  vximm2q.io.enq <> io.vec_ximm2q
  vxcntq.io.enq <> io.vec_cntq
  
  val vpfcmdq = new queueSimplePF(16)({Bits(width=SZ_VCMD)})
  val vpfximm1q = new queueSimplePF(16)({Bits(width=SZ_VIMM)})
  val vpfximm2q = new queueSimplePF(16)({Bits(width=SZ_VSTRIDE)})

  vpfcmdq.io.enq <> io.vec_pfcmdq
  vpfximm1q.io.enq <> io.vec_pfximm1q
  vpfximm2q.io.enq <> io.vec_pfximm2q

  val vru = new vuVRU()
  val vxu = new vuVXU()
  val evac = new vuEvac()

  // vxu
  io.illegal <> vxu.io.illegal

  vxu.io.vxu_cmdq.bits := vcmdq.io.deq.bits
  vxu.io.vxu_cmdq.valid := vcmdq.io.deq.valid
  vcmdq.io.deq.ready := vxu.io.vxu_cmdq.ready || evac.io.vcmdq.ready

  vxu.io.vxu_immq.bits := vximm1q.io.deq.bits
  vxu.io.vxu_immq.valid := vximm1q.io.deq.valid
  vximm1q.io.deq.ready := vxu.io.vxu_immq.ready || evac.io.vimm1q.ready

  vxu.io.vxu_imm2q.bits := vximm2q.io.deq.bits
  vxu.io.vxu_imm2q.valid := vximm2q.io.deq.valid
  vximm2q.io.deq.ready := vxu.io.vxu_imm2q.ready || evac.io.vimm2q.ready

  vxu.io.vxu_cntq.bits := vxcntq.io.deq.bits
  vxu.io.vxu_cntq.valid := vxcntq.io.deq.valid
  vxcntq.io.deq.ready := vxu.io.vxu_cntq.ready || evac.io.vcntq.ready

  vxu.io.vec_ackq <> io.vec_ackq

  vxu.io.cp_imul_req <> io.cp_imul_req
  vxu.io.cp_imul_resp <> io.cp_imul_resp
  io.cp_dfma <> vxu.io.cp_dfma
  io.cp_sfma <> vxu.io.cp_sfma

  vxu.io.imem_req <> io.imem_req
  vxu.io.imem_resp <> io.imem_resp

  vxu.io.cpu_exception <> io.cpu_exception

  // vru
  vru.io.vec_pfcmdq <> vpfcmdq.io.deq
  vru.io.vec_pfximm1q <> vpfximm1q.io.deq
  vru.io.vec_pfximm2q <> vpfximm2q.io.deq
 
  val irb = new vuIRB()

  // irb
  irb.io.irb_enq_cmdb <> vxu.io.irb_cmdb
  irb.io.irb_enq_imm1b <> vxu.io.irb_imm1b
  irb.io.irb_enq_imm2b <> vxu.io.irb_imm2b
  irb.io.irb_enq_cntb <> vxu.io.irb_cntb

  irb.io.issue_to_irb <> vxu.io.issue_to_irb
  irb.io.irb_to_issue <> vxu.io.irb_to_issue
  irb.io.seq_to_irb <> vxu.io.seq_to_irb

  // evac
  evac.io.cpu_exception <> io.cpu_exception

  evac.io.irb_cmdb <> irb.io.irb_deq_cmdb
  evac.io.irb_imm1b <> irb.io.irb_deq_imm1b
  evac.io.irb_imm2b <> irb.io.irb_deq_imm2b
  evac.io.irb_cntb <> irb.io.irb_deq_cntb
  evac.io.irb_cntb_last <> irb.io.irb_deq_cntb_last

  evac.io.vcmdq.bits := vcmdq.io.deq.bits
  evac.io.vcmdq.valid := vcmdq.io.deq.valid

  evac.io.vimm1q.bits := vximm1q.io.deq.bits
  evac.io.vimm1q.valid := vximm1q.io.deq.valid
  
  evac.io.vimm2q.bits := vximm2q.io.deq.bits
  evac.io.vimm2q.valid := vximm2q.io.deq.valid

  evac.io.vcntq.bits := vxcntq.io.deq.bits
  evac.io.vcntq.valid := vxcntq.io.deq.valid


  // vmu
  val memif = new vuMemIF()


  // address queues and counters
  val vvaq_arb = new hArbiter(2)( new io_vvaq() )
  val vvaq = new queue_spec(16)({ new io_vvaq_bundle() })
  val vpfvaq = new queue_spec(16)({ new io_vvaq_bundle() })
  
  val vpaq = new queue_spec(16)({ new io_vpaq_bundle() })
  val vpfpaq = new queue_spec(16)({ new io_vpaq_bundle() })
  val vpaq_arb = new hArbiter(2)({ new io_vpaq() })

  val vvaq_count = new qcnt(16,16)
  val vpaq_count = new qcnt(0,16)

  // tlb signals
  val tlb_vec_req = vvaq.io.deq.valid && vpaq.io.enq.ready
  val tlb_vec_hit = Reg(tlb_vec_req) && Reg(io.vec_tlb_req.ready) && !io.vec_tlb_resp.miss
  val tlb_vec_miss = Reg(tlb_vec_req) && Reg(io.vec_tlb_req.ready) && io.vec_tlb_resp.miss
  val tlb_vecpf_req = vpfvaq.io.deq.valid && vpfpaq.io.enq.ready
  val tlb_vecpf_hit = Reg(tlb_vecpf_req) && Reg(io.vec_pftlb_req.ready) && !io.vec_pftlb_resp.miss
  val tlb_vecpf_miss = Reg(tlb_vecpf_req) && Reg(io.vec_pftlb_req.ready) && io.vec_pftlb_resp.miss

  // vvaq arbiter, port 0: lane vaq
  vvaq_arb.io.in(0) <> vxu.io.lane_vaq
  // vvaq arbiter, port 1: evac
  vvaq_arb.io.in(1) <> evac.io.vaq
  // vvaq arbiter, output
  vvaq_arb.io.out.ready := vvaq_count.io.watermark // vaq.io.enq.ready
  vvaq.io.enq.valid := vvaq_arb.io.out.valid
  vvaq.io.enq.bits := vvaq_arb.io.out.bits

  // vvaq counts available space
  vvaq_count.io.qcnt := vxu.io.qcnt
  // vvaq frees an entry, when vvaq kicks out an entry to vpaq
  vvaq_count.io.inc := tlb_vec_hit && vpaq.io.enq.ready
  // vvaq occupies an entry, when the lane kicks out an entry
  vvaq_count.io.dec := vxu.io.lane_vaq_dec
  
  // vvaq tlb hookup
  io.vec_tlb_req.valid := tlb_vec_req
  io.vec_tlb_req.bits.kill := Bool(false)
  io.vec_tlb_req.bits.cmd := vvaq.io.deq.bits.cmd
  io.vec_tlb_req.bits.vpn := vvaq.io.deq.bits.vpn
  io.vec_tlb_req.bits.asid := Bits(0)
  vvaq.io.deq.ready := vpaq.io.enq.ready
  vvaq.io.ack := tlb_vec_hit && vpaq.io.enq.ready
  vvaq.io.nack := tlb_vec_miss || !vpaq.io.enq.ready

  // tlb vpaq hookup
  // enqueue everything but the page number from virtual queue
  vpaq.io.enq.valid := tlb_vec_hit
  vpaq.io.enq.bits.cmd := Reg(vvaq.io.deq.bits.cmd)
  vpaq.io.enq.bits.typ := Reg(vvaq.io.deq.bits.typ)
  vpaq.io.enq.bits.typ_float := Reg(vvaq.io.deq.bits.typ_float)
  vpaq.io.enq.bits.idx := Reg(vvaq.io.deq.bits.idx)
  vpaq.io.enq.bits.ppn := io.vec_tlb_resp.ppn
 
  // vpfvaq hookup
  vpfvaq.io.enq <> vru.io.vpfvaq

  // vpfvaq tlb hookup
  io.vec_pftlb_req.valid := tlb_vecpf_req
  io.vec_pftlb_req.bits.kill := Bool(false)
  io.vec_pftlb_req.bits.cmd := vpfvaq.io.deq.bits.cmd
  io.vec_pftlb_req.bits.vpn := vpfvaq.io.deq.bits.vpn
  io.vec_pftlb_req.bits.asid := Bits(0) // FIXME
  vpfvaq.io.deq.ready := vpfpaq.io.enq.ready
  vpfvaq.io.ack := tlb_vecpf_hit && vpfpaq.io.enq.ready
  vpfvaq.io.nack := tlb_vecpf_miss || !vpfpaq.io.enq.ready
 
  // tlb vpfpaq hookup
  // enqueue everything but the page number from virtual queue
  vpfpaq.io.enq.valid := tlb_vecpf_hit
  vpfpaq.io.enq.bits.cmd := Reg(vpfvaq.io.deq.bits.cmd)
  vpfpaq.io.enq.bits.typ := Reg(vpfvaq.io.deq.bits.typ)
  vpfpaq.io.enq.bits.typ_float := Reg(vpfvaq.io.deq.bits.typ_float)
  vpfpaq.io.enq.bits.idx := Reg(vpfvaq.io.deq.bits.idx)
  vpfpaq.io.enq.bits.ppn := io.vec_pftlb_resp.ppn

  // vpaq arbiter, port 0: vpaq
  vpaq_arb.io.in(0) <> vpaq.io.deq
  // vpaq arbiter, port1: vpfpaq
  vpaq_arb.io.in(1) <> vpfpaq.io.deq
  // vpaq arbiter, output
  memif.io.vaq_deq <> vpaq_arb.io.out
  // vpaq arbiter, register chosen
  val reg_vpaq_arb_chosen = Reg(vpaq_arb.io.chosen)

  // ack, nacks
  val vpaq_ack = memif.io.vaq_ack && reg_vpaq_arb_chosen === Bits(0)
  val vpaq_nack = memif.io.vaq_nack
  
  val vpfpaq_ack = memif.io.vaq_ack && reg_vpaq_arb_chosen === Bits(1)
  val vpfpaq_nack = memif.io.vaq_nack
  
  vpaq.io.ack := vpaq_ack
  vpaq.io.nack := vpaq_nack

  vpfpaq.io.ack := vpfpaq_ack
  vpfpaq.io.nack := vpfpaq_nack
  
  // vpaq counts occupied space
  vpaq_count.io.qcnt := vxu.io.qcnt
  // vpaq occupies an entry, when it accepts an entry from vvaq
  vpaq_count.io.inc := tlb_vec_hit
  // vpaq frees an entry, when the memory system drains it
  vpaq_count.io.dec := vpaq_ack


  // vector load data queue and counter
  val vldq = new queue_reorder_qcnt(65,128,9) // needs to make sure log2up(vldq_entries)+1 <= CPU_TAG_BITS-1

  vldq.io.deq_data.ready := vxu.io.lane_vldq.ready
  vxu.io.lane_vldq.valid := vldq.io.watermark // vldq.deq_data.valid
  vxu.io.lane_vldq.bits := vldq.io.deq_data.bits

  vldq.io.enq <> memif.io.vldq_enq
  memif.io.vldq_deq_rtag <> vldq.io.deq_rtag

  vldq.io.ack := memif.io.vldq_ack
  vldq.io.nack := memif.io.vldq_nack

  // vldq has an embedded counter
  // vldq counts occupied space
  // vldq occupies an entry, when it accepts an entry from the memory system
  // vldq fress an entry, when the lane consumes it
  vldq.io.qcnt := vxu.io.qcnt


  // vector store data queue and counter
  val vsdq_arb = new hArbiter(2)( new io_vsdq() )
  val vsdq = new queue_spec(16)({ Bits(width = 65) })

  val vsdq_count = new qcnt(16,16)
  val vsack_count = new qcnt(31,31)

  // vsdq arbiter, port 0: lane vsdq
  vsdq_arb.io.in(0).valid := vxu.io.lane_vsdq.valid
  vsdq_arb.io.in(0).bits := vxu.io.lane_vsdq.bits
  vxu.io.lane_vsdq.ready := vsdq_arb.io.in(0).ready
  
  // vsdq arbiter, port 1: evac
  vsdq_arb.io.in(1).valid := evac.io.vsdq.valid
  vsdq_arb.io.in(1).bits := evac.io.vsdq.bits
  evac.io.vsdq.ready := vsdq_arb.io.in(1).ready

  // vsdq arbiter, output
  vsdq_arb.io.out.ready :=
    vsdq_count.io.watermark && vpaq_count.io.watermark && vsack_count.io.watermark // vsdq.io.enq.ready
  vsdq.io.enq.valid := vsdq_arb.io.out.valid
  vsdq.io.enq.bits := vsdq_arb.io.out.bits

  memif.io.vsdq_deq <> vsdq.io.deq

  vsdq.io.ack := memif.io.vsdq_ack
  vsdq.io.nack := memif.io.vsdq_nack

  // vsdq counts available space
  vsdq_count.io.qcnt := vxu.io.qcnt
  // vsdq frees an entry, when the memory system drains it
  vsdq_count.io.inc := memif.io.vsdq_ack
  // vsdq occupies an entry, when the lane kicks out an entry
  vsdq_count.io.dec := vxu.io.lane_vsdq_dec

  // vsack counts available space
  vsack_count.io.qcnt := vxu.io.qcnt
  // vsack frees an entry, when the memory system acks the store
  vsack_count.io.inc := memif.io.vsdq_ack
  // vsack occupies an entry, when the lane kicks out an entry
  vsack_count.io.dec := vxu.io.lane_vsdq.valid && vsdq.io.enq.ready
  // there is no stores in flight, when the counter is full
  vxu.io.pending_store := !vsack_count.io.full


  // memif interface
  io.dmem_req <> memif.io.mem_req
  memif.io.mem_resp <> io.dmem_resp
}
