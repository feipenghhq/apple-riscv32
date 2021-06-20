///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Module Name: CacheCtrl
//
// Author: Heqing Huang
// Date Created: 06/18/2021
//
// ================== Description ==================
//
// Cache Controller
//
///////////////////////////////////////////////////////////////////////////////////////////////////

package AppleRISCVSoC

import IP._
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.ahblite._

/** Cache Controller */
case class CacheCtrl(ahbLite3Cfg: AhbLite3Config, cacheConfig: CacheConfig,
                     mem_lo: Long, mem_hi: Long) extends Component {

  val io = new Bundle {
    val in_ahb  = slave(AhbLite3Master(ahbLite3Cfg))
    val out_ahb = master(AhbLite3Master(ahbLite3Cfg))
  }

  // Bypass the cache if we are not access main memory
  val bypass = ~(io.in_ahb.HADDR <= mem_hi && io.in_ahb.HADDR >= mem_hi)

  // Instantiate cache
  val cache = Ahblite3Cache(cacheConfig)
  cache.io.cache_ahb <> io.in_ahb.toAhbLite3()
  cache.io.mem_ahb   <> io.out_ahb
  when(bypass) {
    cache.io.cache_ahb.HSEL   := False
    cache.io.cache_ahb.HTRANS := 0
    io.out_ahb <> io.in_ahb
  }
}
