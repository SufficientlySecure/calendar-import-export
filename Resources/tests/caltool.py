#!/bin/env python
"""Munge test files to create valid calendars for testing"""
#
# Copyright (C) 2015 Jon Griffiths (jon_p_griffiths@yahoo.com)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2.0 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
import os
from argparse import ArgumentParser, FileType

def setSummaryFromTestName(lines):
    out = []
    for line in lines:
        if line.startswith('SUMMARY:'):
            continue
        if line.startswith('X-TEST-NAME:'):
            line = line.replace('X-TEST-NAME', 'SUMMARY')
        out.append(line)
    return out;

def setDescriptionFromTestName(lines):
    out = []
    description = ''
    first = None
    skipped = False
    i = 0
    for line in lines:
        if line.startswith('DESCRIPTION:'):
            skipped = True
            continue
        if line.startswith('X-TEST-NOTE:'):
            description = description + line.replace('X-TEST-NOTE:', '')
            first = first or i
        else:
            out.append(line)
            i += 1

    if skipped and len(description) == 0:
        return lines
    if len(description) != 0:
        out.insert(i, 'DESCRIPTION:' + description)
    return out;

def parseTestFile(args, test_file):
    """Read in and munge a test file"""

    MODE_SEARCHING, MODE_READING = 0, 1
    uid_seq = 1
    REQUIRED = { 'DTSTAMP' : lambda args: '20000000T000000Z',
                 'UID' : lambda args: '%s_%s' % (os.path.basename(test_file.name), str(uid_seq)),
                 'ORGANIZER' : lambda args: 'mailto:%s' % args.organizer,
               }
    TEST_VALS = { 'X-TEST-VALUE': '', 'X-TEST-MIN-VERSION': '' }

    mode = MODE_SEARCHING
    result = []
    current, seen = [], {}

    for line in test_file.readlines():

        line = line.strip()
        if len(line) == 0 or (args.convert and line.split(':')[0] in TEST_VALS):
            continue # test item to skip

        if line == 'BEGIN:VEVENT':
            mode = MODE_READING
            uid_seq = uid_seq + 1
            current, seen = [], {}
        elif line == 'END:VEVENT' or (mode == MODE_READING and line.startswith('BEGIN:')):
            # Add missing mandatory elements
            for k, v in REQUIRED.iteritems():
                if k not in seen:
                    seen[k] = True
                    current.insert(1, k + ':' + v(args))

        if line == 'END:VEVENT':
            if seen.has_key('SUMMARY'):
                current = [l for l in current if not l.startswith('X-TEST-NAME')]
            else:
                if args.convert:
                    current = setSummaryFromTestName(current)
                else:
                    current.insert(1, 'SUMMARY:')
            if not seen.has_key('DESCRIPTION') and args.convert:
                current = setDescriptionFromTestName(current)

            current.append(line)
            result.extend(current)
            mode = MODE_SEARCHING
        else:
            kw = line.split(':')[0]
            if ';' in kw:
                kw = line.split(';')[0]
            seen[kw] = True

        if mode != MODE_SEARCHING:
            current.append(line)

    return result


def writeResultCalendar(args, lines):

    print 'BEGIN:VCALENDAR'
    print 'PRODID:-//caltool//caltool 1.0//EN'
    print 'CALSCALE:GREGORIAN'
    print 'VERSION:2.0'

    for line in lines:
        print line

    print 'END:VCALENDAR'


if __name__ == '__main__':

    parser = ArgumentParser(version='1.0')
    parser.add_argument('--organizer', default='foo@bar.com',
                        help='use ORGANIZER as the organiser email (where missing)')
    parser.add_argument('--convert', default=False, action="store_true",
                        help='convert test name/notes to summary/description')
    parser.add_argument('file', type=FileType('r'), nargs='+')
    args = parser.parse_args()

    lines = []
    for test_file in args.file:
        lines.extend(parseTestFile(args, test_file))

    writeResultCalendar(args, lines)

