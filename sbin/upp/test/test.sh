#!/bin/bash

TPU_VBIOS_URL=https://www.techpowerup.com/vgabios
# RX 480 8 GB
ROM_RX480=184327/AMD.RX480.8192.160603.rom
# RX Vega 64 8 GB
ROM_VEGA64=194441/AMD.RXVega64.8176.170719.rom
# Radeon VII 16 GB
ROM_RADEON7=208116/AMD.RadeonVII.16384.190116.rom
# RX 5700 XT 8 GB
ROM_RX5700=212120/AMD.RX5700XT.8192.190616.rom
# RX 6800 16 GB Reference
ROM_RX6800=226802/AMD.RX6800.16384.201007.rom
# RX 6900 16 GB Reference
ROM_RX6900=227070/AMD.RX6900.16384.201104.rom

TEST_ROMS="${ROM_RX480} ${ROM_VEGA64} ${ROM_RADEON7} ${ROM_RX5700} ${ROM_RX6900}"
TEST_ROOT=${PWD}
ROM_DIR=${PWD}/ROMs
TMP_DIR=${PWD}/tmp

[ ! -d ${ROM_DIR} ] && mkdir ${ROM_DIR}
[ ! -d ${TMP_DIR} ] && mkdir ${TMP_DIR}

pushd ../src

for VBIOS in ${TEST_ROMS}; do
  if [ ! -r ${ROM_DIR}/${VBIOS#*/} ]; then
    wget -P ${ROM_DIR} ${TPU_VBIOS_URL}/${VBIOS}
  fi
  python3 -m upp.upp -p ${TMP_DIR}/${VBIOS#*/}.pp_table extract -r ${ROM_DIR}/${VBIOS#*/}
  python3 -m upp.upp -p ${TMP_DIR}/${VBIOS#*/}.pp_table dump > ${TMP_DIR}/${VBIOS#*/}.dump
  python3 -m upp.upp -p ${TMP_DIR}/${VBIOS#*/}.pp_table dump -r > ${TMP_DIR}/${VBIOS#*/}.rawdump
  diff -s ${TEST_ROOT}/${VBIOS#*/}.dump ${TMP_DIR}/${VBIOS#*/}.dump
  if [ $? -ne "0" ]; then
    echo "ERROR in ${TMP_DIR}/${VBIOS#*/}.dump:"
    diff -u ${TEST_ROOT}/${VBIOS#*/}.dump ${TMP_DIR}/${VBIOS#*/}.dump
    exit 2
  fi
  diff -s ${TEST_ROOT}/${VBIOS#*/}.rawdump ${TMP_DIR}/${VBIOS#*/}.rawdump
  if [ $? -ne "0" ]; then
    echo "ERROR in ${TMP_DIR}/${VBIOS#*/}.rawdump:"
    diff -u ${TEST_ROOT}/${VBIOS#*/}.rawdump ${TMP_DIR}/${VBIOS#*/}.rawdump
    exit 2
  fi
done

python3 -m upp.upp -p ${TMP_DIR}/${ROM_RX5700#*/}.pp_table set --write \
  smc_pptable/SocketPowerLimitAc/0=110        \
  smc_pptable/SocketPowerLimitDc/0=110        \
  smc_pptable/FanStartTemp=100                \
  smc_pptable/MinVoltageGfx=2800              \
  smc_pptable/MaxVoltageGfx=3900              \
  smc_pptable/MinVoltageSoc=2800              \
  smc_pptable/MaxVoltageSoc=3800              \
  smc_pptable/qStaticVoltageOffset/0/c=-0.03  \
  smc_pptable/UlvVoltageOffsetSoc=0           \
  smc_pptable/UlvVoltageOffsetGfx=0           \
  smc_pptable/FreqTableGfx/1=1650             \
  smc_pptable/MemMvddVoltage/0=4400           \
  smc_pptable/MemVddciVoltage/0=2600          \
  smc_pptable/MemMvddVoltage/1=4600           \
  smc_pptable/MemVddciVoltage/1=3200          \
  smc_pptable/MemMvddVoltage/2=4800           \
  smc_pptable/MemVddciVoltage/2=3200          \
  smc_pptable/MemMvddVoltage/3=5000           \
  smc_pptable/MemVddciVoltage/3=3200          \
  smc_pptable/FreqTableUclk/3=750

python3 -m upp.upp -p ${TMP_DIR}/${ROM_RX5700#*/}.pp_table get \
  smc_pptable/SocketPowerLimitAc/0      \
  smc_pptable/SocketPowerLimitDc/0      \
  smc_pptable/FanStartTemp              \
  smc_pptable/MinVoltageGfx             \
  smc_pptable/MaxVoltageGfx             \
  smc_pptable/MinVoltageSoc             \
  smc_pptable/MaxVoltageSoc             \
  smc_pptable/qStaticVoltageOffset/0/c  \
  smc_pptable/UlvVoltageOffsetSoc       \
  smc_pptable/UlvVoltageOffsetGfx       \
  smc_pptable/FreqTableGfx/1            \
  smc_pptable/MemMvddVoltage/0          \
  smc_pptable/MemVddciVoltage/0         \
  smc_pptable/MemMvddVoltage/1          \
  smc_pptable/MemVddciVoltage/1         \
  smc_pptable/MemMvddVoltage/2          \
  smc_pptable/MemVddciVoltage/2         \
  smc_pptable/MemMvddVoltage/3          \
  smc_pptable/MemVddciVoltage/3         \
  smc_pptable/FreqTableUclk/3           \
  >  ${TMP_DIR}/${ROM_RX5700#*/}.check

diff -s ${TEST_ROOT}/${ROM_RX5700#*/}.check ${TMP_DIR}/${ROM_RX5700#*/}.check
if [ $? -ne "0" ]; then
  echo "ERROR in ${TMP_DIR}/${ROM_RX5700#*/}.check:"
  diff -u ${TEST_ROOT}/${ROM_RX5700#*/}.check ${TMP_DIR}/${ROM_RX5700#*/}.check
  exit 2
fi
