
![](https://github.com/weaselflink/star-cruiser/workflows/CI%20with%20Gradle/badge.svg)

## Star Cruiser Crew Simulator

An experiment with a browser based game.

Client/Server communication is handled via websocket. 
All code ist written in Kotlin (client/server/shared).

### Preconditions

* Java 9+ (e.g. https://www.azul.com/downloads/zulu-community/)

### How to run

    ./gradlew :server:run
    
Then open the URL shown in the logs in a browser

    http://localhost:35667

Spawn a ship and click it, now you should see a radar-like UI.
Try pressing W,A,S,D,P or clicking stuff.

### Thanks to

Initial space ship model created by [niko-3d-models](https://niko-3d-models.itch.io). 
The model is from the [free sci-fi spaceships pack](https://niko-3d-models.itch.io/free-sc-fi-spaceships-pack).

Textures for sky box created with [Spacescape](http://wwwtyro.github.io/space-3d) 
(Source: https://github.com/petrocket/spacescape).
