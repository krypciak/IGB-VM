package me.krypek.igb.vm;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.krypek.igb.cl1.datatypes.IGB_Binary;
import me.krypek.mc.datapackparser.Datapack;
import me.krypek.mc.datapackparser.Datapack.DatapackFunction;

public class MCMPC_Parser {
	public static int PACK_VERSION = 6;
	public static String DESCRIPTION = "";

	private MCMPC_Parser() {}

	public static void parse(IGB_Binary[] bins, String name, String path) {

		final String submodule = "mcmp_" + name;
		StringBuilder sbCallAll = new StringBuilder();
		sbCallAll.append("tellraw @a [\"\",{\"text\":\"Parsing main \",\"bold\":true,\"color\":\"gold\"},{\"text\":\"" + name
				+ "\",\"bold\":true,\"color\":\"green\"},{\"text\":\"...\",\"bold\":true,\"color\":\"gold\"}]\n");
		List<DatapackFunction> functions = Stream.of(bins).map(bin -> {
			StringBuilder sb = new StringBuilder(bin.bin().length * 100);
			final int startline = bin.startline();

			sb.append("tellraw @a [\"\",{\"text\":\"   Parsing \",\"bold\":true,\"color\":\"gold\"},{\"text\":\"" + bin.name()
					+ "\",\"bold\":true,\"color\":\"aqua\"}," + "{\"text\":\" to \",\"bold\":true,\"color\":\"gold\"},{\"text\":\"" + startline
					+ "\",\"bold\":true,\"color\":\"blue\"},{\"text\":\"...\",\"bold\":true,\"color\":\"gold\"}]\n");
			final int pesX = 250;
			final int pesZ = 250;

			int x = startline % pesX;
			final int y = 3;
			int z = startline / pesX;

			for (int[] inst : bin.bin()) {
				sb.append("setblock ");
				sb.append(x);
				sb.append(' ');
				sb.append(y);
				sb.append(' ');
				sb.append(z);
				sb.append(" minecraft:jukebox{RecordItem:{id:\"minecraft:egg\",Count:1b,tag:{a:[");
				for (int i = 0; i < inst.length; i++) {
					sb.append(inst[i]);
					if(i != inst.length - 1)
						sb.append(',');
				}
				sb.append("]}}} destroy\n");
				x++;
				if(x >= pesX) {
					x = 0;
					z++;
					if(z >= pesZ) {
						throw new IGB_VM_Exception("Cannot parse to mc outside of pes Z.");
					}
				}
			}
			String name1 = bin.name();
			if(name1.startsWith("$res "))
				name1 = name1.substring(5);

			sbCallAll.append("function ");
			sbCallAll.append(submodule);
			sbCallAll.append(':');
			sbCallAll.append(name1);
			sbCallAll.append('\n');

			return new DatapackFunction(submodule, name1, sb.toString());
		}).collect(Collectors.toList());
		sbCallAll.append("\nkill @e[type=item]\n");
		functions.add(new DatapackFunction(submodule, "parse", sbCallAll.toString()));

		Datapack datapack = new Datapack("MCMP_" + name, PACK_VERSION, DESCRIPTION, functions.toArray(DatapackFunction[]::new));
		datapack.parse(path);
	}

}
