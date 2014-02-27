package hwacha

import Chisel._
import Node._
import Constants._

class Expander(implicit conf: HwachaConfiguration) extends Module
{
  val io = new Bundle {
    val xcpt = new XCPTIO().flip

    val seqop = new SequencerOpIO().flip
    val laneop = new LaneOpIO

    val expand_to_hazard = new ExpanderToHazardIO
  }

  class BuildExpander[T<:Data](gen: T, n: Int)
  {
    val valid = Vec.fill(n){Reg(init=Bool(false))}
    val last = Vec.fill(n){Reg(init=Bool(false))}
    val bits = Vec.fill(n){Reg(gen)}

    (0 until n).reverse.foreach(i => ({
      val step_en = if (i==n-1) Bool(false) else valid(i+1)
      valid(i) := step_en
      if (i==n-1) {
        bits(i) := bits(i).fromBits(Bits(0))
        last(i) := Bool(false)
      } else {
        when (step_en) {
          bits(i) := bits(i+1)
          last(i) := last(i+1)
        }
      }
    }))

    def ondeck = Pipe(valid(0), bits(0), 0)
  }

  def ren(rinfo: RegInfo) = rinfo.active && !rinfo.zero
  def rblen(b: Bits) = new ReadBankOp().rblen.fromBits(b)

  val rexp = new BuildExpander(new ReadBankOp, conf.shift_buf_read)
  val wexp = new BuildExpander(new WriteBankOp, conf.shift_buf_write)
  val viuexp = new BuildExpander(new VIUOp, 3)
  val vau0exp = new BuildExpander(new VAU0Op, 4)
  val vau1exp = new BuildExpander(new VAU1Op, 5)
  val vau2exp = new BuildExpander(new VAU2Op, 3)
  val vguexp = new BuildExpander(new VGUOp, 3)
  val vluwexp = new BuildExpander(new WriteBankOp, 2)

  // NOTE: oplen delayed 1 cycle in bank.scala
  // NOTE: rblen delayed 2 cycle in bank.scala
  // NOTE: brqen delayed 1 cycle in bank.scala

