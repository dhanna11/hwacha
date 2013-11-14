package hwacha

import Chisel._
import Node._
import Constants._

class vuVXU_Pointer extends Module
{
  val io = new Bundle
  {
    val ptr = UInt(INPUT, SZ_BPTR)
    val incr = UInt(INPUT, SZ_BPTR1)
    val bcnt = UInt(INPUT, SZ_BCNT)
    val nptr = UInt(OUTPUT, SZ_BPTR)
  }

  val add = io.ptr + io.incr

  io.nptr := MuxLookup(
    Cat(add, io.bcnt), UInt(0, SZ_BPTR), Array(
      Cat(UInt(0,5),UInt(3,4)) -> UInt(0,3),
      Cat(UInt(0,5),UInt(4,4)) -> UInt(0,3),
      Cat(UInt(0,5),UInt(5,4)) -> UInt(0,3),
      Cat(UInt(0,5),UInt(6,4)) -> UInt(0,3),
      Cat(UInt(0,5),UInt(7,4)) -> UInt(0,3),
      Cat(UInt(0,5),UInt(8,4)) -> UInt(0,3),
      Cat(UInt(1,5),UInt(3,4)) -> UInt(1,3),
      Cat(UInt(1,5),UInt(4,4)) -> UInt(1,3),
      Cat(UInt(1,5),UInt(5,4)) -> UInt(1,3),
      Cat(UInt(1,5),UInt(6,4)) -> UInt(1,3),
      Cat(UInt(1,5),UInt(7,4)) -> UInt(1,3),
      Cat(UInt(1,5),UInt(8,4)) -> UInt(1,3),
      Cat(UInt(2,5),UInt(3,4)) -> UInt(2,3),
      Cat(UInt(2,5),UInt(4,4)) -> UInt(2,3),
      Cat(UInt(2,5),UInt(5,4)) -> UInt(2,3),
      Cat(UInt(2,5),UInt(6,4)) -> UInt(2,3),
      Cat(UInt(2,5),UInt(7,4)) -> UInt(2,3),
      Cat(UInt(2,5),UInt(8,4)) -> UInt(2,3),
      Cat(UInt(3,5),UInt(3,4)) -> UInt(0,3),
      Cat(UInt(3,5),UInt(4,4)) -> UInt(3,3),
      Cat(UInt(3,5),UInt(5,4)) -> UInt(3,3),
      Cat(UInt(3,5),UInt(6,4)) -> UInt(3,3),
      Cat(UInt(3,5),UInt(7,4)) -> UInt(3,3),
      Cat(UInt(3,5),UInt(8,4)) -> UInt(3,3),
      Cat(UInt(4,5),UInt(3,4)) -> UInt(1,3),
      Cat(UInt(4,5),UInt(4,4)) -> UInt(0,3),
      Cat(UInt(4,5),UInt(5,4)) -> UInt(4,3),
      Cat(UInt(4,5),UInt(6,4)) -> UInt(4,3),
      Cat(UInt(4,5),UInt(7,4)) -> UInt(4,3),
      Cat(UInt(4,5),UInt(8,4)) -> UInt(4,3),
      Cat(UInt(5,5),UInt(3,4)) -> UInt(2,3),
      Cat(UInt(5,5),UInt(4,4)) -> UInt(1,3),
      Cat(UInt(5,5),UInt(5,4)) -> UInt(0,3),
      Cat(UInt(5,5),UInt(6,4)) -> UInt(5,3),
      Cat(UInt(5,5),UInt(7,4)) -> UInt(5,3),
      Cat(UInt(5,5),UInt(8,4)) -> UInt(5,3),
      Cat(UInt(6,5),UInt(3,4)) -> UInt(0,3),
      Cat(UInt(6,5),UInt(4,4)) -> UInt(2,3),
      Cat(UInt(6,5),UInt(5,4)) -> UInt(1,3),
      Cat(UInt(6,5),UInt(6,4)) -> UInt(0,3),
      Cat(UInt(6,5),UInt(7,4)) -> UInt(6,3),
      Cat(UInt(6,5),UInt(8,4)) -> UInt(6,3),
      Cat(UInt(7,5),UInt(3,4)) -> UInt(1,3),
      Cat(UInt(7,5),UInt(4,4)) -> UInt(3,3),
      Cat(UInt(7,5),UInt(5,4)) -> UInt(2,3),
      Cat(UInt(7,5),UInt(6,4)) -> UInt(1,3),
      Cat(UInt(7,5),UInt(7,4)) -> UInt(0,3),
      Cat(UInt(7,5),UInt(8,4)) -> UInt(7,3),
      Cat(UInt(8,5),UInt(3,4)) -> UInt(2,3),
      Cat(UInt(8,5),UInt(4,4)) -> UInt(0,3),
      Cat(UInt(8,5),UInt(5,4)) -> UInt(3,3),
      Cat(UInt(8,5),UInt(6,4)) -> UInt(2,3),
      Cat(UInt(8,5),UInt(7,4)) -> UInt(1,3),
      Cat(UInt(8,5),UInt(8,4)) -> UInt(0,3),
      Cat(UInt(9,5),UInt(3,4)) -> UInt(0,3),
      Cat(UInt(9,5),UInt(4,4)) -> UInt(1,3),
      Cat(UInt(9,5),UInt(5,4)) -> UInt(4,3),
      Cat(UInt(9,5),UInt(6,4)) -> UInt(3,3),
      Cat(UInt(9,5),UInt(7,4)) -> UInt(2,3),
      Cat(UInt(9,5),UInt(8,4)) -> UInt(1,3),
      Cat(UInt(10,5),UInt(3,4)) -> UInt(1,3),
      Cat(UInt(10,5),UInt(4,4)) -> UInt(2,3),
      Cat(UInt(10,5),UInt(5,4)) -> UInt(0,3),
      Cat(UInt(10,5),UInt(6,4)) -> UInt(4,3),
      Cat(UInt(10,5),UInt(7,4)) -> UInt(3,3),
      Cat(UInt(10,5),UInt(8,4)) -> UInt(2,3),
      Cat(UInt(11,5),UInt(3,4)) -> UInt(2,3),
      Cat(UInt(11,5),UInt(4,4)) -> UInt(3,3),
      Cat(UInt(11,5),UInt(5,4)) -> UInt(1,3),
      Cat(UInt(11,5),UInt(6,4)) -> UInt(5,3),
      Cat(UInt(11,5),UInt(7,4)) -> UInt(4,3),
      Cat(UInt(11,5),UInt(8,4)) -> UInt(3,3),
      Cat(UInt(12,5),UInt(3,4)) -> UInt(0,3),
      Cat(UInt(12,5),UInt(4,4)) -> UInt(0,3),
      Cat(UInt(12,5),UInt(5,4)) -> UInt(2,3),
      Cat(UInt(12,5),UInt(6,4)) -> UInt(0,3),
      Cat(UInt(12,5),UInt(7,4)) -> UInt(5,3),
      Cat(UInt(12,5),UInt(8,4)) -> UInt(4,3),
      Cat(UInt(13,5),UInt(3,4)) -> UInt(1,3),
      Cat(UInt(13,5),UInt(4,4)) -> UInt(1,3),
      Cat(UInt(13,5),UInt(5,4)) -> UInt(3,3),
      Cat(UInt(13,5),UInt(6,4)) -> UInt(1,3),
      Cat(UInt(13,5),UInt(7,4)) -> UInt(6,3),
      Cat(UInt(13,5),UInt(8,4)) -> UInt(5,3),
      Cat(UInt(14,5),UInt(3,4)) -> UInt(2,3),
      Cat(UInt(14,5),UInt(4,4)) -> UInt(2,3),
      Cat(UInt(14,5),UInt(5,4)) -> UInt(4,3),
      Cat(UInt(14,5),UInt(6,4)) -> UInt(2,3),
      Cat(UInt(14,5),UInt(7,4)) -> UInt(0,3),
      Cat(UInt(14,5),UInt(8,4)) -> UInt(6,3),
      Cat(UInt(15,5),UInt(3,4)) -> UInt(0,3),
      Cat(UInt(15,5),UInt(4,4)) -> UInt(3,3),
      Cat(UInt(15,5),UInt(5,4)) -> UInt(0,3),
      Cat(UInt(15,5),UInt(6,4)) -> UInt(3,3),
      Cat(UInt(15,5),UInt(7,4)) -> UInt(1,3),
      Cat(UInt(15,5),UInt(8,4)) -> UInt(7,3),
      Cat(UInt(16,5),UInt(3,4)) -> UInt(1,3),
      Cat(UInt(16,5),UInt(4,4)) -> UInt(0,3),
      Cat(UInt(16,5),UInt(5,4)) -> UInt(1,3),
      Cat(UInt(16,5),UInt(6,4)) -> UInt(4,3),
      Cat(UInt(16,5),UInt(7,4)) -> UInt(2,3),
      Cat(UInt(16,5),UInt(8,4)) -> UInt(0,3),
      Cat(UInt(17,5),UInt(3,4)) -> UInt(2,3),
      Cat(UInt(17,5),UInt(4,4)) -> UInt(1,3),
      Cat(UInt(17,5),UInt(5,4)) -> UInt(2,3),
      Cat(UInt(17,5),UInt(6,4)) -> UInt(5,3),
      Cat(UInt(17,5),UInt(7,4)) -> UInt(3,3),
      Cat(UInt(17,5),UInt(8,4)) -> UInt(1,3)
      //Cat(UInt(18,5),UInt(3,4)) -> UInt(0,3),
      //Cat(UInt(18,5),UInt(4,4)) -> UInt(2,3),
      //Cat(UInt(18,5),UInt(5,4)) -> UInt(3,3),
      //Cat(UInt(18,5),UInt(6,4)) -> UInt(0,3),
      //Cat(UInt(18,5),UInt(7,4)) -> UInt(4,3),
      //Cat(UInt(18,5),UInt(8,4)) -> UInt(2,3),
      //Cat(UInt(19,5),UInt(3,4)) -> UInt(1,3),
      //Cat(UInt(19,5),UInt(4,4)) -> UInt(3,3),
      //Cat(UInt(19,5),UInt(5,4)) -> UInt(4,3),
      //Cat(UInt(19,5),UInt(6,4)) -> UInt(1,3),
      //Cat(UInt(19,5),UInt(7,4)) -> UInt(5,3),
      //Cat(UInt(19,5),UInt(8,4)) -> UInt(3,3),
      //Cat(UInt(20,5),UInt(3,4)) -> UInt(2,3),
      //Cat(UInt(20,5),UInt(4,4)) -> UInt(0,3),
      //Cat(UInt(20,5),UInt(5,4)) -> UInt(0,3),
      //Cat(UInt(20,5),UInt(6,4)) -> UInt(2,3),
      //Cat(UInt(20,5),UInt(7,4)) -> UInt(6,3),
      //Cat(UInt(20,5),UInt(8,4)) -> UInt(4,3),
      //Cat(UInt(21,5),UInt(3,4)) -> UInt(0,3),
      //Cat(UInt(21,5),UInt(4,4)) -> UInt(1,3),
      //Cat(UInt(21,5),UInt(5,4)) -> UInt(1,3),
      //Cat(UInt(21,5),UInt(6,4)) -> UInt(3,3),
      //Cat(UInt(21,5),UInt(7,4)) -> UInt(0,3),
      //Cat(UInt(21,5),UInt(8,4)) -> UInt(5,3)
    ))
}