
# Making a release

* Bump *versionCode* and *versionName* in [CalendarImportExport/build.gradle](CalendarImportExport/build.gradle)

* Make a release tag

      git tag vX.Y.Z -s -m 'Branded release.'

* Publish

      git push
      git push --tags

* Watch https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/org.sufficientlysecure.ical.yml
  and https://f-droid.org/en/packages/org.sufficientlysecure.ical/
  until release emerges.
