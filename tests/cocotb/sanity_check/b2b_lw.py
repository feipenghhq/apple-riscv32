"""
 === Description ===

Test LUI/AUIPC

 === ASM ===

.text
main:
	li  x31, 0x80000000
	ori x1, x0, 1
	ori x2, x0, 2
	ori x3, x0, 3
	ori x4, x0, 4
    sw  x1, 0(x31)
    sw  x2, 4(x31)
    sw  x3, 8(x31)
    sw  x4, 12(x31)
    lw	x5, 0(x31)
    lw	x6, 4(x31)
    lw	x7, 8(x31)
    lw	x8, 12(x31)
end:
	j end

"""

imem_data = [
0x80000fb7,
0x000f8f93,
0x00106093,
0x00206113,
0x00306193,
0x00406213,
0x001fa023,
0x002fa223,
0x003fa423,
0x004fa623,
0x000fa283,
0x004fa303,
0x008fa383,
0x00cfa403,
0x0000006f,
]

expected_register = {
1 : 1,
2 : 2,
3 : 3,
4 : 4,
5 : 1,
6 : 2,
7 : 3,
8 : 4,
}
