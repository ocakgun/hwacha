package hwacha 

import Chisel._
import config._
import tile.FPConstants._
import tile.FType
import HardFloatHelper._

object ScalarFPUDecode {
  val FX: List[BitPat]=
                 List(X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X)
  val FCVT_S_W = List(N,Y,N,N,N,X,X,Y,Y,Y,N,N,N,N,N,Y)
  val FCVT_S_WU= List(N,Y,N,N,N,X,X,Y,Y,Y,N,N,N,N,N,Y)
  val FCVT_S_L = List(N,Y,N,N,N,X,X,Y,Y,Y,N,N,N,N,N,Y)
  val FCVT_S_LU= List(N,Y,N,N,N,X,X,Y,Y,Y,N,N,N,N,N,Y)
  val FCLASS_S = List(N,N,Y,N,N,N,X,Y,Y,N,Y,N,N,N,N,N)
  val FCVT_W_S = List(N,N,Y,N,N,N,X,Y,Y,N,Y,N,N,N,N,Y)
  val FCVT_WU_S= List(N,N,Y,N,N,N,X,Y,Y,N,Y,N,N,N,N,Y)
  val FCVT_L_S = List(N,N,Y,N,N,N,X,Y,Y,N,Y,N,N,N,N,Y)
  val FCVT_LU_S= List(N,N,Y,N,N,N,X,Y,Y,N,Y,N,N,N,N,Y)
  val FEQ_S    = List(N,N,Y,Y,N,N,N,Y,Y,N,Y,N,N,N,N,Y)
  val FLT_S    = List(N,N,Y,Y,N,N,N,Y,Y,N,Y,N,N,N,N,Y)
  val FLE_S    = List(N,N,Y,Y,N,N,N,Y,Y,N,Y,N,N,N,N,Y)
  val FSGNJ_S  = List(N,Y,Y,Y,N,N,N,Y,Y,N,N,Y,N,N,N,N)
  val FSGNJN_S = List(N,Y,Y,Y,N,N,N,Y,Y,N,N,Y,N,N,N,N)
  val FSGNJX_S = List(N,Y,Y,Y,N,N,N,Y,Y,N,N,Y,N,N,N,N)
  val FMIN_S   = List(N,Y,Y,Y,N,N,N,Y,Y,N,N,Y,N,N,N,Y)
  val FMAX_S   = List(N,Y,Y,Y,N,N,N,Y,Y,N,N,Y,N,N,N,Y)
  val FADD_S   = List(N,Y,Y,Y,N,N,Y,Y,Y,N,N,N,Y,N,N,Y)
  val FSUB_S   = List(N,Y,Y,Y,N,N,Y,Y,Y,N,N,N,Y,N,N,Y)
  val FMUL_S   = List(N,Y,Y,Y,N,N,N,Y,Y,N,N,N,Y,N,N,Y)
  val FMADD_S  = List(N,Y,Y,Y,Y,N,N,Y,Y,N,N,N,Y,N,N,Y)
  val FMSUB_S  = List(N,Y,Y,Y,Y,N,N,Y,Y,N,N,N,Y,N,N,Y)
  val FNMADD_S = List(N,Y,Y,Y,Y,N,N,Y,Y,N,N,N,Y,N,N,Y)
  val FNMSUB_S = List(N,Y,Y,Y,Y,N,N,Y,Y,N,N,N,Y,N,N,Y)
  val FDIV_S   = List(N,Y,Y,Y,N,N,N,Y,Y,N,N,N,N,Y,N,Y)
  val FSQRT_S  = List(N,Y,Y,N,N,Y,X,Y,Y,N,N,N,N,N,Y,Y)

