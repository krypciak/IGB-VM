**IGB VM** is an execution enviroment for [**IGB Binary**](https://github.com/krypciak/IGB-Compiler-L1).  

The instruction set is explained [here](https://github.com/krypciak/IGB-Compiler-L1#instruction-explanation).

### Floating points values
Floating point values are supported, but they are limited to only 3 digits.  
This is done by using an integer as if it was shifted by 3 digits to the right:
- `1.234` is stored as `1234`
- `0.001` is stored as `1`  

Since the number is shifted by 3 digits to the right, the maxium value is 2147483 **.** 647, not 2147483647.  
<br/>

There's how the operations are handled with such system:
- `+` works normally; `3.3 + 1.2 == 4.5` `3300 + 1200 == 4500`
- `-` works normally; `3.3 - 1.2 == 2.1` `3300 - 1200 == 2100`
- `*` the first/second number has to be divided by 1000;  
  `3.3 * 1.2 == 3.96` `(3300 * 1200)/1000 == 3960`
- `/` the first number has to be multiplied by 1000, therefore is limited to 2147.483647;  
  `3.3 / 1.2 == 2.75` `3300*1000 / 1200 == 2750`
- '%' works normally; `3.3 % 1.2 == 0.9` `3300 % 1200 == 900`  


### Screen types
There are 2 screen types:
- `rgb` which has 16777216 colors
- `16c` which has 16 colors (see a list of them [here](/src/me/krypek/igb/vm/_16Color.java))


### Pixel cache
To set a pixel, you need to set the pixel cache first.  
It's like picking a color, then drawing.  
This way is much faster then setting the color every time.  

### Take a screenshot using F2  

<br /><br /><br />
## Dependencies:
- [FreeArgParser-Java](https://github.com/krypciak/FreeArgParser-Java)
- [IGB-Compiler-L1](https://github.com/krypciak/IGB-Compiler-L1)
- [IGB-Compiler-L2](https://github.com/krypciak/IGB-Compiler-L2)  
- [Utils](https://github.com/krypciak/Utils)


# License
Licensed under GNU GPLv3 or later
