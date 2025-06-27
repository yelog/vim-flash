# ⚡vim-flash

![Build](https://github.com/yelog/vim-flash/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/25101-vim-flash)](https://plugins.jetbrains.com/plugin/25101-vim-flash)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25101-vim-flash)](https://plugins.jetbrains.com/plugin/25101-vim-flash)

<h2>Like <a href="https://github.com/folke/flash.nvim">flash.nvim</a> on IdeaVim</h2>

<table>
  <tr>
    <th>Standalone Jump</th>
    <th><code>f</code>, <code>t</code>, <code>F</code>, <code>T</code>, <code>;</code>, <code>,</code></th>
  </tr>
  <tr>
    <td>
      <img src="https://github.com/user-attachments/assets/acd88f0a-d628-40ef-89e3-53ccbd3a676a" />
    </td>
    <td>
      <img src="https://github.com/user-attachments/assets/acd88f0a-d628-40ef-89e3-53ccbd3a676a" />
    </td>
  </tr>
</table>

## Usage

Add the following code to your `.ideavimrc` file.

```vim
" Search for string in visible area and jump
nmap s <Action>(flash.search)
xmap s <Action>(flash.search)
" enhance vim f (find for char in characters to the right of the current cursor)
nmap f <Action>(flash.find)
xmap f <Action>(flash.find)
" enhance vim F (find for char in characters to the left of the current cursor)
nmap F <Action>(flash.find_backward)
xmap F <Action>(flash.find_backward)
" enhance vim t (till for char in characters to the right of the current cursor)
nmap t <Action>(flash.till)
xmap t <Action>(flash.till)
" enhance vim T (till for char in characters to the left of the current cursor)
nmap T <Action>(flash.till_backward)
xmap T <Action>(flash.till_backward)
" enhance vim ; (Repeat the last f/F/t/T search)
nmap ; <Action>(flash.repeat)
xmap ; <Action>(flash.repeat)
" enhance vim , (Repeat the last f/F/t/T search backward)
nmap , <Action>(flash.repeat_backward)
xmap , <Action>(flash.repeat_backward)
```

Then you can use `s` and type the word you want to search for. The word will be highlighted in the editor.

You can also use `f` to find a character to the right of the current cursor position, highlight all matches to the right, and press `f` again to jump to the next occurrence of that character. If you want to find a character to the left, use `F`. Similar enhancements are provided for other commands like `t`, `T`, `;`, and `,`.


## Configuration
Find `Settings -> Others Settings -> vim-flash` to configure the plugin.

- flash.search
    * Characters: This This is the sequence of letters used in order of proximity from nearest to farthest between the match and the current cursor.
    * Label Color: The first input box is the color of label text, and the second input box is the color of label background.
    * Label hit Color: The first input box is the color of label hit text, and the second input box is the color of label hit background.
    * Match Color: The first input box is the color of match text, and the second input box is the color of match background.
    * Match nearest color: The first input box is the color of match nearest text, and the second input box is the color of match nearest background.
    * Label Position: This is the position of the label. Default is false which meas it will be displayed after the match. If set to true, it will be displayed before the match.
    * Auto Jump: If this is set to true, them the plugin will automatically jump when there is only one match.
- flash.[find,find_backward,till,till_backward,repeat,repeat_backward]
    * Scroll Off: This is the number of lines to keep above and below the cursor when jumping to a match. Default is 4.


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
