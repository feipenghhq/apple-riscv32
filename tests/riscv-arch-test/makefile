# -----------------------------------------
# Makefile
# -----------------------------------------

target_path = riscv-arch-test/riscv-target

build: link_target
	cd riscv-arch-test && $(MAKE) RISCV_TARGET=AppleRISCV RISCV_DEVICE=I clean
	cd riscv-arch-test && $(MAKE) RISCV_TARGET=AppleRISCV RISCV_DEVICE=I build
	cd riscv-arch-test && $(MAKE) RISCV_TARGET=AppleRISCV RISCV_DEVICE=M build
	cd riscv-arch-test && $(MAKE) RISCV_TARGET=AppleRISCV RISCV_DEVICE=privilege build

link_target:
	@cd $(target_path) && rm -rf AppleRISCV
	@cd $(target_path) && ln -s ../../AppleRISCV

