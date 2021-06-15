############################################################
# makefile for running Quartus
############################################################

# Variable
REPO_ROOT   = $(shell git rev-parse --show-toplevel)
TOP_RTL		= DE2_top.v

export PRJ_NAME 	= de2
export TOP 			= De2_top
export DEVICE 	    = EP2C35F672C7
export FAMILY 	    = Cyclone II

PRJ_SOF				= de2/output/De2_top.sof

all: clean build sta pgm

build: $(TOP_RTL)
	mkdir -p $(PRJ_NAME)
	cd $(PRJ_NAME) && ln -s ../scripts/build.tcl
	cd $(PRJ_NAME) && quartus_sh --64bit -t build.tcl | tee build.log

sta:
	cd $(PRJ_NAME) && ln -s ../scripts/sta.tcl
	cd $(PRJ_NAME) && quartus_sta --64bit -t sta.tcl | tee sta.log

pgm:
	quartus_pgm -m jtag -o "p;$(PRJ_SOF)"

$(TOP_RTL):
	cd $(REPO_ROOT) && sbt "runMain Board.De2_topMain"

clean:
	rm -rf $(PRJ_NAME)

