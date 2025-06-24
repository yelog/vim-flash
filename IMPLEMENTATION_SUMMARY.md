# 增强vim f功能实现总结

## 实现的功能

✅ **按f后光标后文字变灰色** - 通过修改JumpHandler的setGrayColor方法实现，支持全文档范围  
✅ **输入字符后高亮所有匹配** - 使用配置的标签颜色(labelBg/labelFg)高亮  
✅ **智能大小写匹配** - 小写字母忽略大小写，大写字母和符号精确匹配  
✅ **全文档搜索** - 不局限于当前屏幕，搜索整个文档光标后的内容  
✅ **自动跳转到最近匹配** - 立即跳转到第一个匹配的字符  
✅ **按f继续跳转** - 支持循环跳转到下一个匹配  
✅ **Esc/退格键退出** - 通过JumpHandler的事件处理实现  
✅ **性能优化** - 只高亮可见区域内的匹配字符  

## 增强功能详解

### 1. 智能大小写处理
```kotlin
val searchChars = if (c.isLowerCase()) {
    listOf(c, c.uppercaseChar())
} else {
    listOf(c)
}
```
- 输入小写字母时，同时匹配大小写版本
- 输入大写字母或符号时，精确匹配

### 2. 全文档搜索
```kotlin
// 使用整个文档文本而不是可见区域文本
this.documentText = e.document.text

// 在整个文档中搜索
for (i in (cursorOffset + 1) until documentText.length) {
    if (searchChars.contains(documentText[i])) {
        matches.add(i)
    }
}
```

### 3. 配置颜色支持
```kotlin
val highlightAttributes = TextAttributes().apply {
    backgroundColor = Color(config.labelBg, true)
    foregroundColor = Color(config.labelFg, true)
}
```

### 4. 性能优化高亮
```kotlin
// 只高亮可见区域内的匹配
for (offset in offsets) {
    if (offset >= visibleStart && offset < visibleEnd) {
        // 添加高亮器
    }
}
```

## 主要文件修改

### 修改的现有文件:
- `src/main/kotlin/org/yelog/ideavim/flash/action/VimF.kt` - **重大改写**
  - 从可见区域搜索改为全文档搜索
  - 添加智能大小写处理`findMatchesInDocument()`方法
  - 使用配置颜色替代硬编码红色
  - 优化高亮性能，只高亮可见区域

- `src/main/kotlin/org/yelog/ideavim/flash/JumpHandler.kt` - **小幅优化**
  - 改进VimF模式下的灰色高亮边界处理
  - 确保不超出文档长度

### 保持不变的文件:
- `src/main/kotlin/org/yelog/ideavim/flash/action/Finder.kt`
- `src/main/kotlin/org/yelog/ideavim/flash/Actions.kt`
- `src/main/resources/META-INF/plugin.xml`

## 功能特性对比

| 特性 | 原始版本 | 增强版本 |
|-----|---------|---------|
| 搜索范围 | 当前屏幕可见区域 | 整个文档（光标后） |
| 大小写处理 | 精确匹配 | 小写忽略大小写，大写精确匹配 |
| 高亮颜色 | 硬编码红色 | 使用配置的标签颜色 |
| 性能优化 | 无 | 只高亮可见区域，搜索全文档 |
| 跳转能力 | 屏幕内跳转 | 可跳转到屏幕外 |

## 使用方法

在`.ideavimrc`中添加配置:
```vim
nmap f <Action>(flash.vim_f)
xmap f <Action>(flash.vim_f)
```

## 功能流程增强

1. 按下f键 → 激活VimF模式，光标后可见文字变灰
2. 输入目标字符 → 
   - 小写字母：忽略大小写搜索整个文档
   - 大写字母/符号：精确匹配搜索整个文档
   - 高亮可见区域内的所有匹配（使用配置颜色）
   - 跳转到最近匹配
3. 继续按f → 跳转到下一个匹配（可能在屏幕外）
4. 按Esc/Backspace → 退出搜索模式

## 技术亮点

- **双重搜索策略**: 全文档搜索 + 可见区域高亮，平衡功能和性能
- **智能大小写**: 符合vim传统行为，小写忽略大小写
- **配置集成**: 使用现有的颜色配置，保持界面一致性
- **边界安全**: 正确处理文档边界和可见区域边界
- **资源管理**: 正确清理高亮器，避免内存泄露
- **性能考虑**: 避免对整个大文档进行高亮，只高亮可见部分
