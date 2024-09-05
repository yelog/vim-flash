# vim-flash

![Build](https://github.com/yelog/vim-flash/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/25101-vim-flash)](https://plugins.jetbrains.com/plugin/25101-vim-flash)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25101-vim-flash)](https://plugins.jetbrains.com/plugin/25101-vim-flash)

<h2>Like <a href="https://github.com/folke/flash.nvim">flash.nvim</a> on IdeaVim</h2>

![ideavim-flash](https://github.com/user-attachments/assets/acd88f0a-d628-40ef-89e3-53ccbd3a676a)


## Usage

Add `map s <Action>(flash.search)` to your `.ideavimrc` file.

Then you can use `s` and type the word you want to search for. The word will be highlighted in the editor.

You can use other keybindings. for example `map <leader>s <Action>(flash.search)`

## Configuration
Find `Settings -> Others Settings -> vim-flash` to configure the plugin.

1. Characters: This This is the sequence of letters used in order of proximity from nearest to farthest between the match and the current cursor.
2. Label color: The first input box is the color of label text, and the second input box is the color of label background.
3. Match color: The first input box is the color of match text, and the second input box is the color of match background.
4. Match nearest color: The first input box is the color of match nearest text, and the second input box is the color of match nearest background.


## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "vim-flash"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/yelog/vim-flash/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Planned Features

- From GitHub issues and PRs

## References

- [flash.nvim](https://github.com/folke/flash.nvim)
- [KJump](https://github.com/a690700752/KJump)
