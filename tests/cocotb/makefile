#------------------------------------------------
# Makefile
#------------------------------------------------

SOC 	?= arty
TIMEOUT ?= 20000

#------------------------------------------------
# Run RISCV Test
#------------------------------------------------
.PHONY: dedicated_tests riscv_tests

objects = dedicated_tests riscv_tests

all: $(objects)

$(objects): clean
	@rm -rf output/$@
	@mkdir -p output/$@
	@cd output/$@ && ln -s ../../scripts/run_all_tests.py .
	@cd output/$@ && ln -s ../../scripts/get_all_tests.py .
	@cd output/$@ && ln -s ../../scripts/run_one_test.py .
	@cd output/$@ && ln -s ../../scripts/makefile .
	@cd output/$@ && python3 run_all_tests.py -soc $(SOC) -timeout $(TIMEOUT) -test "$@"


#------------------------------------------------
# Run software test
#------------------------------------------------

NAME ?=
DUMP ?=0

software_test_check:
ifeq ($(NAME), )
	$(error NAME not set)
endif

software_test: software_test_check clean
	@rm -rf output/$@
	@mkdir -p output/$@
	@cd output/$@ && ln -s ../../scripts/run_software_tests.py .
	@cd output/$@ && ln -s ../../scripts/run_one_test.py .
	@cd output/$@ && ln -s ../../scripts/makefile .
	@cd output/$@ && python3 run_software_tests.py -soc $(SOC) -timeout $(TIMEOUT) -test $(NAME)

clean:
	@rm -rf output/$(objects)