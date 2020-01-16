#!/bin/bash
LOG="/dog/log/boot.log"
AMD="amdgpu-pro-18.50-708488-ubuntu-18.04"
NVIDIA="NVIDIA-Linux-x86_64-415.27"
RIG_CFG="/dog/cfg/rig.cfg"
FLASH_CFG="/dog-flash/rig.cfg"
NVIDIASMI_FILE=/tmp/nvidiagpudetect
AMDMEMINFO_FILE=/tmp/amdmeminfo

exec &>>$LOG

cp /dog/service/environment /etc/environment
. /etc/environment
export PATH=$PATH
export LC_ALL="en_US.UTF-8"
echo "$(date --rfc-3339=seconds) $0 started."
echo "================================================================"

function tech_info()
{
	local info="#This is some technical information\n"
	info+="hwclock: `sudo hwclock`\n"
	info+="uptime -s: `sudo uptime -s`\n"
	info+="`sudo timedatectl | sed 's/^\s*//g'`\n\n"
	info+="Hostname: `cat /etc/hostname`\n"
	info+="`ifconfig -a`"
	echo -e "$info"
}

time_start=`date +%s`

if [[ ! -f $RIG_CFG ]]; then
	echo -n "There is no config file on rig..."
	if [[ ! -f $FLASH_CFG ]]; then
		echo " and I can\`t find it on flash."
	else
		echo " but I find it on flash."
		dos2unix -n $FLASH_CFG /tmp/rig.cfg
		. /tmp/rig.cfg
		if [[ ! -z $HOST && ! -z $RIG_ID && ! -z $PASSWD ]]; then
			cp /tmp/rig.cfg $RIG_CFG #if don`t do it, next data looks like PASSWD="12345678"MINER=
			echo -e "\n" >> $RIG_CFG
			echo "user:$PASSWD" | chpasswd #Change password
			echo "Config copied"
		else
			echo "There is not enough data in rig.cfg on flash. Check HOST, PASSWD or RIG_ID"
		fi
	fi
fi

disksize=`fdisk -l /dev/sda | grep "Disk /dev/sda:" | sed "s#Disk /dev/sda: \([0-9.]*\) GiB.*#\1#"`
echo "Disk size: $disksize Gb"
if [[ `echo "if ($disksize < 17) print 1" | bc -l` -eq 1 ]]; then
	. colors
	echo -e "######################################################################################\nIt seems this is flash drive\n######################################################################################\n"
	. $RIG_CFG
	if [[ -z $LOGS ]]; then
		logs-off
		echo "LOGS=\"OFF\"" >> $RIG_CFG
		sreboot
	fi
else
	echo "It seems this is not flash drive"
fi

[[ `gpu-detect AMD` -gt 0 ]] && sleep 2 && amdmeminfo -q -s > $AMDMEMINFO_FILE

if [[ ! -f /dog/log/firstrun.log ]]; then
	tech_info > /dog/log/firstrun.log
fi
netsetup -f

/dog/sbin/wd-opendev initial
/dog/sbin/wd-qinheng initial
/dog/sbin/rodos --initial
hello
. $RIG_CFG
if [[ ! -z $NOTIFY_ON_BOOT && $NOTIFY_ON_BOOT == 1 ]]; then
	msg "Rig booted" info "`uptime`"
fi

if [[ $USE_GRAPHIC == 1 || -z $USE_GRAPHIC ]]; then
	if [[ `gpu-detect AMD` -lt 8 ]]; then
		echo "> Starting OSdog Xserver"
		sudo systemctl start dogx
	else
		echo "> Don\`t start OSdog Xserver (there are 8+ AMD GPUs)"
		sudo systemctl start dog-console
	fi
else
	echo "> Don\`t start OSdog Xserver (due to settings)"
	sudo systemctl start dog-console
fi

if [[ $REMOTESSH_USE == 1 ]]; then
	time=0
	while [[ time -lt 15 ]]; do
		[[ ! -z `systemctl status vnc | grep active` ]] && break
		(( time++ ))
		sleep 1
	done
	echo "> Waited $time seconds to get VNC active"
	echo "> Starting RemoteSSH"
	sudo systemctl start remotessh
fi

time_stop=`date +%s`
let "time=time_stop - time_start"
echo "$(date --rfc-3339=seconds) $0 stopped. ($time seconds)"
echo "================================================================"
