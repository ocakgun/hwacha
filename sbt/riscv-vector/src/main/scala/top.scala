package riscvVector
{
  import Chisel._
  import Node._
  import Fpu._

  /*
  class TopIo extends Bundle {
    val io = Bool('input); // Here temporarily just so this will compile
  }

  class Top extends Component {
    override val io = new TopIo();
    val vuVMU_BHWDsel = new vuVMU_BHWD_sel();
    val vuVMU_Ctrl_ut_issue = new vuVMU_Ctrl_ut_issue();
  }
  */

  object top_main
  {
    def main(args: Array[String]) =
    {
      val boot_args = args ++ Array("--target-dir", "generated-src");
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_fma());
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_imul());
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_conv());
      //chiselMain(boot_args, () => new vuVXU_Banked8_FU_alu());
      //chiselMain(boot_args, () => new vuVXU_Banked8_Bank());
      //chiselMain(boot_args, () => new vuVXU_Banked8_Lane());
      chiselMain(boot_args, () => new vuVXU_Issue_VT());
    } 
  }
} 