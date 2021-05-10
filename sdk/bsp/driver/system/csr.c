///////////////////////////////////////////////////////////////////////////////////////////////////
//
// Copyright 2021 by Heqing Huang (feipenghhq@gamil.com)
//
// ~~~ Hardware in SpinalHDL ~~~
//
// Author: Heqing Huang
// Date Created: 05/09/2021
//
// ================== Description ==================
//
// Defining common routines for CSR register
//
///////////////////////////////////////////////////////////////////////////////////////////////////


/**
 * clear and enable branch counter
 */
void clr_en_br_cnt(void) {
    asm volatile (
        "csrci mcountinhibit, 24;\
        csrw  mhpmcounter3,   x0;\
        csrw  mhpmcounter4,   x0;\
        csrsi mcountinhibit,  24"
    );
}

/**
 * stop branch counter
 */
void stp_br_cnt(void) {
    asm volatile (
        "csrci mcountinhibit, 24"
    );
}

