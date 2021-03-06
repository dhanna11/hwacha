package hwacha

import Chisel._
import freechips.rocketchip.config._

class TLBRequest(implicit p: Parameters) extends VMUBundle()(p) {
    val addr = UInt(width = bVAddrExtended)
    val store = Bool()
    val mt = new DecodedMemType
    val status = new freechips.rocketchip.rocket.MStatus()
}

class TLBIO(implicit p: Parameters) extends VMUBundle()(p) {
  val req = Decoupled(new TLBRequest)
  val resp = new Bundle {
    val ppn = UInt(INPUT, bPPN)
    val xcpt = Bool(INPUT)
  }

  def pgidx(dummy: Int = 0): UInt = this.req.bits.addr(bPgIdx-1, 0)
  def vpn(dummy: Int = 0): UInt = this.req.bits.addr(bVAddrExtended-1, bPgIdx)
  def paddr(dummy: Int = 0): UInt = Cat(this.resp.ppn, this.pgidx())
}

class RTLBReqWithStatus(implicit p: Parameters) extends VMUBundle()(p) {
  val req = new freechips.rocketchip.rocket.TLBReq(log2Ceil(p(HwachaRegLen)))(p)
  val status = new freechips.rocketchip.rocket.MStatus()
}

class RTLBIO(implicit p: Parameters) extends VMUBundle()(p) {
  val req = Decoupled(new RTLBReqWithStatus)
  val resp = (new freechips.rocketchip.rocket.TLBResp()).flip

  def bridge(client: TLBIO) {
    this.req.bits.req.vaddr := client.req.bits.addr
    this.req.bits.req.passthrough := Bool(false)
    this.req.bits.req.size := client.req.bits.mt.shift()
    this.req.bits.req.cmd := client.req.bits.store // for now just give whether its read or write M_XRD or M_XWR
    this.req.bits.status := client.req.bits.status

    this.req.valid := client.req.valid
    client.req.ready := this.req.ready && !this.resp.miss

    client.resp.ppn := this.resp.paddr(bPAddr-1, bPgIdx)
  }
}

class TBox(n: Int)(implicit p: Parameters) extends VMUModule()(p) {
  val io = new Bundle {
    val inner = Vec(n, new TLBIO()).flip
    val outer = new RTLBIO
    val irq = new IRQIO
  }

  val arb = Wire(new TLBIO())
  io.outer.bridge(arb)

  /* Priority mux */
  arb.req.bits := io.inner.init.foldRight(io.inner.last.req.bits) {
    case (a, b) => Mux(a.req.valid, a.req.bits, b)
  }
  arb.req.valid := io.inner.map(_.req.valid).reduce(_ || _)

  val ready = io.inner.init.map(!_.req.valid).scanLeft(arb.req.ready)(_ && _)
  io.inner.zip(ready).foreach { case (i, r) =>
    i.req.ready := r
    i.resp.ppn <> arb.resp.ppn
    i.resp.xcpt <> arb.resp.xcpt
  }

  /* Misalignment */
  val mt = arb.req.bits.mt
  val ma = Seq(mt.h, mt.w, mt.d).zipWithIndex.map(x =>
    x._1 && (arb.req.bits.addr(x._2, 0) =/= UInt(0))).reduce(_ || _)

  val write = arb.req.bits.store
  val read = !write

  val xcpts = Seq(
    ma && read,
    ma && write,
    io.outer.resp.pf.ld && read,
    io.outer.resp.pf.st && write)
  val irqs = Seq(
    io.irq.vmu.ma_ld,
    io.irq.vmu.ma_st,
    io.irq.vmu.faulted_ld,
    io.irq.vmu.faulted_st)

  val fire = arb.req.fire()
  irqs.zip(xcpts).foreach { case (irq, xcpt) =>
    irq := xcpt && fire
  }
  io.irq.vmu.aux := arb.req.bits.addr
  arb.resp.xcpt := xcpts.reduce(_ || _)
}
