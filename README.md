# iCal Import/Export

This app allows you to import iCalender files to your calender without using google synchronization services. 

This is a updated fork from iCal Import/Export, found on Googlecode (http://code.google.com/p/ical-import-export/).

# Build the project

1. Have Android SDK "tools" directory in your PATH (http://developer.android.com/sdk/index.html)
2. Change to "iCalImportExport" directory with ``cd Local-Calendar``
3. Execute ``android update project -p .``
4. Execute ``ant debug`` or ``ant release``

# Contribute

Fork the git repository and do a Pull Request. I will merge your changes back into the main project.

# Libraries

All JAR-Libraries are provided in this repository under "libs", all Android Library projects are under "android-libs".

# Coding Style

## Code
* Indentation: 4 spaces, no tabs
* Maximum line width for code and comments: 100
* Opening braces don't go on their own line
* Field names: Non-public, non-static fields start with m.
* Acronyms are words: Treat acronyms as words in names, yielding !XmlHttpRequest, getUrl(), etc.

See http://source.android.com/source/code-style.html

## XML
* XML Maximum line width 999
* XML: Split multiple attributes each on a new line (Eclipse: Properties -> XML -> XML Files -> Editor)
* XML: Indent using spaces with Indention size 4 (Eclipse: Properties -> XML -> XML Files -> Editor)

See http://www.androidpolice.com/2009/11/04/auto-formatting-android-xml-files-with-eclipse/

# Licenses
iCal Import/Export is licensed under the GPLv3+.  
The file COPYING includes the full license text.

## Details
iCal Import/Export is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

iCal Import/Export is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with iCal Import/Export.  If not, see <http://www.gnu.org/licenses/>.

## Libraries

* iCal4J  
  http://ical4j.sourceforge.net/  
  New BSD License

## Images
