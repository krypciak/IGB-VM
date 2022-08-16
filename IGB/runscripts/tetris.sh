#!/bin/sh
java -ea -jar ../vm.jar -cp [ ../L2/tetris.igb_l2 ] -sl 15000 -fps 60 -rs 3000 -ps 20000 -pu -ws ../ -dp /home/krypek/Games/minecraft/instances/mcmulator/.minecraft/saves/MCMulator_v7/datapacks/
echo Press enter to continue 
read ans
