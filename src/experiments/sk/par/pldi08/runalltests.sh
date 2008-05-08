export PYTHONPATH="$PYTHONPATH:.."

./runtest.sh queue1 
./runtest.sh queue1_2 
./runtest.sh finelist1 
./runtest.sh finelist2 
./runtest.sh lazylist 
./runtest.sh philo 

# ./runtest.sh barrier
# ./runtest.sh finelist3 
# ./runtest.sh finelist4 
# ./runtest.sh queue2
