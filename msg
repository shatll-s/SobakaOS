#!/bin/bash
RIG_CFG="/dog/cfg/rig.cfg"
. $RIG_CFG
set -o noglob #else there are problems with data put in json

if [[ $# -lt 1 || $# -gt 4 ]]; then
	basename=`basename $0`
	echo "Usage: $basename \"title\" [\"type (warning, danger, success, file)\"] [\"data\"]"
	exit 1
fi
title=$1

if [[ $# -ge 3 ]]; then
	data="$3"

	request=$(
			jq -n \
			--arg password "$PASSWD" \
			--arg title "$title" \
			--arg type "$type" \
			--arg data "$data" \
			'{"method": "message", "jsonrpc": "2.0",
			"stats": {$password, "data": {$title, $data}}}'
		)

	[[ $3 == "payload" ]] && payload=$(cat) #if $3 = payload, read from stdin
	if [[ ! -z $payload ]]; then
		payload_json=`echo "$payload" | jq -R -s '{"stats": {"data": {"payload": .}}}'`
		request=`echo "$request $payload_json" | jq -sc '.[0] * .[1]'`
	fi	
else
	request=$(
			jq -n \
			--arg password "$PASSWD" \
			--arg title "$title" \
			--arg type "$type" \
			--arg data "$data" \
			'{"method": "message", "jsonrpc": "2.0",
			"stats": {$password, "data": {$title}}}'
		)
fi
 
if [[ $# -ge 2 ]]; then
	if [[ $2 == "success" || $2 == "ok" ]]; then
		type="success"
	elif [[ $2 == "danger" || $2 == "error" ]]; then 
		type="danger"
	elif [[ $2 == "warning" ]]; then
		type="warning"
	else
		type=$2
	fi
	type_json=`echo -n "$type" | jq -R -s '{"stats": {"data": {"type": .}}}'`
	request=`echo "$request $type_json" | jq -sc '.[0] * .[1]'`
fi

#echo "$request" | jq '.' #no need, just visualisation

#get server answer
response=$(echo "$request" | curl -s --data @- \
	--connect-timeout 7 --max-time 15 \
	"${HOST}?id=$RIG_ID&method=message"  -H "Content-Type: application/json")

echo -n $(date --rfc-3339=seconds)" Message ($title) sent"
echo " [answer: $response]" #server answer (commands usually):