  val FCVT_D_W = List(N,Y,N,N,N,X,X,N,N,Y,N,N,N,N,N,Y)
  val FCVT_D_WU= List(N,Y,N,N,N,X,X,N,N,Y,N,N,N,N,N,Y)
  val FCVT_D_L = List(N,Y,N,N,N,X,X,N,N,Y,N,N,N,N,N,Y)
  val FCVT_D_LU= List(N,Y,N,N,N,X,X,N,N,Y,N,N,N,N,N,Y)
  val FCLASS_D = List(N,N,Y,N,N,N,X,N,N,N,Y,N,N,N,N,N)
  val FCVT_W_D = List(N,N,Y,N,N,N,X,N,N,N,Y,N,N,N,N,Y)
  val FCVT_WU_D= List(N,N,Y,N,N,N,X,N,N,N,Y,N,N,N,N,Y)
  val FCVT_L_D = List(N,N,Y,N,N,N,X,N,N,N,Y,N,N,N,N,Y)
  val FCVT_LU_D= List(N,N,Y,N,N,N,X,N,N,N,Y,N,N,N,N,Y)
  val FCVT_S_D = List(N,Y,Y,N,N,N,X,N,Y,N,N,Y,N,N,N,Y)
  val FCVT_D_S = List(N,Y,Y,N,N,N,X,Y,N,N,N,Y,N,N,N,Y)
  val FEQ_D    = List(N,N,Y,Y,N,N,N,N,N,N,Y,N,N,N,N,Y)
  val FLT_D    = List(N,N,Y,Y,N,N,N,N,N,N,Y,N,N,N,N,Y)
  val FLE_D    = List(N,N,Y,Y,N,N,N,N,N,N,Y,N,N,N,N,Y)
  val FSGNJ_D  = List(N,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,N,N)
  val FSGNJN_D = List(N,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,N,N)
  val FSGNJX_D = List(N,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,N,N)
  val FMIN_D   = List(N,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,N,Y)
  val FMAX_D   = List(N,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,N,Y)
  val FADD_D   = List(N,Y,Y,Y,N,N,Y,N,N,N,N,N,Y,N,N,Y)
  val FSUB_D   = List(N,Y,Y,Y,N,N,Y,N,N,N,N,N,Y,N,N,Y)
  val FMUL_D   = List(N,Y,Y,Y,N,N,N,N,N,N,N,N,Y,N,N,Y)
  val FMADD_D  = List(N,Y,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,Y)
  val FMSUB_D  = List(N,Y,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,Y)
  val FNMADD_D = List(N,Y,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,Y)
  val FNMSUB_D = List(N,Y,Y,Y,Y,N,N,N,N,N,N,N,Y,N,N,Y)
  val FDIV_D   = List(N,Y,Y,Y,N,N,N,N,N,N,N,N,N,Y,N,Y)
  val FSQRT_D  = List(N,Y,Y,N,N,Y,X,N,N,N,N,N,N,N,Y,Y)

  val FCVT_S_S = List(N,N,Y,N,N,N,X,Y,Y,N,N,Y,N,N,N,Y) // special op for half conversions

}

class HwachaFPInput(implicit p: Parameters) extends tile.FPInput {
  val bSRegs = log2Up(p(HwachaNScalarRegs))
  val in_fmt = UInt(width = 2)
  val tag = UInt(width = bSRegs)
  override def cloneType = new HwachaFPInput()(p).asInstanceOf[this.type]
}

class HwachaFPResult(implicit p: Parameters) extends tile.FPResult {
  val bSRegs = log2Up(p(HwachaNScalarRegs))
  val tag = UInt(width = bSRegs)
  override def cloneType = new HwachaFPResult()(p).asInstanceOf[this.type]
}

class ScalarFPUInterface(implicit p: Parameters) extends HwachaModule()(p) with Packing with tile.HasFPUParameters {
  val io = new Bundle {
    val hwacha = new Bundle {
      val req = Decoupled(new HwachaFPInput).flip
      val resp = Decoupled(new HwachaFPResult)
    }
    val rocc = new Bundle {
      val req = Decoupled(new tile.FPInput)
      val resp = Decoupled(new tile.FPResult).flip
    }
  }

  val pending_fpu = Reg(init=Bool(false))
  val pending_fpu_req = Reg(new HwachaFPInput)
  val pending_fpu_typ = Reg(Bits(width=2))

  val reqq = Module(new Queue(new HwachaFPInput, 2))
  reqq.suggestName("reqqInst")
  val respq = Module(new Queue(new tile.FPResult, 2))
  respq.suggestName("respqInst")

  reqq.io.enq <> io.hwacha.req

  private val hreq = reqq.io.deq.bits

