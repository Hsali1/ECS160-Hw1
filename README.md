
Issues to fix:
```
    Duplicate posts being created in db
```
Things to work on:
```
    Do the statistics portion of the hw
```

## Running the code

Navigate to root

Install all dependencies:
```
mvn clean install
```

compile the code and create .jar file:
```
mvn clean package
```

Run the code:

```
java -jar target/HW1-solution-1.0-SNAPSHOT.jar {weighted} [file name]

```
weighted = true | false

The default filename used is src/main/resources/input.json

