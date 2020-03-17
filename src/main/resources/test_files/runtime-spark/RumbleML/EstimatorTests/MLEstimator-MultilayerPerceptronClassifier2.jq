(:JIQS: ShouldRun; Output="({ "label" : 0, "name" : "a", "age" : 20, "weight" : 50, "prediction" : 1 }, { "label" : 1, "name" : "b", "age" : 21, "weight" : 55.3, "prediction" : 1 }, { "label" : 2, "name" : "c", "age" : 22, "weight" : 60.6, "prediction" : 1 }, { "label" : 3, "name" : "d", "age" : 23, "weight" : 65.9, "prediction" : 1 }, { "label" : 4, "name" : "e", "age" : 24, "weight" : 70.3, "prediction" : 1 }, { "label" : 5, "name" : "f", "age" : 25, "weight" : 75.6, "prediction" : 1 })" :)
let $data := annotate(
    json-file("./src/main/resources/queries/rumbleML/sample-ml-data-flat.json"),
    { "label": "integer", "binaryLabel": "integer", "name": "string", "age": "double", "weight": "double", "booleanCol": "boolean", "nullCol": "null", "stringCol": "string", "stringArrayCol": ["string"], "intArrayCol": ["integer"],  "doubleArrayCol": ["double"],  "doubleArrayArrayCol": [["double"]] }
)

let $est := get-estimator("MultilayerPerceptronClassifier")
let $tra := $est(
    $data,
    { "labelCol": "binaryLabel", "featuresCol": ["age", "weight"], "layers": [2, 5, 4, 6], "blockSize": 128, "seed": 1234, "maxIter": 3}
)
for $result in $tra(
    $data,
    { "featuresCol": ["age", "weight"] }
)
return {
    "label": $result.label,
    "name": $result.name,
    "age": $result.age,
    "weight": $result.weight,
    "prediction": $result.prediction
}

(:
specify layers for the neural network:
input layer of size 2 (features), two intermediate of size 5 and 4
and output of size 6 (classes)
:)

