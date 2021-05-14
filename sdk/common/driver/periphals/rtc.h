///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/14/2021
//
// ================== Description ==================
//
// Defining common routines for RTC
//
///////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef __RTC_H__
#define __RTC_H__

#define RTC_CFG             0x0040
#define RTC_LO              0x0048
#define RTC_HI              0x004C
#define RTC_CS              0x0050
#define RTC_CMP             0x0060


#define _rtc_set_cmp(base, value) \
    IOWR(base, RTC_CMP, value)

#define _rtc_set_cfg(base, value) \
    IOWR(base, RTC_CFG, value)

#define _rtc_clr_lo(base) \
    IOWR(base, RTC_LO, 0x0);

#define _rtc_clr_hi(base) \
    IOWR(base, RTC_HI, 0x0);

#endif