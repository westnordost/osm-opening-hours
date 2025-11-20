## 0.4.0

Strict parsing:
- FIX: Times like `Nov11:11:11` (11th of November at 11:11) didn't parse correctly. The parser was confused where the colon belonged to.
- allow times like `24:30` because the specification allows it

Lenient parsing:
- FIX: Times like `Nov11:11` (in November, at 11:11) and `Nov24/7` (in November, 24/7) didn't parse correctly. The parser was confused were the colon or slash belonged to.
- understand weekday and months abbreviations in some more languages
- allow some Chinese and Japanese characters for ranges, enumerations and hour:minute separation
- allow full-width characters (`：`, `，`, `；`, `０`, `９`,…)
- allow hours and minutes with more leading zeroes (e.g. `011:030`)
- allow intervals like `dusk-dawn/2h` (every 2 hours between dusk and dawn)
- allow `week` keyword to be repeated for each range (e.g. `week 1-9, week 40-52`)

## 0.3.0

- added WASM support, by @sargunv
- added [documentation page](https://westnordost.github.io/osm-opening-hours/) , by @sargunv

### 0.2.0

Support syntax like (#2)

- `Oct Su[-1] - 1 day` = _One day before last Sunday in October_
- `Oct 16 +Su +1 day` = _One day after the Sunday after the 16th of October_

### 0.1.0

Initial release
