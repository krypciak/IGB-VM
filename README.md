**IGB VM** is an execution enviroment for [**IGB Binary**](https://github.com/krypciak/IGB-Compiler-L1).  

The instruction set is explained [here](https://github.com/krypciak/IGB-Compiler-L1#instruction-explanation).

### Screen types
There are 2 screen types:
- `rgb` which has 16777216 colors
- `16c` which has 16 colors (see a list of them [here](/src/me/krypek/igb/vm/_16Color.java))


### Pixel cache
To set a pixel, you need to set the pixel cache first.  
It's like picking a color, then drawing.  
This way is much faster then setting the color every time.  

### Floating points values

