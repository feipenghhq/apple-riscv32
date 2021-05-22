#ifndef _COMPLIANCE_MODEL_H
#define _COMPLIANCE_MODEL_H

#define RVMODEL_DATA_SECTION

#define BEGIN_SIGNATURE_PTR    0x80000000
#define END_SIGNATURE_PTR      0x80000004

#define RVMODEL_HALT                                              \
  la t0, begin_signature;                                         \
  la t1, BEGIN_SIGNATURE_PTR;                                     \
  sw t0, 0(t1);                                                   \
  la t0, end_signature;                                           \
  la t1, END_SIGNATURE_PTR;                                       \
  sw t0, 0(t1);                                                   \
  self_loop:  j self_loop;

// The .align 4 ensures that the signature ends at a 16-byte boundary
#define RVMODEL_DATA_BEGIN                                                    \
  .align 4; .global begin_signature; begin_signature:

#define RVMODEL_DATA_END                                                      \
  .align 4; .global end_signature; end_signature:
  RVMODEL_DATA_SECTION


//-----------------------------------------------------------------------
// RV IO Macros (Non functional)
//-----------------------------------------------------------------------

#define RVMODEL_BOOT
#define RVMODEL_IO_WRITE_STR(_SP, _STR)
#define RVMODEL_IO_ASSERT_GPR_EQ(_SP, _R, _I)
#define RVMODEL_IO_ASSERT_SFPR_EQ(_F, _R, _I)
#define RVMODEL_IO_ASSERT_DFPR_EQ(_D, _R, _I)


#define RVMODEL_SET_MSW_INT
#define RVMODEL_CLEAR_MSW_INT


#define RVMODEL_CLEAR_MTIMER_INT
#define RVMODEL_CLEAR_MEXT_INT

#endif // _COMPLIANCE_MODEL_H

