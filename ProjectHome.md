## Welcome ##

Watts is just something I wrote for myself so I could get a visualisation of what my battery was doing and what tasks affect it the most and I've put it on the market simply because maybe others will find it useful too.

| If you dislike the marketplace point your<br>barcode scanner at the QR code to get a<br>download link for this app. <br>
<tr><td> <img src='http://chart.apis.google.com/chart?cht=qr&chs=300x300&chl=http://watts.googlecode.com/svn/trunk/apks/Watts-1.2.5.apk&choe=UTF-8&file=qr.png' /> </td></tr>
<tr><td> <a href='http://code.google.com/p/watts/source/browse/#svn/trunk/apks'>older versions</a>                           </td></tr></tbody></table>

## Help make Watts better ##

Please log bugs by clicking "Issues" above and adding a new one, please include as much information as you can (and if you're a fellow developer the output of a adb logcat).  I can't fix bugs if I don't know about them :)

## Quick Usage Instructions ##

Some basic usage instructions:

  * run the app when you want to start collecting, it will then collect until phone gets rebooted but won't start again unless you start it
  * most recent battery levels are on the left, going further back in time to the right
    * this might change to a configurable option soon as I know it's not to all peoples liking
  * pressing menu allows you to quickly change the zoom level
  * select "finish" from the menu if you want the app to quit running and stop collecting battery data
  * rotating the screen without opening the keyboard will do what you'd think
  * **NOTE:** this app does not poll the battery instead it sits back and waits for OS notification that a battery event has occurred, so this app itself should not impact on your power consumption unduly.

## Screenshots ##

| **Charging** | **Landscape** |
|:-------------|:--------------|
| ![http://watts.googlecode.com/svn/trunk/docs/watts_charging.png](http://watts.googlecode.com/svn/trunk/docs/watts_charging.png) | ![http://watts.googlecode.com/svn/trunk/docs/watts_landscape.png](http://watts.googlecode.com/svn/trunk/docs/watts_landscape.png) |

## Release Notes ##

  * 1.2.5
    * fixed raced condition on first install if app ran before service had a chance to read a battery level
  * 1.2.4
    * dont kill painter thread in onStop, only pause it
  * 1.2.3
    * painter thread leaking fixed
  * 1.2.2
    * another bug in prefs storing fixed
  * 1.2.1
    * bug in prefs storing fixed
  * 1.2.0
    * new graphing engine
    * added text-to-speach support
    * added sound alerts for when battery discharges to 15%, 10% and from 5% and below
  * 1.1.2
    * made UI a bit more pretty using gradients
  * 1.1.1
    * fixed crash if battery charge remains constant for long period of time like for example on standby overnight
  * 1.1.0
    * collector moved to a service, no more relying on UI thread
  * 1.0.5
    * attempted fix to missing intents
  * 1.0.4
    * onDestroy() incorrectly reaped intent receiver
  * 1.0.3
    * found cause of leaked windows, fixed
  * 1.0.2
    * more exceptions caught
  * 1.0.1
    * fixed some missed exceptions made more robust
  * 1.0.0
    * initial release

## Release Philsophy ##

Much like how I vote; Early and Often.