DESCRIPTION = "Linux kernel for the Hardkernel ODROID-XU device"
SECTION = "kernel"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=d7810fab7487fb0aad327b76f1be7cd7"


# Mark archs/machines that this kernel supports
COMPATIBLE_MACHINE = "odroid-xu"

inherit kernel siteinfo

# from where to fetch the kernel
KERNEL_REPO_OWNER ??= "hardkernel"
KERNEL_REPO_URI ??= "git://github.com/${KERNEL_REPO_OWNER}/linux.git"
KBRANCH ?= "odroid-3.12.y"

SRC_URI = " \
  ${KERNEL_REPO_URI};branch=${KBRANCH} \
"

S = "${WORKDIR}/git/"

SRCREV = "${AUTOREV}"

KV = "3.12.4"
PV = "${KV}+gitr${SRCPV}"
LOCALVERSION ?= ""

# stolen from meta-oe's linux.inc
#kernel_conf_variable CMDLINE "\"${CMDLINE} ${CMDLINE_DEBUG}\""
kernel_conf_variable() {
    CONF_SED_SCRIPT="$CONF_SED_SCRIPT /CONFIG_$1[ =]/d;"
    if test "$2" = "n"
    then
        echo "# CONFIG_$1 is not set" >> ${S}/.config
    else
        echo "CONFIG_$1=$2" >> ${S}/.config
    fi
}

do_configure_prepend() {
     yes '' | oe_runmake odroidxu_defconfig
    CONF_SED_SCRIPT=""

    #
    # oabi / eabi support
    #
    kernel_conf_variable AEABI y
    if [ "${ARM_KEEP_OABI}" = "1" ] ; then
        kernel_conf_variable OABI_COMPAT y
    else
        kernel_conf_variable OABI_COMPAT n
    fi

    # When enabling thumb for userspace we also need thumb support in the kernel
    if [ "${ARM_INSTRUCTION_SET}" = "thumb" ] ; then
    kernel_conf_variable ARM_THUMB y
    fi
    kernel_conf_variable CMDLINE "\"${CMDLINE} ${CMDLINE_DEBUG}\""

    kernel_conf_variable LOCALVERSION "\"${LOCALVERSION}\""
    kernel_conf_variable LOCALVERSION_AUTO n

    kernel_conf_variable SYSFS_DEPRECATED n
    kernel_conf_variable SYSFS_DEPRECATED_V2 n
    kernel_conf_variable HOTPLUG y
    kernel_conf_variable UEVENT_HELPER_PATH \"\"
    kernel_conf_variable UNIX y
    kernel_conf_variable SYSFS y
    kernel_conf_variable PROC_FS y
    kernel_conf_variable TMPFS y
    kernel_conf_variable INOTIFY_USER y
    kernel_conf_variable SIGNALFD y
    kernel_conf_variable TMPFS_POSIX_ACL y
    kernel_conf_variable BLK_DEV_BSG y
    kernel_conf_variable DEVTMPFS y
    kernel_conf_variable DEVTMPFS_MOUNT y

    # Newer inits like systemd need cgroup support
    if [ "${KERNEL_ENABLE_CGROUPS}" = "1" ] ; then
        kernel_conf_variable CGROUP_SCHED y
        kernel_conf_variable CGROUPS y
        kernel_conf_variable CGROUP_NS y
        kernel_conf_variable CGROUP_FREEZER y
        kernel_conf_variable CGROUP_DEVICE y
        kernel_conf_variable CPUSETS y
        kernel_conf_variable PROC_PID_CPUSET y
        kernel_conf_variable CGROUP_CPUACCT y
        kernel_conf_variable RESOURCE_COUNTERS y
    fi

    #
    # root-over-nfs-over-usb-eth support. Limited, but should cover some cases.
    # Enable this by setting a proper CMDLINE_NFSROOT_USB.
    #
    if [ ! -z "${CMDLINE_NFSROOT_USB}" ]; then
        bbnote "Configuring the kernel for root-over-nfs-over-usb-eth with CMDLINE ${CMDLINE_NFSROOT_USB}"
        kernel_conf_variable INET y
        kernel_conf_variable IP_PNP y
        kernel_conf_variable USB_GADGET y
        kernel_conf_variable USB_GADGET_SELECTED y
        kernel_conf_variable USB_ETH y
        kernel_conf_variable NFS_FS y
        kernel_conf_variable ROOT_NFS y
        kernel_conf_variable CMDLINE \"${CMDLINE_NFSROOT_USB} ${CMDLINE_DEBUG}\"
    fi

    # edit inline, as we base on the defconfig provided by hardkernel repo
    sed -i -e "${CONF_SED_SCRIPT}" '${S}/.config'
    yes '' | oe_runmake oldconfig
}

do_install_append() {
    # Helper script provided by Mauro Ribeiro
    # tools/hardkernel/genBscr.sh
    oe_runmake headers_install INSTALL_HDR_PATH=${D}${exec_prefix}/src/linux-${KERNEL_VERSION} ARCH=$ARCH
}

do_deploy_append() {
    # cp -v *.scr ${DEPLOYDIR}
}

PACKAGES =+ "kernel-headers"
FILES_kernel-headers = "${exec_prefix}/src/linux*"
