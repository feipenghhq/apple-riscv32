############################################################
# makefile for running Quartus
############################################################

help:
	@echo "#############################################"
	@echo "#        Make flow for running Quartus      #"
	@echo "#############################################"
	@echo "# make clean    clean all the run result    #"
	@echo "# make soc      run quartus on soc          #"
	@echo "# make core     run quartus on core         #"
	@echo "#############################################"


# Variable
_DEVICE = EP2C35F672C7
_FAMILY = Cyclone II

# run vivado at SOC level
soc: export PRJ_NAME 	= soc
soc: export TOP 		= apple_riscv_soc
soc: export DEVICE 	    = $(_DEVICE)
soc: export FAMILY 	    = $(_FAMILY)
soc: clean_soc
	mkdir -p $(PRJ_NAME)
	cd $(PRJ_NAME) && ln -s ../run.tcl
	cd $(PRJ_NAME) && ln -s ../run.sh
	cd $(PRJ_NAME) && quartus_sh --64bit -t run.tcl

# run vivado at SOC level
core: export PRJ_NAME 	= core
core: export TOP 		= apple_riscv
core: export DEVICE 	= $(_DEVICE)
core: export FAMILY 	= $(_FAMILY)
core: clean_core
	mkdir -p $(PRJ_NAME)
	cd $(PRJ_NAME) && ln -s ../run.tcl
	cd $(PRJ_NAME) && ln -s ../run.sh
	cd $(PRJ_NAME) && quartus_sh --64bit -t run.tcl

clean: clean_soc clean_core

clean_soc:
	rm -rf soc

clean_core:
	rm -rf core