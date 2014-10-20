#!/bin/bash
# This script prepares a VM for disk compression. Only use this script if you really need to compress your virtual disks. The execution time can be quite long on regular HDDs. 

# This script simply writes out as much zeros as it can to the ~ and /data directories and then removes the files. After the operation is finished, you should turn the VM off and compress/compact the disks.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

# writes zeros to free space
# after this VMware can compress the virtual disks
cat /dev/zero > ~/zeros
rm ~/zeros

cat /dev/zero > /data/zeros
rm /data/zeros

echo "=> Now you can turn off the VM and compress the disks"
