# SeaRoute

[SeaRoute](https://github.com/eurostat/searoute) computes shortest maritime routes between pairs of locations.

See below an example from [Marseille (5.3E,43.3N)](https://www.openstreetmap.org/#map=10/43.3/5.3) to [Shanghai (121.8E,31.2N)](https://www.openstreetmap.org/#map=10/31.2/121.8). The red line is the computed maritime route. The black line is the [great-circle route](https://en.wikipedia.org/wiki/Great-circle_distance).

![From Marseille to Shangai](docs/img/mars_shan.png)

**NEW:** It is now possible to compute maritime routes avoiding the Suez and/or Panama channel.

## Usage

### As a program

[SeaRoute](https://github.com/eurostat/searoute) requires Java 1.9 or higher. Run `java --version` to check if Java is installed and what is the current version.

Download the lastest release [here](https://github.com/eurostat/searoute/releases), unzip it and run `java -jar searoute.jar -h` to see the help which describes everything you need to know.

Examples of executions for windows users are provided in `searoute.bat` (for linux users, see `searoute.sh`). `test_input.csv` is an example of input file. It is a simple CSV file with origin/destination coordinates of the routes. Note that only geographical coordinates (decimal degrees) are supported. The output file is a [GeoJSON](https://geojson.org/) (\*.geojson), SHP (\*.shp) or [GeoPackage](http://www.geopackage.org/) (\*.gpkg) file. This file can be displayed on any modern GIS software such as [QGIS](https://qgis.org). GeoJSON files can be displayed easily with [geojson.io](http://geojson.io/).

![Example](docs/img/example.png)

### For coders

[SeaRoute](https://github.com/eurostat/searoute) can be used as a Java library. To quickly setup a development environment, see [these instructions](https://eurostat.github.io/README/howto/java_eclipse_maven_git_quick_guide).

Download and install [SeaRoute](https://github.com/eurostat/searoute) with:

```
git clone https://github.com/eurostat/searoute.git
cd searoute
mvn clean install
```

and then use it in your Java project as a dependency by adding it to the *pom.xml* file:

```
<dependencies>
	...
	<dependency>
		<groupId>eu.europa.ec.eurostat</groupId>
		<artifactId>searoute-core</artifactId>
		<version>2.0</version>
	</dependency>
</dependencies>
```

Here is an example of shortest maritime route computation:

```java
//create the routing object
SeaRouting sr = new SeaRouting();

//get the route between Marseille (5.3E,43.3N) and Shanghai (121.8E,31.2N)
Feature route = sr.getRoute(5.3, 43.3, 121.8, 31.2);

//compute the route distance in km
MultiLineString routeGeom = (MultiLineString) route.getGeometry();
double d = GeoDistanceUtil.getLengthGeoKM(routeGeom);

//extract the route in geoJSON format
String rgj = SeaRoute.toGeoJSON(routeGeom);
```

For further overview, see [the documentation](https://eurostat.github.io/searoute/src/site/apidocs/index.html).

### As a webservice

To deploy [SeaRoute](https://github.com/eurostat/searoute) as a webservice (Java servlet), run:

```
git clone https://github.com/eurostat/searoute.git
cd searoute
mvn clean package
```

and move the servlet `/target/searoute.war` into your `/tomcatX.Y/webapps/` folder. Go then to http://localhost:8080/searoute/ to see the REST-API documentation and some examples.

## Some additional information

The shortest maritime routes are computed from a network of lines covering the seas and following some of the most frequent martitime routes. This maritime network is based on the *Oak Ridge National Labs CTA Transportation Network Group, Global Shipping Lane Network, World, 2000* (retrieved from [geocommons.com](http://geocommons.com/datasets?id=25) or [github](https://github.com/geoiq/gc_data/blob/master/datasets/25.geojson)), enriched with some additional lines around the European coasts based on [AIS data](https://en.wikipedia.org/wiki/Automatic_identification_system). Simplified versions of this network have been produced for different resolutions (5km, 10km, 20km, 50km, 100km) based on a shrinking of too short edges and a removal of similar edges.

[![Maritime network overview](docs/img/marnet_overview_.png)](docs/img/marnet_overview.png)

[SeaRoute](https://github.com/eurostat/searoute) can be reused with custom maritime networks produced from some other custom maritime line datasets. The class *MarnetBuilding* provides some utilities for the creation and preparation of such maritime network datasets, with generalisation methods. To be able to handle *Suez* and *Panama* channels, the custom maritime sections need to be characterised with a new property *desc_* set with the values *suez* and *panama* for the network sections passing by the Suez and Panama channels. The program will then be able to recognise them and possibly avoid them, on user request.

The shortest maritime routes are computed from this network using the [Dijkstra's algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm) implemented in the wonderful [GeoTools](https://geotools.org/) library.

## Support and contribution

Feel free to [ask support](https://github.com/eurostat/searoute/issues/new), fork the project or simply star it (it's always a pleasure). If anyone feels like helping fixing the existing issues, you are welcome !
