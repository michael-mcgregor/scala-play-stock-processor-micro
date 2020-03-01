# my-play-scala-websocket-example

This is a modification of the open-source play-scala-websocket-example. This example uses `yahoofinance` apis to fetch 
historical and real time data for specified stocks. It's backed by a lightweight mysql table that will persist tracked stocks.
The user can add and remove stocks which will perform crud operations and fetch data from the api.

To run this simply download and execute `sbt run` and navigate to `localhost:9000`. The DB will build itself and automatically populate Google stock data.

## Backend Functionality

The application also supports insight into what's being stored in mysql by using the following api calls.
* `GET` `http://localhost:9000/stocks` - retrieve all rows of stocks being tracked as json
* `GET` `http://localhost:9000/stock` `request body => { "id" = "<key>" }`- retrieve a single row of a stock being tracked as json
