package me.krypek.igb.vm;

import me.krypek.mc.datapackparser.Datapack;
import me.krypek.mc.datapackparser.Datapack.DatapackFunction;

public class MCMPC_Parser {
	public static int PACK_VERSION = 6;
	public static String DESCRIPTION = "";

	public static void parse(String name, String path, boolean optimize) {

		DatapackFunction[] functions = null;
		Datapack datapack = new Datapack(name, PACK_VERSION, DESCRIPTION, functions);
		datapack.parse(path);
	}

}
