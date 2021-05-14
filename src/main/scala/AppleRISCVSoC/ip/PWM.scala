///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: PWM
//
// Author: Heqing Huang
// Date Created: 05/13/2021
//
// ================== Description ==================
//
// PWM
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC.ip

import AppleRISCVSoC.bus._
import spinal.core._
import spinal.lib._

case class PWM(cmpwidth: Int, sibCfg: SibConfig) extends Component {
  require(cmpwidth <= 16)
  val io = new Bundle {
    val pwm_sib     = slave(Sib(sibCfg))
    val pwmcmpip    = out Bits(4 bits)
    val pwmcmpgpio  = out Bits(4 bits)
  }
  noIoPrefix()

  val busCtrl = SibSlaveFactory(io.pwm_sib)

  // ====================================
  // Register
  // ====================================

  // PWM Configuration Register (pwmcfg) - 0x00
  val pwmscale      = busCtrl.createReadAndWrite(UInt(4 bits), 0x00, 0, "PWM Counter scale")
  val pwmsticky     = busCtrl.createReadAndWrite(Bool, 0x00, 8, "PWM Sticky - disallow clearing pwmcmpXip bits")
  val pwmzerocmp    = busCtrl.createReadAndWrite(Bool, 0x00, 9, "PWM Zero - counter resets to zero after match")
  val pwmdeglitch   = busCtrl.createReadAndWrite(Bool, 0x00, 10, "PWM Deglitch - latch pwmcmpXip within same cycle")
  val pwmenalways   = busCtrl.createReadAndWrite(Bool, 0x00, 12, "PWM enable always - run continuously") init False
  val pwmenoneshot  = busCtrl.createReadAndWrite(Bool, 0x00, 13, "PWM enable one shot - run one cycle") init False
  val pwmcmp0center = busCtrl.createReadAndWrite(Bool, 0x00, 16, "PWM0 Compare Center")
  val pwmcmp1center = busCtrl.createReadAndWrite(Bool, 0x00, 17, "PWM0 Compare Center")
  val pwmcmp2center = busCtrl.createReadAndWrite(Bool, 0x00, 18, "PWM0 Compare Center")
  val pwmcmp3center = busCtrl.createReadAndWrite(Bool, 0x00, 19, "PWM0 Compare Center")
  val pwmcmp0gang   = busCtrl.createReadAndWrite(Bool, 0x00, 24, "PWM0/PWM1 Compare Gang")
  val pwmcmp1gang   = busCtrl.createReadAndWrite(Bool, 0x00, 25, "PWM1/PWM2 Compare Gang")
  val pwmcmp2gang   = busCtrl.createReadAndWrite(Bool, 0x00, 26, "PWM2/PWM3 Compare Gang")
  val pwmcmp3gang   = busCtrl.createReadAndWrite(Bool, 0x00, 27, "PWM3/PWM0 Compare Gang")
  val pwmcmp0ip     = busCtrl.createReadAndWrite(Bool, 0x00, 28, "PWM0 Interrupt Pending")
  val pwmcmp1ip     = busCtrl.createReadAndWrite(Bool, 0x00, 29, "PWM1 Interrupt Pending")
  val pwmcmp2ip     = busCtrl.createReadAndWrite(Bool, 0x00, 30, "PWM2 Interrupt Pending")
  val pwmcmp3ip     = busCtrl.createReadAndWrite(Bool, 0x00, 31, "PWM3 Interrupt Pending")

  // PWM counter (pwmcount)
  val pwmcount = busCtrl.createReadAndWrite(UInt(32 bits), 0x08, 0, "PWM counter")

  // PWM scaled counter (pwms)
  val pwms = (pwmcount |>> pwmscale).resize(cmpwidth bits)
  busCtrl.read(pwms, 0x10, 0, "PWM scaled counter")

  // PWM0 Compare Register (pwmcmp0 - pwmcmp3) - 0x20 - 0x2C
  val pwmcmp0 = busCtrl.createReadAndWrite(UInt(cmpwidth bits), 0x20, 0, "PWM 0 Compare Value")
  val pwmcmp1 = busCtrl.createReadAndWrite(UInt(cmpwidth bits), 0x24, 0, "PWM 1 Compare Value")
  val pwmcmp2 = busCtrl.createReadAndWrite(UInt(cmpwidth bits), 0x28, 0, "PWM 2 Compare Value")
  val pwmcmp3 = busCtrl.createReadAndWrite(UInt(cmpwidth bits), 0x2C, 0, "PWM 3 Compare Value")


  // ====================================
  // Logic
  // ====================================

  val pwmcount_carryout = pwmcount(15 + cmpwidth + 1)
  val pwmcmp_match   = Bool
  val pwmcount_clear = pwmcount_carryout | (pwmcmp_match & pwmzerocmp)

  when(pwmcount_clear) {pwmenoneshot := False}

  when(pwmenoneshot || pwmenalways) {
    pwmcount := pwmcount + 1
  }.elsewhen(pwmcount_clear) {
    pwmcount := 0
  }

  val disallow_clear = RegNext(pwmsticky | (pwmdeglitch & ~pwmcount_clear)) init False

  val pwmcmpX       = Vec(pwmcmp0, pwmcmp1, pwmcmp2, pwmcmp3)
  val pwmcmpXip     = Vec(pwmcmp0ip, pwmcmp1ip, pwmcmp2ip, pwmcmp3ip, pwmcmp0ip)
  val pwmcmpXgang   = Vec(pwmcmp0gang, pwmcmp1gang, pwmcmp2gang, pwmcmp3gang)
  val pwmcmpXcenter = Vec(pwmcmp0center, pwmcmp1center, pwmcmp2center, pwmcmp3center)

  def pwm_gen(i: Int) = new Area {
    val center = pwms.msb & pwmcmpXcenter(i)
    val cmpgreater = (center? ~pwms | pwms) >= pwmcmpX(i)
    if (i == 0) {pwmcmp_match := cmpgreater}
    val pwmcmpip = RegInit(False)
    val noncenterip = (pwmcmpXip(i) & disallow_clear) | cmpgreater
    pwmcmpip := center ? False | noncenterip
    io.pwmcmpgpio(i) := ~(pwmcmpXgang(i) & pwmcmpXip(i+1)) & pwmcmpXip(i)
    io.pwmcmpip(i) := pwmcmpXip(i)
  }

  pwm_gen(0)
  pwm_gen(1)
  pwm_gen(2)
  pwm_gen(3)

}

