package me.krypek.igb.vm;

import static me.krypek.igb.vm.IGB_VM.getRGBValue;

import java.util.HashMap;

//@f:off
public enum _16Color {
   white(		getRGBValue(255, 255, 255)), 
   yellow(		getRGBValue(255, 216, 61)), 
   orange(		getRGBValue(249, 128, 29)), 
   red(			getRGBValue(176, 46, 38)), 
   magenta(		getRGBValue(198, 79, 189)), 
   purple(		getRGBValue(137, 50, 183)), 
   blue(		getRGBValue(60, 68, 169)), 
   lightBlue(	getRGBValue(58, 179, 218)), 
   lime(		getRGBValue(128, 199, 31)), 
   green(		getRGBValue(93, 124, 21)), 
   brown(		getRGBValue(130, 84, 50)), 
   cyan(		getRGBValue(22, 156, 157)), 
   lightGray(	getRGBValue(156, 157, 151)), 
   pink(		getRGBValue(172, 81, 114)), 
   gray(		getRGBValue(71, 79, 82)), 
   black(		getRGBValue(0, 0, 0));
//@f:on

	private static final HashMap<Integer, _16Color> RGBValueTo16;
	static {
		RGBValueTo16 = new HashMap<>();
		for (_16Color _16c : _16Color.values())
			RGBValueTo16.put(_16c.mcrgb, _16c);
	}

	public static _16Color getFromRGB(int rgb) {
		_16Color _16c = RGBValueTo16.get(rgb);
		assert _16c != null;
		return _16c;
	}

	public final int mcrgb;

	private _16Color(int rgb) { this.mcrgb = rgb; }
}
