#!/bin/bash

# studentZip=$1
# unzip $studentZip
# 
# dir_name=$(basename $studentZip .zip)
# 
# cd $dir_name

sudo service redis-server start
redis-cli flushall

echo ">>>>>>>>>>> CHECKING THE APP COMPILES"

mvn clean install

if [ $? -eq 0 ]; then
	echo "Compilation succeeded"
else
	echo "Compilation failed"
	exit -1
fi

# Default input.json, non-weighted
echo ">>>>>>>>>>> CHECKING THE APP RUNS"

output=$(java -jar target/HW1-solution-1.0-SNAPSHOT.jar --weighted false --file "/update/this/to/provide/an/absolute/path/input.json")

if [ $? -eq 0 ]; then
	echo "Execution succeeded"
else
	echo "Execution failed"
	exit -1
fi

# echo "$output"

total_posts=$(echo "$output" | grep "Total posts" | awk -F": " '{print $2}')
avg_replies=$(echo "$output" | grep "Average number of replies" | awk -F": " '{print $2}')
duration=$(echo "$output" | grep "Average duration between replies" | awk -F": " '{print $2}')

# Convert the time duration to total seconds for comparison
IFS=: read -r hours minutes seconds <<< "$duration"
# Default to 0 if any value is empty to avoid arithmetic errors
hours=${hours:-0}
minutes=${minutes:-0}
seconds=${seconds:-0}

# Calculate total seconds
total_seconds=$((hours * 3600 + minutes * 60 + seconds))

# We will manually check the results if the validation fails
# As long as the values look reasonable we will accept the results
echo ">>>>>>>>>> CHECKING WITH FIRST JSON"
echo "***** application ouptut begins *****"
echo "Total posts = " $total_posts
echo "Avg. replies = " $avg_replies
echo "Duration = " $duration
echo "***** application output ends *****"

if [ "$total_posts" -gt 1992 ] && [ "$total_seconds" -lt 6000 ]; then
	echo "Results meet the conditions!"
else
	echo "Results don't meet the conditions. Please review manually."
fi


# Run tests

echo ">>>>>>>>>>> CHECKING JUNIT TESTS "
test_run_output=$(mvn test)

if [[ "$test_run_output" == *"Failures: 0"* && "$test_run_output" == *"Errors: 0"* && "$test_run_output" == *"Skipped: 0"* ]]; then
	echo "All tests passed!"
else
	echo "Some tests failed!"
	echo "$test_run_output"
fi

tests_run=$(echo "$test_run_output" | grep "Tests run: \d+" | grep -v "elapsed" | awk -F": " '{print $2}')
tests_run=$(echo "$tests_run" | tr -d ' \n')

echo "Number of tests run: $tests_run"
if [[ $tests_run -lt 4 ]]; then
	echo "Fewer than 4 tests were run"
else
	echo "More than 4 tests were run"
fi

# Check the number of records entered
num_db_records=$(redis-cli DBSIZE | awk '{print $1}')

echo ">>>>>>>>>>> CHECKING REDIS RECORDS "
if [[ $num_db_records -gt 2000 ]]; then
	echo "Successfully entered records in Redis"
else
	echo "Something went wrong in record insertion. Must check manually."
fi


### Check the number of classes. We will also manually evaluate the design

if [[ $(find . -name "*.class"  | wc | awk '{print $1}' | bc ) -gt 4 ]] ; 
then 
	echo "More than 4 classes created."; 
else
	echo "Fewer than 4 classes created."
fi