  private val hreq_ctrl = Wire(new tile.FPUCtrlSigs)
  hreq_ctrl <> hreq
  //We handle half conversions locally
  val enq_rocc = !(hreq_ctrl.getElements.zip(ScalarFPUDecode.FCVT_S_S).map{case(l,r) => r === l.asInstanceOf[UInt]}.reduce(_ && _))
  val mask_rocc_req_ready = !enq_rocc || io.rocc.req.ready
  val mask_respq_enq_ready = enq_rocc || respq.io.enq.ready

  def fire(exclude: Bool, include: Bool*) = {
    val rvs = Seq(!pending_fpu,
      reqq.io.deq.valid, mask_rocc_req_ready, mask_respq_enq_ready)
    (rvs.filter(_ ne exclude) ++ include).reduce(_ && _)
  }

  reqq.io.deq.ready := fire(reqq.io.deq.valid)
  io.rocc.req.valid := fire(mask_rocc_req_ready, enq_rocc)

  when (fire(null)) {
    pending_fpu := Bool(true)
    pending_fpu_req := hreq
    pending_fpu_typ := Mux(hreq.fromint, hreq.in_fmt, hreq.typ)
  }

  val h2s =
    List(hreq.in1, hreq.in2, hreq.in3) map { case in =>
      val h2s = Module(new hardfloat.RecFNToRecFN(5, 11, 8, 24))
      h2s.suggestName("h2sInst")
      h2s.io.in := recode_hp(in)
      h2s.io.roundingMode := hreq.rm
      // XXX: use h2s.io.exceptionFlags
      h2s.io.out
    }

  io.rocc.req.bits <> hreq

  def unboxAndRecode(in: UInt, minT: FType) = {
    unbox(recode(in, hreq.in_fmt), !hreq.singleIn, Some(minT))
  }

  val rec_s_in1 = recode(hreq.in1, hreq.in_fmt)
  io.rocc.req.bits.in1 :=
    Mux(hreq.fromint, hreq.in1,//unboxing is unecessary here
      Mux(hreq.in_fmt === UInt(0), unboxAndRecode(hreq.in1, FType.S),
        Mux(hreq.in_fmt === UInt(1), unboxAndRecode(hreq.in1, FType.D),
          unboxAndRecode(h2s(0), FType.S))))
  io.rocc.req.bits.in2 :=
    Mux(hreq.in_fmt === UInt(0), unboxAndRecode(hreq.in2, FType.S),
      Mux(hreq.in_fmt === UInt(1), unboxAndRecode(hreq.in2, FType.D),
        unboxAndRecode(h2s(1), FType.S)))
  io.rocc.req.bits.in3 :=
    Mux(hreq.in_fmt === UInt(0), unboxAndRecode(hreq.in3, FType.S),
      Mux(hreq.in_fmt === UInt(1), unboxAndRecode(hreq.in3, FType.D),
        unboxAndRecode(h2s(2), FType.S)))

  respq.io.enq.valid := io.rocc.resp.valid || fire(mask_respq_enq_ready, !enq_rocc)
  respq.io.enq.bits := io.rocc.resp.bits
  when (fire(null, !enq_rocc)) {
    respq.io.enq.bits.data := Mux(hreq.in_fmt === UInt(0), rec_s_in1, h2s(0))
  }

  respq.io.deq.ready := io.hwacha.resp.ready
  io.hwacha.resp.valid := respq.io.deq.valid
  io.rocc.resp.ready := respq.io.enq.ready

  when (respq.io.deq.fire()) {
    pending_fpu := Bool(false)
  }

  private val rresp = respq.io.deq.bits
  private val hresp = io.hwacha.resp.bits

  val s2h = Module(new hardfloat.RecFNToRecFN(8, 24, 5, 11))
  s2h.suggestName("s2hInst")
  s2h.io.in := rresp.data
  s2h.io.roundingMode := pending_fpu_req.rm
  // XXX: use s2h.io.exceptionFlags

  val unrec_h = ieee_hp(s2h.io.out)
  val unrec_s = Fill(2, ieee(rresp.data)(31,0))
  val unrec_d = ieee(rresp.data)
  val unrec_fpu_resp =
    Mux(pending_fpu_typ === UInt(0), unrec_s,
      Mux(pending_fpu_typ === UInt(1), unrec_d,
        expand_float_h(unrec_h)))

  hresp.tag := pending_fpu_req.tag
  hresp.data :=
    Mux(pending_fpu_req.toint, rresp.data(63, 0), unrec_fpu_resp)
}
