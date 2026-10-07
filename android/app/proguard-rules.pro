# App-specific R8 keep rules for release builds.
# None needed yet: the app uses no reflection or serialization, and AndroidX
# (including the SessionViewModel factory) ships its own consumer rules.

# Keep line numbers in Play Console crash reports; the mapping file in the
# bundle restores the original names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
