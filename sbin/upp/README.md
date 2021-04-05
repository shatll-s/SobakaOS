## UPP

UPP: Uplift Power Play

A tool for parsing, dumping and modifying data in Radeon PowerPlay tables

### Introduction

UPP is able to parse and modify binary data structures of PowerPlay tables
commonly found on certain AMD Radeon GPUs. Drivers on recent AMD GPUs
allow PowerPlay tables to be dynamically modified on runtime, which may be
known as "soft" PowerPlay table. On Linux, the PowerPlay table is by default
found at: `/sys/class/drm/card0/device/pp_table`.

Alternatively, one can use this tool to get PowerPlay data by:

* Extracting PowerPlay table from Video ROM image (see extract command)
* Importing "Soft PowerPlay" table from Windows registry, directly from
  offline Windows/System32/config/SYSTEM file on disk, so it would work
  from Linux distro that has acces to mounted Windows partition
  (path to SYSTEM registry file is specified with `--from-registry` option)

This tool currently supports parsing and modifying PowerPlay tables found
on the following AMD GPU families:

* Polaris
* Vega
* Radeon VII
* Navi 10
* Navi 14
* Navi 21 (Sienna Cichlid)
* Navi 22 (Navy Flounder)

Note: iGPUs found in many recent AMD APUs are using completely different
PowerPlay control methods, this tool does not support them.

**WARNING**: Authors of this tool are in no way responsible for any damage
that may happen to your expansive graphics card if you choose to modify
card voltages, power limits, or any other PowerPlay parameters. Always
remember that you are doing it entierly on your own risk!

If you have bugs to report or features to request please create an issue on:
https://github.com/sibradzic/upp

### Requirements

Python 3.6+, click library. Optionally, for reading "soft" PowerPlay table
from Windows registry: python-registry. Should work on Windows as well
(testers wanted).

### Usage

At its current form this is a CLI only tool. Getting help:

    upp --help

or

    upp <command> --help

Upp will only work by specifying a command which tells it what to do to one's
Radeon PowerPlay table data. Currently available commands are:

* **dump** - Dumps all PowerPlay data to console
* **extract** - Extracts PowerPlay data from full VBIOS ROM image
* **inject** - Injects PowerPlay data from file into VBIOS ROM image
* **get** - Retrieves current value of one or multiple PowerPlay parametter(s)
* **set** - Sets value to one or multiple PowerPlay parameters
* **version** - Shows UPP version

So, an usage pattern would be like this:

    upp [OPTIONS] COMMAND [ARGS]...

Some generic options applicable to all commands may be used, but please note
that they have to be specified *before* an actual command:

    -p, --pp-file <filename>        Input/output PP table file.
    -f, --from-registry <filename>  Import PP_PhmSoftPowerPlayTable from Windows
    -d, --debug / --no-debug        Debug mode.
    -h, --help                      Show help.

#### Dumping all data:

The **dump** command de-serializes PowerPlay binary data into a human-readable
text output. For example:

    upp dump

In standard mode all data will be dumped to console, where data tree hierarchy
is indicated by indentation. In raw mode a table showing all hex and binary
data, as well as variable names and values, will be dumped.

#### Extracting PowerPlay table from Video ROM image:

Use **extract** command for this. The source video ROM binary must be specified
with `-r/--video-rom` parameter, and extracted PowerPlay table will be saved
into file specified with generic `-p/--pp-file` option. For example:

    upp --pp-file=extracted.pp_table extract -r VIDEO.rom

Default output file name will be an original ROM file name with an
additional .pp_table extension.

#### Injecting PowerPlay data from file into VBIOS ROM image:

Use **inject** command for this. The input video ROM binary must be specified
with `-i/--input-rom` parameter, and the output ROM can be specified with an
optional `-o/--output-rom parameter`. For example:

    upp -p modded.pp_table inject -i original.rom -o modded.rom

**WARNING**: Modified vROM image is probalby not going to work if flashed as is
to your card, due to ROM signature checks on recent Radeon cards. Authors of
this tool are in no way responsible for any damage that may happen to your
expansive graphics card if you choose to flash the modified video ROM, you are
doing it entierly on your own risk.

#### Getting PowerPlay table parameter value(s):

The **get** command retrieves current value of one or multiple PowerPlay table
parameter value(s). The parameter variable path must be specified in `/<param>`
notation, for example:

    upp get smc_pptable/FreqTableGfx/1 smc_pptable/FreqTableGfx/2
    1850
    1400

The order of the output values will match the order of the parameter variable
paths specified.

#### Setting PowerPlay table parameter value(s):

The **set** command sets value to one or multiple PowerPlay table
parameter(s). The parameter path and value must be specified in
`/<param>=<value>` notation, for example:

    upp -p /tmp/custom-pp_table set --write  \
      smc_pptable/SocketPowerLimitAc/0=100   \
      smc_pptable/SocketPowerLimitDc/0=100   \
      smc_pptable/FanStartTemp=100           \
      smc_pptable/FreqTableGfx/1=1550

Note the `--write` parameter, which has to be specified to actually commit
changes to the PowerPlay table file.

#### Getting upp version

    upp version

#### Running as sudo

Note that if you need to run upp deployed with **pip** in `--user` mode with
sudo, you'll need to add some parameters to sudo command to make user env
available to super-user. For example:

    sudo -E env "PATH=$PATH" upp --help

