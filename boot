#!/bin/bash
LOG="/dog/log/boot.log"
AMD="amdgpu-pro-18.50-708488-ubuntu-18.04"
NVIDIA="NVIDIA-Linux-x86_64-415.27"
RIG_CFG="/dog/cfg/rig.cfg"
FLASH_CFG="/dog-flash/rig.cfg"
NVIDIASMI_FILE=/tmp/nvidiagpudetect
AMDMEMINFO_FILE=/tmp/amdmeminfo
[ ! -t 1 ] && exec &>> $LOG

cp /dog/service/environment /etc/environment
. /etc/environment
. colors

mkdir -p /run/dog/apiports/ #Create dir for stats & data
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
	#Message about flash
	msg="${YELLOW}"
	msg+="`fix-string '#' 80 '#'`\n"
	msg+="#"`fix-string "      It seems this is flash drive      " 78 "space" "symmetrically"`"#\n"
	msg+="#"`fix-string " May be you need to run command logs-off to extend flash life " 78 "space" "symmetrically"`"#\n"
	msg+=`fix-string "#" 80 "#"`
	msg+="${WHITE}"
	echo -e "$msg" > /dev/tty1
	#echo -e "$msg"
	. $RIG_CFG
	if [[ -z $LOGS ]]; then
		logs-off
		echo "LOGS=\"OFF\"" >> $RIG_CFG
		#sreboot
	fi
else
	echo "It seems this is not flash drive"
fi

netsetup -f
hello -b #get boot parametres
. $RIG_CFG
echo "Service mode: $SERVICE_MODE"
[[ $SERVICE_MODE -ge 1 ]] && systemctl disable mining || systemctl enable mining
if [[ `gpu-detect AMD` -gt 0 && $SERVICE_MODE -le 1 ]]; then
	echo -e "${GREEN}> Including AMD drivers${WHITE}"
	modprobe amdgpu
	sleep 2
	echo "Saving Power Play tables:"
	for ppfile in /sys/class/drm/card*/device/pp_table ; do
		echo -e "\tSaving $ppfile"
		card=$(echo $ppfile | sed 's/.*card\([0-9a-z]*\).*/\1/')
		[[ -z $card ]] && echo "$0: Error matching card number in $ppfile" && continue
		mkdir -p /tmp/pp_tables/card$card
		cp $ppfile /tmp/pp_tables/card$card/pp_table
	done

	#amdmeminfo -q -s -n > $AMDMEMINFO_FILE
fi
amdmeminfo -q -s -n > $AMDMEMINFO_FILE

if [[ `gpu-detect NVIDIA` -gt 0 && $SERVICE_MODE -le 1 ]]; then
	echo -e "${GREEN}> Including NVIDIA drivers${WHITE}"
	modprobe nvidia_drm
fi

if [[ ! -f /dog/log/firstrun.log ]]; then
	tech_info > /dog/log/firstrun.log
fi

#gpu-detect listJS > /run/dog/gpuStats #next hello will use it
/dog/sbin/wd-opendev initial
/dog/sbin/wd-qinheng initial
/dog/sbin/rodos --initial
hello --initial
sleep 0.5
. $RIG_CFG

[[ $WD_KERN -eq 1 ]] && echo "> Starting Kernel Log Watchdog" && systemctl start wd-kern

if [[ ! -z $NOTIFY_ON_BOOT && $NOTIFY_ON_BOOT == 1 ]]; then
	msg "Rig booted" info "`uptime`"
fi

shbox start 

if [[ $USE_GRAPHIC -eq 1 || -z $USE_GRAPHIC ]]; then
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

if [[ $REMOTESSH_USE -eq 1 ]]; then
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
