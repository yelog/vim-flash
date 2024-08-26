# vim-flash

![Build](https://github.com/yelog/vim-flash/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

<h2>Like <a href="https://github.com/folke/flash.nvim">flash.nvim</a> on IdeaVim</h2>

![vim-flash](https://github.com/user-attachments/assets/81c20618-6974-4e96-b199-016061536156)

## Usage

Add `nmap s :action flash.search<cr>` to your `.ideavimrc` file.

Then you can use `s` and type the word you want to search for. The word will be highlighted in the editor.

You can use other keybindings. for example <code>nmap <leader>s :action flash.search<cr></code>

## Configuration
Find `Settings -> Others Settings -> vim-flash` to configure the plugin.

1. Characters: This This is the sequence of letters used in order of proximity from nearest to farthest between the match and the current cursor.
2. Background color: This is the Background color of the match


## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "vim-flash"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/yelog/vim-flash/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Planned Features

- Goto closest match when pressing enter key

## References

- [flash.nvim](https://github.com/folke/flash.nvim)
- [KJump](https://github.com/a690700752/KJump)
