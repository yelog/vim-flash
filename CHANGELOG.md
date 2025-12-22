<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# vim-flash Changelog

## 1.0.5

- fix: Fix an issue where flash.search searched across all open files when Search across splits was enabled.
- feat: When calling flash.search in split view, recent matches are prioritized in the split containing the cursor.

## 1.0.4

- fix: Compatibility verification for `Deprecated field ModalityState.NON_MODAL`

## 1.0.3

- feat: Support setting a timeout for cancellation in `flash.[find,find_backward,till,till_backward,repeat,repeat_backward]` operations
- fix: Fixed highlight background range in `flash.[find,find_backward,till,till_backward,repeat,repeat_backward]` operations

## 1.0.2

- feat: Removed the upper limit requirement for IntelliJ Platform versions, now supports 231 and higher

## 1.0.1

- feat: `flash.search` is now available when using split screens; enabled by default and can be controlled in settings via `Search across splits`

## 1.0.0

Most of the intended features have already been implemented. This marks the official release, with the version number changed to 1.0.0.

- feat: Changed the color setting in the settings interface from text input to a color picker
- feat: All configuration options in the settings interface now support resetting to default values

## 0.1.5

- fix: register document listeners with disposables to avoid deprecated API usage on 253 builds

## 0.1.4

- feat: Add `flash.treesitter` to set visual area based on syntax
- feat: Add `flash.remote` to jump to a remote line after pressing an operator like `d`

## 0.1.3

- fix: other keys become immediately active in `f`, `F`, `t`, `T`, `;`, and `,` motions

## 0.1.2

- fix: selection area is incorrect in visual mode [issues#66](https://github.com/yelog/vim-flash/issues/66)
- fix: adjust text rendering for vertical alignment in MarksCanvas [issues#66](https://github.com/yelog/vim-flash/issues/66)

## 0.1.1

- fix: fix the issue where `<Action>` was escaped in the plugin introduction page

## 0.1.0

- feat: `f`, `F`, `t`, `T`, `;` and `,` motions
- fix: update selection and history when jump

## 0.0.9

- support auto jump when only one match

## 0.0.8

- Support showing tags in front of the match, can be modified in the settings panel

## 0.0.7

- Ensured compatibility with the upcoming IDE version (252)

## 0.0.6

- Merge [pull#56](https://github.com/yelog/vim-flash/pull/56)

## 0.0.5
- Fix [issues#39](https://github.com/yelog/vim-flash/issues/39)
- Fix [issues#40](https://github.com/yelog/vim-flash/issues/40)

## 0.0.4
- Fix [issues#27](https://github.com/yelog/vim-flash/issues/27)
- Fix [issues#30](https://github.com/yelog/vim-flash/issues/30)

## 0.0.3
- Goto closest match when pressing enter key
- Default use the color of flash.nvim

## 0.0.2

### Added

- Update description
- Support latest IntelliJ IDEA

### Modified

- Fix issue of deprecated methods

## 0.0.1

### Added

- Basic functionality

## [Unreleased]

### Optimization

- Jump to the most recent match by pressing Enter


