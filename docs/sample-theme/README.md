# 样例主题包

`.btheme` 是 ZIP 容器（format 1），条目白名单：`theme.json`（必需，≤64KB）、
`preview.png`（≤1MB）、`background[_dark|_light].png`（≤6MB）；整包 ≤8MB、≤12 条目。

## 正样例

- `aurora.btheme` —— 青色种子 `#008080`、blend 40、scrim 0.5、背景图/预览图齐全。

## 负样例（全部应被拒绝并提示原因）

| 文件 | 预期拒绝原因 |
|---|---|
| `zip-slip.btheme` | 含 `../evil.png` 条目，白名单拦截 |
| `oversize.btheme` | 整包 >8MB |
| `entry-too-big.btheme` | 单张 background >6MB |
| `format99.btheme` | format 99 → “主题版本过新，请升级哔哩终端” |
| `no-json.btheme` | 缺 theme.json |

## 重新生成

```bash
python3 make_samples.py
```
