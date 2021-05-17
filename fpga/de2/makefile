############################################################
# makefile for running Quartus
############################################################

help:
	@echo "#############################################"
	@echo "#        Make flow for running Quartus      #"
	@echo "#############################################"
	@echo "# make clean    clean all the run result    #"
	@echo "# make soc      run quartus on soc          #"
	@echo "#############################################"


# Variable
_DEVICE = EP2C35F672C7
_FAMILY = Cyclone II

# run vivado at SOC level
export PRJ_NAME 	= soc
export TOP 			= DE2_top
export DEVICE 	    = $(_DEVICE)
export FAMILY 	    = $(_FAMILY)

PRJ_SOF				= $(PRJ_NAME)/output/$(TOP).sof

all: build sta pgm

build: clean
	mkdir -p $(PRJ_NAME)
	cd $(PRJ_NAME) && ln -s ../scripts/build.tcl
	cd $(PRJ_NAME) && quartus_sh --64bit -t build.tcl | tee build.log

sta:
	cd $(PRJ_NAME) && ln -s ../scripts/sta.tcl
	cd $(PRJ_NAME) && quartus_sta --64bit -t sta.tcl | tee sta.log

pgm:
	quartus_pgm -m jtag -o "p;soc/output/DE2_top.sof"

$(PRJ_SOF): build

clean:
	rm -rf $(PRJ_NAME)

