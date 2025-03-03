<!--<p align="center">
  <img width="128" align="center" src="...">
</p>-->
<h1 align="center">
  GlobalQuake
</h1>

> [!NOTE]<br>
> GlobalQuake 0.9.0 has just been released!
> Read about the new features in the [releases section](https://github.com/xspanger3770/GlobalQuake/releases)

![GlobalQuake 0.9.0](https://github-production-user-asset-6210df.s3.amazonaws.com/100421968/259861791-6c41b8e4-d33c-44bc-a8ca-4f2ad7ecac40.png)

<div style="display: grid; grid-template-columns: 1fr 1fr;">
<img alt="StationManager" title="StationManager" src="https://github.com/xspanger3770/GlobalQuake/assets/100421968/a37319ec-2132-426a-b095-2e6a9e064322" style="width: 49%; height: auto;" />
<img alt="SourceManager" title="SourceManager" src="https://i.imgur.com/T1tmMtN.png" style="width: 49%; height: auto;" />
</div>

## Description

GlobalQuake is my experimental Java application that can be used to monitor earthquakes world-wide in real time

Enables selection of seismic stations from publicly available seismic networks - `fdsnws services`. \
Real time data is supplied by `seedlink` networks. \
The program then uses the data to detect earthquakes and visualize them on 3D global interactive map \
It can also quickly estimate the earthquake magnitude using some empirical methods, but this only works for small and medium quakes up to magnitude 5-6

Project is in early development state and there is quite some room for improvement in many of the features

### What is GlobalQuake good at

* Easily select publicly available seismic stations you wish to monitor
* Visualise detected earthquakes on 3D global interactive map
* Quickly estimate small and moderate earthquake magnitude, location and depth

### What GlobalQuake struggles with currently

* Larger earthquakes (M6+) that often trigger false detections or show duplicated earthquakes
* Multiple earthquakes from the same place in short time interval
* Distant earthquakes - the further the earthquake is from the stations, the less accurate the calculated epicenter will be

## Running GlobalQuake

* GlobalQuake comes as a standard executable JAR file. You can download the lastest version [here](https://github.com/xspanger3770/GlobalQuake/releases)
* Make sure you have the latest version of Java installed. [Download Java](https://www.oracle.com/java/technologies/downloads/)
* Put the JAR file to the location where you want the application data to be stored
* Run GlobalQuake by executing `java -jar [filename].jar` using your terminal/command line. You can also include `-Xms8G -Xmx8G` parameters to allow more memory allocation

## System Requirements

The system requirements will scale by the number of stations you select. This includes RAM, CPU and network usage\
You can run GlobalQuake on slower system only to monitor earthquakes in your local area, or if your system can handle it, select hundreds or even thousands of stations around the world \
Roughly speaking, 16GB of RAM, 6 CPU cores and 5Mbit network connection should be enough to handle about 2,000 stations (in version 0.9.0)

## Contributing

Any contributions, including feedback and suggestions are highly appreciated! See [Contributing guidelines](https://github.com/xspanger3770/GlobalQuake/blob/main/CONTRIBUTING.md)

## Special thanks to

![JQuake](https://images.weserv.nl/?url=avatars.githubusercontent.com/u/26931126?v=4&h=20&w=20&fit=cover&mask=circle&maxage=7d) [François Le Neindre](https://github.com/fleneindre) ([JQuake](https://jquake.net/en/)) - inspiration for the layout, intensity scale and more
