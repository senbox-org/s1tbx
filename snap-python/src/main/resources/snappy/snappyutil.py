import argparse
import logging
import os
import os.path
import platform
import sys
import zipfile


def _find_file(dir_path, regex):
    if os.path.isdir(dir_path):
        for filename in os.listdir(dir_path):
            if regex.match(filename):
                file = os.path.join(dir_path, filename)
                if os.path.isfile(file):
                    return file
    return None


def _configure_snappy(snap_home=None,
                      java_module=None,
                      java_home=None,
                      jvm_max_mem=None,
                      req_arch=None,
                      req_java=False,
                      req_py=False,
                      force=False):
    """
    Unzips matching jpy binary distribution from ../lib/jpy.<platform>-<python-version>.zip,
    imports unpacked 'jpyutil' and configures jpy for SNAP.

    :param snap_home: SNAP distribution directory.
    :param java_home: Java home directory. See also Java system property "java.home".
    :param jvm_max_mem: The heap size of the JVM.
    :param req_arch:  Required JVM architecture (amd64, ia86, x86, etc). See Java system property "os.arch".
    :param req_java:  Fail, if configuration of jpy's Java API fails.
    :param req_py:    Fail, if configuration of jpy's Python API fails.
    :param force:     Force overwriting of existing files.
    :return:
    """

    logging.info("Installing from Java module '" + java_module + "'")

    if req_arch:
        req_arch = req_arch.lower()
        act_arch = platform.machine().lower()

        if req_arch != act_arch:
            logging.warning("Architecture requirement possibly not met: "
                            "Python is " + act_arch + " but JVM requires " + req_arch)
        is64 = sys.maxsize > 2 ** 31 - 1
        if is64 and not req_arch in ('amd64', 'ia64', 'x64', 'x86_64'):
            logging.warning("Architecture requirement possibly not met: "
                            "Python is 64 bit but JVM requires " + req_arch)

    snappy_dir = os.path.dirname(os.path.abspath(__file__))
    snappy_ini_file = os.path.join(snappy_dir, 'snappy.ini')

    jpyutil_file = os.path.join(snappy_dir, 'jpyutil.py')
    jpyconfig_java_file = os.path.join(snappy_dir, 'jpyconfig.properties')
    jpyconfig_py_file = os.path.join(snappy_dir, 'jpyconfig.py')

    must_install_jpy = force \
                       or not os.path.exists(jpyutil_file)
    if not must_install_jpy:
        try:
            import jpyutil
        except ImportError:
            must_install_jpy = True

    must_configure_jpy = force \
                         or must_install_jpy \
                         or not (os.path.exists(jpyconfig_java_file) and os.path.exists(jpyconfig_py_file))
    if not must_configure_jpy:
        try:
            import jpyutil

            jpyutil.preload_jvm_dll()
            import jpy
        except:
            must_configure_jpy = True

    must_configure_snappy = force \
                            or must_configure_jpy \
                            or not os.path.exists(snappy_ini_file)

    #
    # If jpy is not installed yet at all, try to do so by using any compatible binary wheel found
    # in ${snappy_dir} or ${java_module}.
    #
    if must_install_jpy:
        logging.info("Installing jpy...")

        # See "PEP 0425 -- Compatibility Tags for Built Distributions"
        # https://www.python.org/dev/peps/pep-0425/
        import distutils.util

        platform_tag = distutils.util.get_platform().replace('-', '_').replace('.', '_')
        python_tag = 'cp%d%d' % (sys.version_info.major, sys.version_info.minor,)
        jpy_wheel_file_pat = 'jpy-{version}-%s-{abi_tag}-%s.whl' % (python_tag, platform_tag)

        import re

        jpy_wheel_file_re = jpy_wheel_file_pat.replace('{version}', '[^\-]+').replace('{abi_tag}', '[^\-]+')
        jpy_wheel_file_rec = re.compile(jpy_wheel_file_re)

        #
        # See if user put a custom jpy platform wheel into snappy dir
        # ./snappy/jpy-{version}-{python_tag}-{abi_tag}-{platform_tag}.whl
        #
        jpy_wheel_file = _find_file(snappy_dir, jpy_wheel_file_rec)
        # No, then search for it in the snap-python module
        if not jpy_wheel_file:
            #
            # Look for
            # ${java_module}/lib/jpy-{version}-{python_tag}-{abi_tag}-{platform_tag}.whl
            # depending of whether ${java_module} it is a JAR file or directory
            #
            if os.path.isfile(java_module):
                with zipfile.ZipFile(java_module) as zf:
                    lib_prefix = 'lib/'
                    for name in zf.namelist():
                        if name.startswith(lib_prefix):
                            basename = name[len(lib_prefix):]
                            if jpy_wheel_file_rec.match(basename):
                                logging.info("Extracting '" + name + "' from '" + java_module + "'")
                                jpy_wheel_file = zf.extract(name, snappy_dir)
                                break
            else:
                lib_dir = os.path.join(java_module, 'lib')
                jpy_wheel_file = _find_file(lib_dir, jpy_wheel_file_rec)

        if jpy_wheel_file and os.path.exists(jpy_wheel_file):
            logging.info("Unzipping '" + jpy_wheel_file + "'")
            with zipfile.ZipFile(jpy_wheel_file) as zf:
                zf.extractall(snappy_dir)
        else:
            logging.error("The module 'jpy' is required to run snappy, but no binary 'jpy' wheel matching the pattern")
            logging.error("'" + jpy_wheel_file_pat + "' could be found.\n"
                          + "You can try to build a 'jpy' wheel yourself and then copy it into\n"
                          + "\"" + snappy_dir + "\" and then run the configuration again.\n"
                          + "Please go to https://github.com/bcdev/jpy and follow the build instructions. E.g.\n"
                          + "  > git clone https://github.com/bcdev/jpy.git\n"
                          + "  > cd jpy\n"
                          + "  > python setup.py bdist_wheel\n"
                          + "  > cp dist/*.whl \"" + snappy_dir + "\"")
            return 10
    else:
        logging.info("jpy is already installed")

    #
    # If jpy isn't configured yet, do so by executing "jpyutil.py" which will write the runtime configurations:
    # - "jpyconfig.properties" - Configuration for Java about Python (jpy extension module)
    # - "jpyconfig.py" - Configuration for Python about Java (JVM)
    #
    if must_configure_jpy:
        logging.info("Configuring jpy...")
        if os.path.exists(jpyutil_file):
            # Note 'jpyutil.py' has been unpacked by the previous step, so we can safely import it
            import jpyutil

            if not java_home:
                jre_dir = os.path.join(snap_home, 'jre')
                if not os.path.exists(jre_dir):
                    parent = os.path.dirname(jpyutil_file)
                    while parent:
                        jre_dir = os.path.join(parent, 'jre')
                        if os.path.exists(jre_dir):
                            break
                        parent = os.path.dirname(parent)
                if os.path.exists(jre_dir):
                    java_home = os.path.normpath(jre_dir)

            ret_code = jpyutil.write_config_files(out_dir=snappy_dir,
                                                  java_home_dir=java_home,
                                                  req_java_api_conf=req_java,
                                                  req_py_api_conf=req_py)
            if ret_code:
                return ret_code
        else:
            logging.error("Missing Python module '" + jpyutil_file + "'\n"
                                                                     "which is required to complete the configuration.")
            return 20
    else:
        logging.info("jpy is already configured")

    #
    # If snappy isn't configured yet, do so by writing a default "snappy.ini".
    # Note, this file is only used if you use SNAP from Python, i.e. importing
    # the snappy module in your Python programs.
    #
    if must_configure_snappy:
        logging.info("Configuring snappy...")
        with open(snappy_ini_file, 'w') as file:
            file.writelines(['[DEFAULT]\n',
                             'snap_home = %s\n' % snap_home,
                             'java_max_mem: %s\n' % jvm_max_mem,
                             '# snap_start_engine: False\n',
                             '# java_class_path: ./target/classes\n',
                             '# java_library_path: ./lib\n',
                             '# java_options: -Djava.awt.headless=false\n',
                             '# debug: False\n'])
            logging.info("snappy configuration written to '" + snappy_ini_file + "'")
    else:
        logging.info("snappy is already configured")

    #
    # Finally, we test the snappy installation/configuration by importing it.
    # If this won't succeed, _main() will catch the error and report it.
    #
    logging.info("Importing snappy for final test...")
    sys.path = [os.path.join(snappy_dir, '..')] + sys.path
    __import__('snappy')

    logging.info("Done. The SNAP-Python interface is located in '%s'\n"
                 "When using SNAP from Python, either do: sys.path.append('%s')\n"
                 "or copy the snappy module into your Python's 'site-packages' directory."
                 % (snappy_dir, snappy_dir.replace("\\", "\\\\")))

    return 0


