#!/usr/bin/env python
# Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import apk_utils
import glob
import optparse
import os
import shutil
import sys
import utils

USAGE = 'usage: %prog [options] <apk>'

def parse_options():
  parser = optparse.OptionParser(usage=USAGE)
  parser.add_option('--dex',
                    help='directory with dex files to use instead of those in the apk',
                    default=None)
  parser.add_option('--out',
                    help='output file (default ./$(basename <apk>))',
                    default=None)
  parser.add_option('--keystore',
                    help='keystore file (default ~/.android/app.keystore)',
                    default=None)
  parser.add_option('--install',
                    help='install the generated apk with adb options -t -r -d',
                    default=False,
                    action='store_true')
  parser.add_option('--adb-options',
                    help='additional adb options when running adb',
                    default=None)
  parser.add_option('--quiet',
                    help='disable verbose logging',
                    default=False)
  (options, args) = parser.parse_args()
  if len(args) != 1:
    parser.error('Expected <apk> argument, got: ' + ' '.join(args))
  apk = args[0]
  return (options, apk)

def findKeystore():
  return os.path.join(os.getenv('HOME'), '.android', 'app.keystore')

def repack(processed_out, original_apk, temp, quiet):
  processed_apk = os.path.join(temp, 'processed.apk')
  shutil.copyfile(original_apk, processed_apk)
  if not processed_out:
    utils.Print('Using original APK as is', quiet=quiet)
    return processed_apk
  utils.Print(
      'Repacking APK with dex files from {}'.format(processed_apk), quiet=quiet)
  with utils.ChangedWorkingDirectory(temp, quiet=quiet):
    cmd = ['zip', '-d', 'processed.apk', '*.dex']
    utils.RunCmd(cmd, quiet=quiet)
  if processed_out.endswith('.zip') or processed_out.endswith('.jar'):
    cmd = ['unzip', processed_out, '-d', temp]
    if quiet:
      cmd.insert(1, '-q')
    utils.RunCmd(cmd, quiet=quiet)
    processed_out = temp
  with utils.ChangedWorkingDirectory(processed_out, quiet=quiet):
    dex = glob.glob('*.dex')
    cmd = ['zip', '-u', '-9', processed_apk] + dex
    utils.RunCmd(cmd, quiet=quiet)
  return processed_apk

def sign(unsigned_apk, keystore, temp, quiet):
  signed_apk = os.path.join(temp, 'unaligned.apk')
  apk_utils.sign(unsigned_apk, signed_apk, keystore, quiet=quiet)
  return signed_apk

def align(signed_apk, temp, quiet):
  utils.Print('Aligning', quiet=quiet)
  aligned_apk = os.path.join(temp, 'aligned.apk')
  cmd = ['zipalign', '-f', '4', signed_apk, aligned_apk]
  utils.RunCmd(cmd, quiet=quiet)
  return signed_apk

def masseur(
    apk, dex=None, out=None, adb_options=None, keystore=None, install=False,
    quiet=False):
  if not out:
    out = os.path.basename(apk)
  if not keystore:
    keystore = findKeystore()
  with utils.TempDir() as temp:
    processed_apk = None
    if dex:
      processed_apk = repack(dex, apk, temp, quiet)
    else:
      utils.Print(
          'Signing original APK without modifying dex files', quiet=quiet)
      processed_apk = os.path.join(temp, 'processed.apk')
      shutil.copyfile(apk, processed_apk)
    signed_apk = sign(processed_apk, keystore, temp, quiet=quiet)
    aligned_apk = align(signed_apk, temp, quiet=quiet)
    utils.Print('Writing result to {}'.format(out), quiet=quiet)
    shutil.copyfile(aligned_apk, out)
    if install:
      adb_cmd = ['adb']
      if adb_options:
        adb_cmd.extend(
            [option for option in adb_options.split(' ') if option])
      adb_cmd.extend(['install', '-t', '-r', '-d', out]);
      utils.RunCmd(adb_cmd, quiet=quiet)

def main():
  (options, apk) = parse_options()
  masseur(apk, **vars(options))
  return 0

if __name__ == '__main__':
  sys.exit(main())
