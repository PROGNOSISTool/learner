retVal="0"
while [ $retVal = "0" ] || [ $retVal = "141" ] ; do
  echo $retVal
  nice -n -19 ./socketAdapter -a 192.168.56.102 -l 5000 -p 20000
  retVal=$?
done
echo $retVal
echo "error code returned, stopping loop"
