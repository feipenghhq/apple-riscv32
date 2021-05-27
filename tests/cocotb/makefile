
.PHONY: dedicated_tests riscv_tests

SOC ?= arty

objects = dedicated_tests riscv_tests

$(objects):
	@rm -rf $@
	@mkdir $@
	@cd $@ && ln -s ../scripts/* .
	@cd $@ && python3 run_all_tests.py -soc $(SOC) -timeout 20000 -test "$@"

clean:
	@rm -rf $(objects)