retVal="0"
while [ $retVal = "0" ] || [ $retVal = "141" ] ; do
  echo $retVal
  nice -n -19 ./socketAdapter -a 192.168.53.102 -l 6000 -p 20000
  retVal=$?
done
echo $retVal
echo "error code returned, stopping loop"
