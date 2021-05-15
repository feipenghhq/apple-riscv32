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
soc: export TOP 		= DE2_top
soc: export DEVICE 	    = $(_DEVICE)
soc: export FAMILY 	    = $(_FAMILY)
soc: clean
	mkdir -p $(PRJ_NAME)
	cd $(PRJ_NAME) && ln -s ../run.tcl
	cd $(PRJ_NAME) && quartus_sh --64bit -t run.tcl | tee run.log

clean:
	rm -rf $(PRJ_NAME)

