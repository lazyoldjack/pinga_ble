# pinga_ble

This is an ultra simple app that can constantly advertise a mobile phone's local name (typically the telephone number).

The app can be used in conjunction with a battery powered device based on an ESP32 that looks for a particular phone advertising and alerts the user when that advertising is not visible. This is the very simplest way of producing a pocketable device that can alert you to the fact that you have moved out of range of your mobile phone (i.e. left it somewhere or someone has taken it!).

If you use the right kind of ESP32 board, the whole thing can fit into an Oral-B SATINtape floss box which easily goes in a pocket.

As yet, no field tests or power consumption test have been conducted, but it works on the bench.

This project has inspired a *slghtly* more advanced project where the ESP32 and the phone can connect and bond, to facilitate lower power usage for both devices. Watch for bonda_ble...
