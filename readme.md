#UNIT TAB#
---------

The UnitTab is a Universal Unit Converter on the Android Platform, available on [![Get it on Google Play](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](https://play.google.com/store/apps/details?id=dh.sunicon)

I made this app to sharpen my programming skills, not for money. So no ads.. The app is free. Try and compare with paid ones. Give a good rate if UnitTab performs better than paid ones...

 - 1200+ units separated into 90+ categories in a very small installation package.
 - Easy to select a unit by typing anything related to it ("km", "length", "distant"..)
 - Currency Converter with Smart (and configurable) live-update rates.
 - Precision output is configurable base on your needs.
 - History of recently used units.
 - Target filtering.

##Text filtering

In order to make a conversion, most of Converter Apps requires to select a category, then select a base unit, a target unit.. Users usually have to scroll and search through some long lists (several of times). UnitTab tries to get rid of this experience and simplify the procedure.

For example: I had been watching the film "247°F" and I wanted to convert from °F to °C. I knew  only that °F is some kind of temperature so I'll type `temp` then space `f`. UnitTab suggest to select the only one result: the `Fahrenheit degree` of the `Temperature` category.. Bingo.

In addition, users often directly pick the unit / currencies on the history list (of 20 most recently used units) without any typing.

If users like a more traditional behavior: click on the small "..." button next to the Unit field, select the "Temperature" category then the "Fahrenheit degree" unit.. If you want to play and discover UnitTab but have no ideas what to type, this button is a good choice ('cause you won't need to type anything)

##Custom output precision

The result values are displayed in one of the 2 following formats:
- Normal format: 4,123.089
- Scientific notation: 4.123089e+3

On the Setting Tab, there are 2 configuration parameters: `Max. Integer` and `Max. Fraction`.

- `Max. Integer = 6` indicates that the integer part of the result in the normal format will not longer than `6`. Otherwise it will be displayed in the Scientific notation format.
For example; if the conversion result = `12,453,287.033`, the integer part has 8 digits (longer than `Max.Integer=6`), it will be display as `1.453287033e+7`

- `Max. Fraction = 9` indicates that the fraction part in both format will not longer than `9`, otherwise it will be around. 

##Currency conversion

In real life, Currency Foreign Exchange is basicly: buy/sell a amount of currency with other. In term of conversion, unitab only give estimate value  base on some Currency Exchange Rates public data source.

Most currency converter use two-steps conversions, for example: to convert 500 EUR to JPY, it firstly converts 500 EUR to USD then convert from USD to JPY. So these apps have to store (and update) only the exchange rates data of USD. If they support for 160 currencies, they will store and update only 160 rates numbers each time.

UnitTab tries to get (and to store offline) the direct exchange rates data EUR-JPY whenever possible in order to make direct-conversion from EUR to JPY. In the reverse case, you want to convert from JPY to EUR, UnitTab will not (even) re-use the previous EUR-JPY rate IF the direct rate JPY-EUR is available. In other words, UnitTab tries to manage 160^2 rates numbers.

But the 160^2 rates numbers are too much to be available or to be retrieved from the Internet. UnitTab implements a sophisticate calculation policy to make currency conversion as accurate as possible depends on data it needed and data it can retrieve (cf. images below). Basically, It makes direct-conversion whenever possible, otherwise inverse-conversions or two-steps conversions.

In addition, Users have choices: they can forced UnitTab to use the traditional calculation method:  two-steps conversion just like other apps: Go to Settings Tab > "L.U Only Usd Rates" > "Yes". For some (minor) currencies, this calculation method may give a better result (but I'm not sure).

The exchange rates data of UnitTab comes from [TheMoneyConverter.com](http://themoneyconverter.com/) (for 90 principals currencies) and from [Yahoo Finance](https://finance.yahoo.com/) (for the others). Hence, the accuracy of UnitTab cannot be more than its data sources. 
Please let me know if there are more reliable FREE data sources.

##TODO

UnitTab is fully functional, but there are features, I would like to have,  but still not have enough time to implement it.

Basicly, I would make UnitTab a generic converter, so that User can add his own Category, Units and Conversion rule, and they are all seamlessly integrate with built-in Category, Units, Conversion rule.

 The database and source code were designed for this fixtures. We has everything ready in code, but there is no UI interface accessible for average users.

##License

https://www.gnu.org/copyleft/gpl.html