  when (io.seqop.valid) {

    when (io.seqop.bits.active.viu) {
      rexp.valid(0) := Bool(true)
      rexp.last(0) := io.seqop.bits.last
      rexp.bits(0).cnt := io.seqop.bits.cnt
      rexp.bits(0).addr := io.seqop.bits.reg.vs.id
      rexp.bits(0).ren := ren(io.seqop.bits.reg.vs)
      rexp.bits(0).oplen := Bits("b001")
      rexp.bits(0).rblen := rblen(Bits(0))
      rexp.bits(0).brqen := Bool(false)

      when (io.seqop.bits.fn.viu.rtype()) {
        rexp.valid(1) := Bool(true)
        rexp.last(1) := io.seqop.bits.last
        rexp.bits(1).cnt := io.seqop.bits.cnt
        rexp.bits(1).addr := io.seqop.bits.reg.vt.id
        rexp.bits(1).ren := ren(io.seqop.bits.reg.vt)
        rexp.bits(1).oplen := Bits("b000")
        rexp.bits(1).rblen := rblen(Bits(0))
        rexp.bits(1).brqen := Bool(false)
      }

      when (io.seqop.bits.fn.viu.s2only()) {
        rexp.bits(0).addr := io.seqop.bits.reg.vt.id
        rexp.bits(0).ren := ren(io.seqop.bits.reg.vt)
      }

      val viu_wptr =
        Mux(io.seqop.bits.fn.viu.rtype(), Bits(conf.int_stages+2), Bits(conf.int_stages+1))

      wexp.valid(viu_wptr) := Bool(true)
      wexp.last(viu_wptr) := io.seqop.bits.last
      wexp.bits(viu_wptr).cnt := io.seqop.bits.cnt
      wexp.bits(viu_wptr).addr := io.seqop.bits.reg.vd.id
      wexp.bits(viu_wptr).sel := Bits(4)

      when (io.seqop.bits.fn.viu.rtype()) {
        viuexp.valid(2) := Bool(true)
        viuexp.bits(2).cnt := io.seqop.bits.cnt
        viuexp.bits(2).fn := io.seqop.bits.fn.viu

        when (io.seqop.bits.reg.vs.zero) { viuexp.bits(2).fn.t0 := M0 }
        when (io.seqop.bits.reg.vt.zero) { viuexp.bits(2).fn.t1 := M0 }
      }
      .otherwise {
        viuexp.valid(1) := Bool(true)
        viuexp.bits(1).cnt := io.seqop.bits.cnt
        viuexp.bits(1).fn := io.seqop.bits.fn.viu
        viuexp.bits(1).utidx := io.seqop.bits.utidx
        viuexp.bits(1).imm := io.seqop.bits.imm.imm

        when (io.seqop.bits.reg.vs.zero) { viuexp.bits(1).fn.t0 := M0 }
      }
    }

    when (io.seqop.bits.active.vau0) {
      rexp.valid(0) := Bool(true)
      rexp.last(0) := io.seqop.bits.last
      rexp.bits(0).cnt := io.seqop.bits.cnt
      rexp.bits(0).addr := io.seqop.bits.reg.vs.id
      rexp.bits(0).ren := ren(io.seqop.bits.reg.vs)
      rexp.bits(0).oplen := Bits("b010")
      rexp.bits(0).rblen := rblen(Bits(0))
      rexp.bits(0).brqen := Bool(false)

      rexp.valid(1) := Bool(true)
      rexp.last(1) := io.seqop.bits.last
      rexp.bits(1).cnt := io.seqop.bits.cnt
      rexp.bits(1).addr := io.seqop.bits.reg.vt.id
      rexp.bits(1).ren := ren(io.seqop.bits.reg.vt)
      rexp.bits(1).oplen := Bits("b001")
      rexp.bits(1).rblen := rblen(Bits("b0000_0011"))
      rexp.bits(1).brqen := Bool(false)

      when (io.seqop.bits.reg.vs.zero) { rexp.bits(1).rblen(0) := Bool(false) }
      when (io.seqop.bits.reg.vt.zero) { rexp.bits(1).rblen(1) := Bool(false) }

      val vau0_wptr = Bits(conf.imul_stages+3)

      wexp.valid(vau0_wptr) := Bool(true)
      wexp.last(vau0_wptr) := io.seqop.bits.last
      wexp.bits(vau0_wptr).cnt := io.seqop.bits.cnt
      wexp.bits(vau0_wptr).addr := io.seqop.bits.reg.vd.id
      wexp.bits(vau0_wptr).sel := Bits(0)

      vau0exp.valid(3) := Bool(true)
      vau0exp.bits(3).cnt := io.seqop.bits.cnt
      vau0exp.bits(3).fn := io.seqop.bits.fn.vau0
    }

    when (io.seqop.bits.active.vau1) {
      when (io.seqop.bits.fn.vau1.fma()) {
        rexp.valid(0) := Bool(true)
        rexp.last(0) := io.seqop.bits.last
        rexp.bits(0).cnt := io.seqop.bits.cnt
        rexp.bits(0).addr := io.seqop.bits.reg.vs.id
        rexp.bits(0).ren := ren(io.seqop.bits.reg.vs)
        rexp.bits(0).oplen := Bits("b100")
        rexp.bits(0).rblen := rblen(Bits(0))
        rexp.bits(0).brqen := Bool(false)

        rexp.valid(1) := Bool(true)
        rexp.last(1) := io.seqop.bits.last
        rexp.bits(1).cnt := io.seqop.bits.cnt
        rexp.bits(1).addr := io.seqop.bits.reg.vt.id
        rexp.bits(1).ren := ren(io.seqop.bits.reg.vt)
        rexp.bits(1).oplen := Bits("b010")
        rexp.bits(1).rblen := rblen(Bits(0))
        rexp.bits(1).brqen := Bool(false)

        rexp.valid(2) := Bool(true)
        rexp.last(2) := io.seqop.bits.last
        rexp.bits(2).cnt := io.seqop.bits.cnt
        rexp.bits(2).addr := io.seqop.bits.reg.vr.id
        rexp.bits(2).ren := ren(io.seqop.bits.reg.vr)
        rexp.bits(2).oplen := Bits("b001")
        rexp.bits(2).rblen := rblen(Bits("b0001_1100"))
        rexp.bits(2).brqen := Bool(false)

        when (io.seqop.bits.reg.vs.zero) { rexp.bits(2).rblen(2) := Bool(false) }
        when (io.seqop.bits.reg.vt.zero) { rexp.bits(2).rblen(3) := Bool(false) }
        when (io.seqop.bits.reg.vr.zero) { rexp.bits(2).rblen(4) := Bool(false) }
      }
      .otherwise {
        rexp.valid(0) := Bool(true)
        rexp.last(0) := io.seqop.bits.last
        rexp.bits(0).cnt := io.seqop.bits.cnt
        rexp.bits(0).addr := io.seqop.bits.reg.vs.id
        rexp.bits(0).ren := ren(io.seqop.bits.reg.vs)
        rexp.bits(0).oplen := Bits("b100")
        rexp.bits(0).rblen := rblen(Bits(0))
        rexp.bits(0).brqen := Bool(false)

        rexp.valid(1) := Bool(true)
        rexp.last(1) := io.seqop.bits.last
        rexp.bits(1).cnt := io.seqop.bits.cnt
        rexp.bits(1).addr := io.seqop.bits.reg.vt.id
        rexp.bits(1).ren := ren(io.seqop.bits.reg.vt)
        rexp.bits(1).oplen := Bits("b001")
        rexp.bits(1).rblen := rblen(Bits("b0001_0100"))
        rexp.bits(1).brqen := Bool(false)

        when (io.seqop.bits.reg.vs.zero) { rexp.bits(1).rblen(2) := Bool(false) }
        when (io.seqop.bits.reg.vt.zero) { rexp.bits(1).rblen(4) := Bool(false) }
      }

      val vau1_wptr =
        Mux(io.seqop.bits.fn.vau1.fma(), Bits(conf.fma_stages+4), Bits(conf.fma_stages+3))

      wexp.valid(vau1_wptr) := Bool(true)
      wexp.last(vau1_wptr) := io.seqop.bits.last
      wexp.bits(vau1_wptr).cnt := io.seqop.bits.cnt
      wexp.bits(vau1_wptr).addr := io.seqop.bits.reg.vd.id
      wexp.bits(vau1_wptr).sel := Bits(1)

      when (io.seqop.bits.fn.vau1.fma()) {
        vau1exp.valid(4) := Bool(true)
        vau1exp.bits(4).cnt := io.seqop.bits.cnt
        vau1exp.bits(4).fn := io.seqop.bits.fn.vau1
      }
      .otherwise {
        vau1exp.valid(3) := Bool(true)
        vau1exp.bits(3).cnt := io.seqop.bits.cnt
        vau1exp.bits(3).fn := io.seqop.bits.fn.vau1
      }
    }

    when (io.seqop.bits.active.vau2) {
      rexp.valid(0) := Bool(true)
      rexp.last(0) := io.seqop.bits.last
      rexp.bits(0).cnt := io.seqop.bits.cnt
      rexp.bits(0).addr := io.seqop.bits.reg.vs.id
      rexp.bits(0).ren := ren(io.seqop.bits.reg.vs)
      rexp.bits(0).oplen := Bits("b001")
      rexp.bits(0).rblen := rblen(Bits("b0010_0000"))
      rexp.bits(0).brqen := Bool(false)

      when (io.seqop.bits.reg.vs.zero) { rexp.bits(0).rblen(5) := Bool(false) }

      val vau2_wptr = Bits(conf.fconv_stages+2)

      wexp.valid(vau2_wptr) := Bool(true)
      wexp.last(vau2_wptr) := io.seqop.bits.last
      wexp.bits(vau2_wptr).cnt := io.seqop.bits.cnt
      wexp.bits(vau2_wptr).addr := io.seqop.bits.reg.vd.id
      wexp.bits(vau2_wptr).sel := Bits(2)

      vau2exp.valid(2) := Bool(true)
      vau2exp.bits(2).cnt := io.seqop.bits.cnt
      vau2exp.bits(2).fn := io.seqop.bits.fn.vau2
    }

    when (io.seqop.bits.active.vgu) {
      rexp.valid(0) := Bool(true)
      rexp.last(0) := io.seqop.bits.last
      rexp.bits(0).cnt := io.seqop.bits.cnt

      when (io.seqop.bits.fn.vmu.utmemop()) {
        rexp.bits(0).addr := io.seqop.bits.reg.vs.id
        rexp.bits(0).ren := ren(io.seqop.bits.reg.vs)
        rexp.bits(0).oplen := Bits("b001")
        rexp.bits(0).rblen := rblen(Bits("b0100_0000"))
        rexp.bits(0).brqen := Bool(false)

        when (io.seqop.bits.reg.vs.zero) { rexp.bits(0).rblen(6) := Bool(false) }
      }
      .otherwise {
        rexp.bits(0).ren := Bool(false)
        rexp.bits(0).rblen := rblen(Bits(0))
      }

      vguexp.valid(2) := Bool(true)
      vguexp.bits(2).cnt := io.seqop.bits.cnt
      vguexp.bits(2).fn := io.seqop.bits.fn.vmu
      vguexp.bits(2).base := io.seqop.bits.imm.imm
    }

    when (io.seqop.bits.active.vlu) {
      vluwexp.valid(1) := Bool(true)
      vluwexp.last(1) := io.seqop.bits.last
    }

    when (io.seqop.bits.active.vsu) {
      rexp.valid(0) := Bool(true)
      rexp.last(0) := io.seqop.bits.last
      rexp.bits(0).cnt := io.seqop.bits.cnt
      rexp.bits(0).addr := io.seqop.bits.reg.vt.id
      rexp.bits(0).ren := ren(io.seqop.bits.reg.vt)
      rexp.bits(0).oplen := Bits("b001")
      rexp.bits(0).rblen := rblen(Bits("b1000_0000"))
      rexp.bits(0).brqen := Bool(true)
    }

  }

  io.expand_to_hazard.wen := wexp.valid(0) || vluwexp.valid(0)
  io.expand_to_hazard.rlast := rexp.valid(0) && rexp.last(0)
  io.expand_to_hazard.wlast := wexp.valid(0) && wexp.last(0) || vluwexp.valid(0) && vluwexp.last(0)

  io.laneop.read <> rexp.ondeck
  io.laneop.write <> wexp.ondeck
  io.laneop.viu <> viuexp.ondeck
  io.laneop.vau0 <> vau0exp.ondeck
  io.laneop.vau1 <> vau1exp.ondeck
  io.laneop.vau2 <> vau2exp.ondeck
  io.laneop.vgu <> vguexp.ondeck

  io.xcpt.report.exp.empty :=
    !(List(rexp, wexp, viuexp, vau0exp, vau1exp, vau2exp, vguexp).map(e => e.valid.toBits().orR()).reduce(_||_))
}
