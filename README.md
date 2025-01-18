# osm-opening-hours

A Kotlin multiplatform library to parse OpenStreetMap opening hours from a string into a data model and back.

- It is pure Kotlin, no dependencies


- It mostly follows the OpenStreetMap [opening hours specification](https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification). For details and remarks, 
  see the section [Specification](#Specification).
  As of Jan 2025, 96.48% of opening hours strings in the wild are considered valid, 99.10% are understood.


- The data model is type-safe, i.e. it is not possible to create an invalid opening hours string 
  from the data model


- It is very fast. Expect one order of magnitude faster than other opening hours syntax parsers, 
  e.g. it parses the average opening hours string about 10x as fast as the Java [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser).

  See [`src/jvmTest/kotlin/tasks/print_statistics/PrintStatistics.kt`](src/jvmTest/kotlin/tasks/print_statistics/PrintStatistics.kt) for the script that measures it.


It is currently used in [StreetComplete](https://github.com/streetcomplete/streetcomplete).


# Copyright and License

© 2024 Tobias Zwick. This library is released under the terms of the MIT License.


# Usage

Add [de.westnordost:osm-opening-hours:0.1.0](https://mvnrepository.com/artifact/de.westnordost/osm-opening-hours/0.1.0) as a Maven dependency or download the jar from there.


Usage e.g.
```kotlin
// parse into data model
val hours = "Mo-Fr 08:00-18:00; Sa 10:00-12:00".toOpeningHoursOrNull()

// create from data model
val hoursString = hours?.toString()
```

# Specification

It mostly follows the OpenStreetMap [opening hours specification](https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification) version 0.7.3, with a few
additions/remarks:

- **Whitespaces**: e.g. `Jan05Mo-Fr08:00`

  The specification does not comprehensively define where and how many spaces are allowed
  or required in-between the tokens. So, we assume that any number (including none) of them are 
  allowed and only required in places where two successive tokens use the same set of characters
  (e.g. `week05␣05:00`, `Su␣sunset`, `Jan05:␣05:00`). Other parsers consider the lack of spaces 
  valid too, though the canonical form always contains spaces in-between for clarity and readability.


- **Specific weekday dates**: e.g. `Jul Fr[2]-Aug Mo[-1]`

  (= Second Friday in July to last Monday in August.) This [undocumented extension](https://wiki.openstreetmap.org/wiki/Talk:Key:opening_hours/specification#Undocumented_extensions_to_spec_0.7.2)
  to the specification is supported by at least [the reference implementation](https://openingh.openstreetmap.de/evaluation_tool/?EXP=Jul%20Fr[2]%20-%20Aug%20Mo[-1])
  and the Java [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser).
  It is supported here also because it has (minor) use and there is no other, valid, way to express 
  its semantic.


- **More restrictive date ranges within month**: e.g. `easter+Su-09-We +3 days` considered invalid
  
  The specification allows for an unnecessarily complex syntax for dates and syntax that doesn't
  make sense (and is therefore not used). So dates are parsed in a slightly more strict way 
  according to the rules as described in [this comment](https://wiki.openstreetmap.org/wiki/Talk:Key:opening_hours/specification#Simplify_months_and_dates_selector_(disallow_syntax_variations_on_within-month-ranges_that_make_no_sense)).


## Lenient parsing

Beyond what is considered valid according to the specification and the mentioned remarks,
the following unambiguous syntax variations are understood by the parser if instructed to be 
lenient: 

#### Generally

- case is ignored (e.g. `MO-FR`, `WEEK 01`, `Easter`...)
- en dashes, em dashes and " to " can be used for ranges (e.g. `08:00—12:00`, `Mo to Fr`)
- any [unicode whitespace](https://en.wikipedia.org/wiki/Whitespace_character) instead of only the normal space allowed in-between the tokens
- rules with "additional" modifier may follow even if previous rule did neither terminate in a time,
  nor has a comment, nor has an explicit selector mode (e.g. `Mo-Th, May-Aug Fr-Sa`).
  [This is normally not allowed](https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:additional_rule_separator).
  Strings like `May-Aug, Mo-Th` are interpreted as `May-Aug Mo-Th`.

#### Times

- times in the 12-hour clock notation (e.g `12:30AM`, `08:00 p.m.`, `12:00 pm`, `08:00 ㏂`)
- only the hour is specified (e.g `12 AM`, `16`, `14h`)
- "h" and "." as minutes separators (e.g `12 h 30`, `8h15am`, `08.00`)
- single digits for hours (e.g `8:30`)
- understand 24/7 as denoting 00:00-24:00 (e.g. `Fr-Su 24/7`)

#### Weekdays / Holidays

- non-abbreviated weekdays, as well as three-character weekdays and German two-letter weekday
  abbreviations (e.g. `Tuesday`, `Tue`, `Di`...)
- a dot directly after a weekday abbreviation (e.g. `Mo.`)
- holidays mixed in weekdays (e.g. `Mo-Sa,PH,Su`)
- a superfluous ":" after weekdays/holidays (e.g. `Mo-Sa: 08:00-12:00`)

#### Months / Dates / Weeks

- multiple month day dates separated by comma (e.g. `Dec 25,31`, `Dec 25-27,31`) 
- non-abbreviated months (e.g. `December`)
- a dot directly after a month abbreviation (e.g. `Dec.`)
- single digits for month day numbers (e.g. `Jan 5`)
- single digits for week numbers (e.g. `week 1-9`)

Usage e.g.
```kotlin
val hours = "October to Dec.: Sun,PH,Thu: 8 am — 12h30 pm; WEEK1 24/7"
    .toOpeningHours(lenient = true)
```

When (re-)creating the string from the data model, of course always a valid opening hours string is 
returned.
