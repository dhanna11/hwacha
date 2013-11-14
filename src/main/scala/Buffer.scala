package hwacha

import Chisel._
import Node._
import Constants._

class io_buffer(DATA_SIZE: Int, ADDR_SIZE: Int) extends Bundle 
{
  val enq = Decoupled(Bits(width=DATA_SIZE)).flip
  val deq = Decoupled(Bits(width=DATA_SIZE))
  val update = Valid(new io_aiwUpdateReq(DATA_SIZE, ADDR_SIZE)).flip

  val rtag = Bits(OUTPUT, ADDR_SIZE)
}

class Buffer(DATA_SIZE: Int, DEPTH: Int) extends Module
{
  val ADDR_SIZE = log2Up(DEPTH)

  val io = new io_buffer(DATA_SIZE, ADDR_SIZE)

  val read_ptr_next = UInt( width=ADDR_SIZE)
  val write_ptr_next = UInt( width=ADDR_SIZE)
  val full_next = Bool()
  
  val read_ptr = Reg(init=UInt(0, ADDR_SIZE))
  val write_ptr = Reg(init=UInt(0, ADDR_SIZE))
  val full = Reg(init=Bool(false))

  read_ptr := read_ptr_next
  write_ptr := write_ptr_next
  full := full_next

  read_ptr_next := read_ptr
  write_ptr_next := write_ptr
  full_next := full

  val do_enq = io.enq.valid && io.enq.ready
  val do_deq = io.deq.ready && io.deq.valid

  when (do_deq) { read_ptr_next := read_ptr + UInt(1) }

  when (do_enq) 
  { 
    write_ptr_next := write_ptr + UInt(1) 
  }

  when (do_enq && !do_deq && (write_ptr_next === read_ptr))
  {
    full_next := Bool(true)
  }
  .elsewhen (do_deq && full) 
  {
    full_next := Bool(false)
  }
  .otherwise 
  {
    full_next := full
  }

  val empty = !full && (read_ptr === write_ptr)

  val data_next = Vec.fill(DEPTH){Bits(width=DATA_SIZE)}
  val data_array = Vec.fill(DEPTH){Reg(Bits(width=DATA_SIZE))}
  
  data_array := data_next

  data_next := data_array

  when (do_enq) { data_next(write_ptr) := io.enq.bits }
  when (io.update.valid) { data_next(io.update.bits.addr):= io.update.bits.data }

  io.enq.ready := !full
  io.deq.valid := !empty

  io.deq.bits := data_array(read_ptr)

  io.rtag := write_ptr
}