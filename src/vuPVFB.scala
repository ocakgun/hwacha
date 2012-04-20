package hwacha

import Chisel._
import Node._
import Constants._
import Commands._
import Instructions._

class pvfBundle extends Bundle
{
  val pc = Bits(width=SZ_ADDR)
  val mask = Bits(width=WIDTH_PVFB)
  val id = Bits(width=SZ_NUM_PVFB)
  val vlen = Bits(width=SZ_VLEN)
}

class ioPVFPipe extends ioPipe()( new pvfBundle() )

class ioPVFB extends Bundle
{
  val vtToPVFB = new ioIssueVTToPVFB().flip()
  val mask = new ioMaskPipe().flip()
  val pvf = new ioPVFPipe()
  val empty = Bool(OUTPUT)
}

class ioPVFBCtrl() extends Bundle
{
  val flush = Bool(INPUT)

  val enq_val = Bool(INPUT)

  val resolved_mask = Bits(WIDTH_PVFB, INPUT)
  val active_mask = Bits(WIDTH_PVFB, INPUT)
  val taken_pc = Bits(SZ_ADDR, INPUT)
  val not_taken_pc = Bits(SZ_ADDR, INPUT)

  val deq_rdy = Bool(INPUT)

  val wen = Bool(OUTPUT)
  val waddr = UFix(log2up(DEPTH_PVFB), OUTPUT)
  val mask_wdata = Bits(WIDTH_PVFB, OUTPUT)
  val pc_wdata = Bits(SZ_ADDR, OUTPUT)

  val next_valid = Bool(OUTPUT)
  val next_mask = Bits(WIDTH_PVFB, OUTPUT)
  val next_pc = Bits(SZ_ADDR, OUTPUT)

  val ren = Bool(OUTPUT)
  val raddr = UFix(log2up(DEPTH_PVFB), OUTPUT)

  val empty = Bool(OUTPUT)
}

class vuPVFBCtrl extends Component 
{
  val io = new ioPVFBCtrl()

  val SIZE_ADDR = log2up(DEPTH_PVFB)

  val enq_ptr_next = Wire(){ UFix(width=SIZE_ADDR) }
  val deq_ptr_next = Wire(){ UFix(width=SIZE_ADDR) }
  val full_next = Wire(){ Bool() }

  val enq_ptr = Reg(enq_ptr_next, resetVal = UFix(0, SIZE_ADDR))
  val deq_ptr = Reg(deq_ptr_next, resetVal = UFix(0, SIZE_ADDR))
  val full = Reg(full_next, resetVal = Bool(false))

  enq_ptr_next := enq_ptr
  deq_ptr_next := deq_ptr
  full_next := full

  val empty = ~full && (enq_ptr === deq_ptr)

  val taken_mask = io.resolved_mask & io.active_mask
  val not_taken_mask = ~io.resolved_mask & io.active_mask

  val active_taken = taken_mask.orR
  val active_not_taken = not_taken_mask.orR

  val do_enq = io.enq_val && active_taken && active_not_taken
  val do_deq = io.deq_rdy && !empty

  when (do_deq) { deq_ptr_next := deq_ptr + UFix(1) }
  when (do_enq) { enq_ptr_next := enq_ptr + UFix(1) }

  when (do_enq && ! do_deq && (enq_ptr_next === deq_ptr))
  {
    full_next := Bool(true)
  }
  . elsewhen (do_deq && full)
  {
    full_next := Bool(true)
  } 
  . otherwise
  {
    full_next := full
  }

  io.wen := do_enq
  io.waddr := enq_ptr
  io.mask_wdata := taken_mask
  io.pc_wdata := io.taken_pc

  io.next_valid := io.enq_val && (active_taken || active_not_taken)
  io.next_pc := Mux(active_not_taken, io.not_taken_pc, io.taken_pc)
  io.next_mask := Mux(active_not_taken, not_taken_mask, taken_mask)

  io.ren := do_deq
  io.raddr := deq_ptr

  io.empty := empty
}

class vuPVFB extends Component 
{
  val io = new ioPVFB

  val pvfb_ctrl = new vuPVFBCtrl()

  val reg_taken_pc = Reg(resetVal = Bits(0, SZ_ADDR))
  val reg_not_taken_pc = Reg(resetVal = Bits(0, SZ_ADDR))
  when (io.vtToPVFB.pc.valid) 
  { 
    reg_taken_pc := io.vtToPVFB.pc.bits.taken
    reg_not_taken_pc := io.vtToPVFB.pc.bits.not_taken
  }

  pvfb_ctrl.io.enq_val := io.mask.valid

  pvfb_ctrl.io.resolved_mask := io.mask.bits.resolved
  pvfb_ctrl.io.active_mask := io.mask.bits.active
  pvfb_ctrl.io.taken_pc := reg_taken_pc
  pvfb_ctrl.io.not_taken_pc := reg_not_taken_pc

  pvfb_ctrl.io.deq_rdy := io.vtToPVFB.stop

  val maskRam = Mem(DEPTH_PVFB, 
                    pvfb_ctrl.io.wen,
                    pvfb_ctrl.io.waddr,
                    pvfb_ctrl.io.mask_wdata,
                    resetVal = null,
                    cs = pvfb_ctrl.io.wen || pvfb_ctrl.io.ren)

  val pcRam = Mem(DEPTH_PVFB,
                  pvfb_ctrl.io.wen,
                  pvfb_ctrl.io.waddr,
                  pvfb_ctrl.io.pc_wdata,
                  resetVal = null,
                  cs = pvfb_ctrl.io.wen || pvfb_ctrl.io.ren)

  maskRam.setReadLatency(1)
  maskRam.setTarget('inst)
  pcRam.setReadLatency(1)
  pcRam.setTarget('inst)

  val reg_ren = Reg(pvfb_ctrl.io.ren)

  io.pvf.valid := pvfb_ctrl.io.next_valid || reg_ren
  io.pvf.bits.mask := Mux(reg_ren, maskRam(pvfb_ctrl.io.raddr), pvfb_ctrl.io.next_mask)
  io.pvf.bits.pc := Mux(reg_ren, pcRam(pvfb_ctrl.io.raddr), pvfb_ctrl.io.next_pc)

  io.empty := pvfb_ctrl.io.empty
}
