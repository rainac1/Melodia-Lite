# 插件目录

应用内的「插件目录」读取一个 `index.json`，用来浏览、安装和检查更新。手动导入 `.melodiaplugin` 继续可用。

默认目录：`https://raw.githubusercontent.com/rinchao0721/melodia-plugins/main/index.json`

## 格式

```json
{
  "format": 1,
  "updatedAt": "2026-11-02",
  "plugins": [
    {
      "id": "com.example.hello",
      "name": "示例",
      "version": "1.0.0",
      "kind": "sandbox",
      "author": "作者",
      "description": "一句话说明",
      "platforms": ["android"],
      "apiVersion": "1.0",
      "permissions": ["storage"],
      "source": "https://github.com/作者/仓库",
      "download": {
        "url": "https://github.com/作者/仓库/releases/download/v1.0.0/hello.melodiaplugin",
        "sha256": "64 位小写十六进制",
        "size": 12345
      }
    }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `format` | 固定为 `1`，否则整个目录无效 |
| `id`、`name`、`version` | 必需，要和包内 `plugin.json` 一致；名称不超过 40 字，版本不超过 20 字符 |
| `kind` | 固定为 `sandbox` |
| `author`、`description`、`updatedAt` | 可选，只用于展示 |
| `platforms`、`apiVersion`、`minAppVersion` | 可选，含义同 `plugin.json`；不兼容的条目显示但不能安装 |
| `permissions` | 可选，只用于展示数量；实际权限以包内清单为准，安装时预览 |
| `source` | 源码地址，必须是 https |
| `download.url` | 必需，只允许 https，可以重定向，但不能重定向到 http |
| `download.sha256` | 必需，整个插件包的 SHA-256 |
| `download.size` | 必需，整个插件包的字节数，不超过 10MB |

单个条目不合规时只跳过这一条，重复 id 只保留第一条，最多读取 1000 条。

`.melodiaplugin` 的 SHA-256 在 Windows 上可以这样算：

```powershell
(Get-FileHash hello.melodiaplugin -Algorithm SHA256).Hash.ToLower()
```

## 提交与审核

提交方式：向目录仓库发 PR，修改 `index.json`。

所有插件：

- 包能在应用里手动导入并正常运行。
- 权限与功能相符，域名范围不宽于实际需要。
- 同一版本号发布后不再替换包内容；要改内容就升版本。

## 更新

应用只提示更新，不自动安装。打开插件设置时检查，启动时距上次成功读取超过 24 小时才联网。升级保留上一版本，可以在插件列表里回退。

## 自定义目录

目录地址可以在目录页修改，只接受 https。哈希写在目录里，所以校验只能保证下载内容和目录一致，不能保证目录本身可信。自定义目录的插件没有经过官方审核，安装预览会提示。