def _main():
    parser = argparse.ArgumentParser(description='Configures snappy, the SNAP-Python interface.')
    parser.add_argument('--snap_home', default=None,
                        help='SNAP distribution directory')
    parser.add_argument('--req_arch', default=None,
                        help='required JVM architecture, e.g. "amd64", '
                             'may be taken from Java system property "os.arch"')
    parser.add_argument('--java_module', default=None,
                        help='directory or JAR file containing the "snap-python" Java module')
    parser.add_argument('--java_home', default=None,
                        help='Java JDK or JRE installation directory, '
                             'may be taken from Java system property "java.home"')
    parser.add_argument('--jvm_max_mem', default='3G', help='size of the Java VM heap space')
    parser.add_argument("--log_file", action='store', default=None, help="file into which to write logging output")
    parser.add_argument("--log_level", action='store', default='INFO',
                        help="log level, possible values are: DEBUG, INFO, WARNING, ERROR")
    parser.add_argument("-j", "--req_java", action='store_true', default=False,
                        help="require that Java API configuration succeeds")
    parser.add_argument("-p", "--req_py", action='store_true', default=False,
                        help="require that Python API configuration succeeds")
    parser.add_argument('-f', '--force', action='store_true', default=False,
                        help='force overwriting of existing files')
    args = parser.parse_args()

    log_level = getattr(logging, args.log_level.upper(), None)
    if not isinstance(log_level, int):
        raise ValueError('Invalid log level: %s' % log_level)

    log_format = '%(levelname)s: %(message)s'
    log_file = args.log_file
    if log_file:
        logging.basicConfig(format=log_format, level=log_level, filename=log_file, filemode='w')
    else:
        logging.basicConfig(format=log_format, level=log_level)

    try:
        ret_code = _configure_snappy(snap_home=args.snap_home,
                                     java_module=args.java_module,
                                     java_home=args.java_home,
                                     jvm_max_mem=args.jvm_max_mem,
                                     req_arch=args.req_arch,
                                     req_java=args.req_java,
                                     req_py=args.req_py,
                                     force=args.force)
        if ret_code != 0:
            logging.error("Configuration failed")
    except:
        ret_code = 30
        logging.exception("Configuration failed")

    exit(ret_code)


if __name__ == '__main__':
    _main()
