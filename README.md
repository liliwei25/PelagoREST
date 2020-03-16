# PelagoREST

## Setup
1. Run `gradlew build`
2. Run `gradlew bootRun`
3. Local address: http://localhost:8080

## API

### Data Fetching
1. Format: `/fetch[?number=]`
2. Fetches `number` e.g. 50 packages from r-project.
3. Parameter `number` is optional. Default value is 50.
4. Stores fetched data to a local MS SQL server and returns fetched data.

### Package Search
1. Format: `/search?name=`
2. Searches the local MS SQL Server for the specified package data. Returns a empty list if not found.
