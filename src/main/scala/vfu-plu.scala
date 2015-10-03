package hwacha

import Chisel._

class PLUOperand extends VXUBundle {
  val fn = new VIPUFn
  val in0 = Bool()
  val in1 = Bool()
  val in2 = Bool()
}

class PLUResult extends VXUBundle {
  val out = Bool()
}

class PLUSlice extends VXUModule {
  val io = new Bundle {
    val req = Valid(new PLUOperand).flip
    val resp = Valid(new PLUResult)
  }

  val op = io.req.bits.fn.op
  val s2 = Mux(io.req.bits.in2, op(7,4), op(3,0))
  val s1 = Mux(io.req.bits.in1, s2(3,2), s2(1,0))
  val s0 = Mux(io.req.bits.in0, s1(1), s1(0))

  val result = new PLUResult
  result.out := s0

  io.resp := Pipe(io.req.valid, result, stagesPLU)
}
